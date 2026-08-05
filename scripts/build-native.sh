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

# JavaFX modules to embed in the runtime image.
JAVAFX_MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.media"

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
        CLASSIFIER="win-x64"
        ICON="packaging/icons/windows/ucs.ico"
        ;;
    macos)
        PLATFORM="macos"
        CLASSIFIER="mac-x64"
        ICON="packaging/icons/macos/ucs.icns"
        ;;
    *)
        echo "Unknown OS: $OS (use linux|windows|macos)" >&2
        exit 2
        ;;
esac

echo "==> [1/5] Building thin obfuscated jar (-Pnative -Pobfuscate)"
mvn -q -Pnative -Pobfuscate -DskipTests verify

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: $JAR not produced" >&2
    exit 1
fi

echo "==> [2/5] Injecting anti-tamper hash"
python3 scripts/hash-and-sign.py "$JAR"

echo "==> [3/5] Ensuring JavaFX jmods ($JAVAFX_VERSION, $CLASSIFIER)"
if [[ ! -d "$JMODS_DIR" ]]; then
    mkdir -p "$JMODS_DIR"
    JMODS_ZIP="openjfx-${JAVAFX_VERSION}_${CLASSIFIER}_bin-jmods.zip"
    JMODS_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/${JMODS_ZIP}"
    echo "    downloading $JMODS_URL"
    curl -fsSL "$JMODS_URL" -o "$JMODS_DIR/$JMODS_ZIP"
    unzip -q -o "$JMODS_DIR/$JMODS_ZIP" -d "$JMODS_DIR"
    rm -f "$JMODS_DIR/$JMODS_ZIP"
fi

echo "==> [4/5] Staging input dir"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
cp "$JAR" "$INPUT_DIR/UltimateCryptoSuite.jar"

mkdir -p "$OUT_DIR"

COMMON_OPTS=(
    --type app-image
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$APP_VENDOR"
    --input "$INPUT_DIR"
    --main-jar UltimateCryptoSuite.jar
    --main-class "$MAIN_CLASS"
    --module-path "$JMODS_DIR/javafx-jmods-${JAVAFX_VERSION}"
    --add-modules "$JAVAFX_MODULES"
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
        # AppImage (optional; requires appimagetool on PATH)
        if command -v appimagetool >/dev/null 2>&1; then
            "$JPACKAGE" "${COMMON_OPTS[@]}" >/dev/null
            ARCH=x86_64 appimagetool "$OUT_DIR/$APP_NAME" "$OUT_DIR/${APP_NAME}-${APP_VERSION}-x86_64.AppImage" >/dev/null
            echo "    appimage -> $OUT_DIR"
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
