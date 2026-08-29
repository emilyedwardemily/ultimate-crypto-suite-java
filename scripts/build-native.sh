#!/usr/bin/env bash
#
# UltimateCryptoSuite - native packaging pipeline
#
#  1. Builds the thin (no-JavaFX) obfuscated jar:   mvn -Pnative -Pobfuscate verify
#  2. Injects the anti-tamper hash:                  scripts/hash-and-sign.py
#  3. Downloads the JavaFX jmods for the target OS (if absent)
#  4. Runs jpackage (jlink-embedded runtime) to produce platform installers
#
# Usage:
#   ./scripts/build-native.sh <linux|windows|macos> [--app-image-only]
#
# Output goes into dist/native/.
#
# Prereqs (CI installs these; macOS also requires signing certs):
#   Linux : jpackage + fakeroot + dpkg-deb (deb), appimagetool (AppImage, optional)
#   Windows: jpackage (uses WiX for MSI)
#   macOS : jpackage + codesign/notarytool certs
#
set -euo pipefail

OS="${1:-linux}"
ONLY_IMAGE="${2:-}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"

# Some CI runners ship a JDK image without jmods (e.g. GitHub Actions Temurin
# for JDK 25), which breaks ProGuard (-libraryjars $JAVA_HOME/jmods/*.jmod) and
# jlink. If jmods are missing, provision a full JDK 25 (with runtime for
# jpackage) from the Adoptium API, then fetch the matching jmods asset and place
# it under $JAVA_HOME/jmods.
ensure_jdk_with_jmods() {
    if [[ -f "$JAVA_HOME/jmods/java.base.jmod" ]]; then
        return 0
    fi
    echo "==> JDK at $JAVA_HOME lacks jmods; provisioning full JDK 25 from Adoptium"
    local os arch
    case "${OS}" in
        windows) os="windows" ;;
        macos)   os="mac" ;;
        *)       os="linux" ;;
    esac
    case "$(uname -m)" in
        aarch64|arm64) arch="aarch64" ;;
        *)             arch="x64" ;;
    esac
    local base="https://api.adoptium.net/v3/binary/latest/25/ga/${os}/${arch}"
    local dl="$PROJECT_DIR/target/jdk/${os}-${arch}-jdk.bin"
    local stage="$PROJECT_DIR/target/jdk/${os}-${arch}"
    mkdir -p "$stage"
    curl -fsSL --retry 3 --retry-all-errors "${base}/jdk/hotspot/normal/eclipse?project=jdk" -o "$dl"
    python3 - "$dl" "$stage" <<'PY'
import sys
p, out = sys.argv[1], sys.argv[2]
with open(p, 'rb') as f:
    head = f.read(4)
if head[:2] == b'PK':
    import zipfile
    with zipfile.ZipFile(p) as z:
        z.extractall(out)
else:
    import tarfile
    with tarfile.open(p, 'r:gz') as t:
        t.extractall(out)
PY
    JAVA_HOME="$(find "$stage" -maxdepth 1 -type d -name 'jdk-*' | head -1)"
    JAVA_HOME="${JAVA_HOME:-$stage}"
    # macOS JDK layout nests the actual home under Contents/Home.
    if [[ -d "$JAVA_HOME/Contents/Home/bin" ]]; then
        JAVA_HOME="$JAVA_HOME/Contents/Home"
    fi
    rm -f "$dl"
    echo "    using $JAVA_HOME"

    if [[ ! -f "$JAVA_HOME/jmods/java.base.jmod" ]]; then
        echo "    fetching jmods asset from Adoptium"
        local jmodsz="$PROJECT_DIR/target/jdk/${os}-${arch}-jmods.tar.gz"
        curl -fsSL --retry 3 --retry-all-errors "${base}/jmods/hotspot/normal/eclipse?project=jdk" -o "$jmodsz"
        python3 - "$jmodsz" "$JAVA_HOME/jmods" <<'PY'
import sys, os
p, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
with open(p, 'rb') as f:
    head = f.read(2)
if head == b'PK':
    import zipfile
    with zipfile.ZipFile(p) as z:
        for m in z.infolist():
            name = m.filename
            if name.endswith('/'):
                continue
            parts = name.split('/')
            if len(parts) < 2:
                continue
            rel = '/'.join(parts[1:])
            dst = os.path.join(out, rel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with z.open(m) as src, open(dst, 'wb') as d:
                d.write(src.read())
else:
    import tarfile
    with tarfile.open(p, 'r:gz') as t:
        for m in t.getmembers():
            if '/' not in m.name:
                continue
            m.name = m.name.split('/', 1)[1]
            if m.name.endswith('/'):
                continue
            t.extract(m, out)
PY
        rm -f "$jmodsz"
    fi
    echo "    java.base.jmod present: $(test -f "$JAVA_HOME/jmods/java.base.jmod" && echo yes || echo no)"
}
ensure_jdk_with_jmods

export JAVA_HOME
JPACKAGE="$JAVA_HOME/bin/jpackage"

JAVAFX_VERSION="25.0.3"
JAR="target/UltimateCryptoSuite-1.0-SNAPSHOT-obfuscated.jar"
MAIN_CLASS="app.Launcher"
APP_NAME="UltimateCryptoSuite"
APP_VENDOR="emilyedward"
APP_VERSION="1.0.0"
JMODS_DIR="$PROJECT_DIR/target/jmods"
INPUT_DIR="$PROJECT_DIR/target/jpackage-input"
OUT_DIR="$PROJECT_DIR/dist/native"

# JavaFX + JDK modules to embed in the runtime image.
# java.management is required by TamperGuard (ManagementFactory);
# java.net.http/java.naming/java.security.jgss/jdk.crypto.ec by ApiClient/Mongo.
APP_MODULES="java.base,java.logging,java.management,java.desktop,java.xml,java.net.http,java.naming,java.security.jgss,jdk.crypto.ec,jdk.unsupported"
APP_MODULES="$APP_MODULES,javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media"

# JVM options for the packaged app.
JVM_OPTS="--enable-native-access=javafx.graphics"
JVM_OPTS="$JVM_OPTS --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
JVM_OPTS="$JVM_OPTS -Xms128m -Xmx1g"

case "$OS" in
    linux)
        PLATFORM="linux"
        CLASSIFIER="linux-x64"
        ICON="packaging/icons/linux/ucs.png"
        ;;
    windows)
        PLATFORM="windows"
        CLASSIFIER="windows-x64"
        ICON="packaging/icons/windows/ucs.ico"
        ;;
    macos)
        PLATFORM="macos"
        # osx-x64 for Intel, osx-aarch64 for Apple Silicon. Detect arch.
        if [ "$(uname -m)" = "arm64" ]; then
            CLASSIFIER="osx-aarch64"
        else
            CLASSIFIER="osx-x64"
        fi
        ICON="packaging/icons/macos/ucs.icns"
        ;;
    *)
        echo "Unknown OS: $OS (use linux|windows|macos)" >&2
        exit 2
        ;;
esac

echo "==> [1/5] Ensuring JavaFX jmods ($JAVAFX_VERSION, $CLASSIFIER)"
if [[ ! -d "$JMODS_DIR/javafx-jmods-${JAVAFX_VERSION}" ]]; then
    mkdir -p "$JMODS_DIR"
    JMODS_ZIP="openjfx-${JAVAFX_VERSION}_${CLASSIFIER}_bin-jmods.zip"
    JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/${JMODS_ZIP}"
    echo "    downloading $JMODS_URL"
    curl -fsSL --retry 3 --retry-all-errors "$JMODS_URL" -o "$JMODS_DIR/$JMODS_ZIP"
    unzip -q -o "$JMODS_DIR/$JMODS_ZIP" -d "$JMODS_DIR"
    rm -f "$JMODS_DIR/$JMODS_ZIP"
fi

echo "==> [2/5] Building thin obfuscated jar (-Pnative -Pobfuscate)"
mvn -q -Pnative -Pobfuscate -DskipTests verify

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: $JAR not produced" >&2
    exit 1
fi

echo "==> [3/5] Injecting anti-tamper hash"
python3 scripts/hash-and-sign.py "$JAR"

echo "==> [4/5] Staging input dir"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
cp "$JAR" "$INPUT_DIR/UltimateCryptoSuite.jar"

mkdir -p "$OUT_DIR"
# Remove stale app-image so jpackage can rebuild it.
rm -rf "$OUT_DIR/$APP_NAME"

COMMON_OPTS=(
    --type app-image
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$APP_VENDOR"
    --input "$INPUT_DIR"
    --main-jar UltimateCryptoSuite.jar
    --main-class "$MAIN_CLASS"
    --module-path "$JMODS_DIR/javafx-jmods-${JAVAFX_VERSION}"
    --add-modules "$APP_MODULES"
    --java-options "$JVM_OPTS"
    --dest "$OUT_DIR"
)
if [[ -f "$ICON" ]]; then
    COMMON_OPTS+=(--icon "$ICON")
fi

echo "==> [5/5] Running jpackage for $OS"
if [[ "$ONLY_IMAGE" == "--app-image-only" ]]; then
    "$JPACKAGE" "${COMMON_OPTS[@]}" >/dev/null
    echo "    app-image -> $OUT_DIR/$APP_NAME"
    exit 0
fi

case "$OS" in
    linux)
        "$JPACKAGE" "${COMMON_OPTS[@]}" --type deb >/dev/null
        echo "    deb -> $OUT_DIR"
        # Post-process the deb so the app menu shows the icon reliably:
        # installs the icon into the hicolor theme + a proper .desktop entry.
        DEB_FILE=""
        shopt -s nullglob
        DEB_CANDIDATES=( "$OUT_DIR"/ultimatecryptosuite_*_"$PLATFORM"*.deb "$OUT_DIR"/ultimatecryptosuite_*.deb )
        shopt -u nullglob
        [ -n "${DEB_CANDIDATES[0]:-}" ] && DEB_FILE="${DEB_CANDIDATES[0]}"
        if [ -n "${DEB_FILE:-}" ]; then
            "$PROJECT_DIR/scripts/fix-deb-desktop.sh" "$DEB_FILE" "$ICON"
        fi
        # AppImage (optional; requires appimagetool on PATH). Never fatal.
        if command -v appimagetool >/dev/null 2>&1; then
            if "$JPACKAGE" "${COMMON_OPTS[@]}" >/dev/null 2>&1 && \
               ARCH=x86_64 appimagetool --appimage-extract-and-run \
                   "$OUT_DIR/$APP_NAME" "$OUT_DIR/${APP_NAME}-${APP_VERSION}-x86_64.AppImage" >/dev/null 2>&1; then
                echo "    appimage -> $OUT_DIR"
            else
                echo "    [skip] AppImage build failed (non-fatal); deb is the primary artifact"
            fi
        else
            echo "    [skip] appimagetool not found; app-image still produced at $OUT_DIR/$APP_NAME"
        fi
        ;;
    windows)
        # exe (with Inno Setup wizard if ISCC present) or fallback msi via jpackage
        if command -v ISCC >/dev/null 2>&1; then
            "$JPACKAGE" "${COMMON_OPTS[@]}" >/dev/null
            ISCC //F"$OUT_DIR/${APP_NAME}-Setup" packaging/innosetup/ucs.iss >/dev/null
            echo "    exe -> $OUT_DIR/${APP_NAME}-Setup.exe"
        else
            "$JPACKAGE" "${COMMON_OPTS[@]}" --type msi >/dev/null
            echo "    msi -> $OUT_DIR"
        fi
        ;;
    macos)
        "$JPACKAGE" "${COMMON_OPTS[@]}" --type dmg >/dev/null
        echo "    dmg -> $OUT_DIR"
        # pkg (requires productbuild, present on macOS)
        "$JPACKAGE" "${COMMON_OPTS[@]}" --type pkg >/dev/null
        echo "    pkg -> $OUT_DIR"
        ;;
esac

echo "==> done. Artifacts in $OUT_DIR"
