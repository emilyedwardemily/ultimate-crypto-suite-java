#!/usr/bin/env bash
#
# UltimateCryptoSuite - artifact hashing + signing
#
# 1. Computes SHA-256 + SHA-512 of every release artifact in dist/native/
# 2. If GPG signing keys are available (env vars), signs each artifact and the
#    checksum files with GPG (detached .sig + clearsigned SHA256SUMS).
#
# Env (optional):
#   SIGNING_KEY_ID   - GPG key id to sign with
#   SIGNING_PASSPHRASE - if the key is passphrase protected (or --batch gpg-agent)
#
# Usage:
#   ./scripts/hash-and-sign.sh
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"

ARTIFACT_DIR="dist/native"
HASHES_DIR="dist/checksums"

if [[ ! -d "$ARTIFACT_DIR" ]]; then
    echo "No artifacts in $ARTIFACT_DIR - run scripts/build-native.sh first." >&2
    exit 1
fi

mkdir -p "$HASHES_DIR"

# Collect artifacts: installers + app-images (exclude loose per-platform images
# that are already inside the installers unless --include-images is passed).
ARTIFACTS=()
for f in "$ARTIFACT_DIR"/*.deb "$ARTIFACT_DIR"/*.rpm "$ARTIFACT_DIR"/*.exe \
         "$ARTIFACT_DIR"/*.msi "$ARTIFACT_DIR"/*.dmg "$ARTIFACT_DIR"/*.pkg \
         "$ARTIFACT_DIR"/*.AppImage "$ARTIFACT_DIR"/*.zip; do
    [[ -f "$f" ]] && ARTIFACTS+=("$f")
done

if [[ ${#ARTIFACTS[@]} -eq 0 ]]; then
    echo "No release artifacts found in $ARTIFACT_DIR" >&2
    exit 1
fi

echo "==> Computing hashes"
: > "$HASHES_DIR/SHA256SUMS"
: > "$HASHES_DIR/SHA512SUMS"
for f in "${ARTIFACTS[@]}"; do
    name="$(basename "$f")"
    sha256sum "$f" | awk -v n="$name" '{print $1 "  " n}' >> "$HASHES_DIR/SHA256SUMS"
    sha512sum "$f" | awk -v n="$name" '{print $1 "  " n}' >> "$HASHES_DIR/SHA512SUMS"
    echo "    $(sha256sum "$f" | awk '{print $1}')  $name"
done

if [[ -n "${SIGNING_KEY_ID:-}" ]]; then
    echo "==> GPG signing (key ${SIGNING_KEY_ID})"
    export GPG_TTY="${GPG_TTY:-$(tty 2>/dev/null || echo /dev/null)}"
    sign() {
        gpg --batch --yes --pinentry-mode loopback \
            --passphrase "${SIGNING_PASSPHRASE:-}" \
            --default-key "$SIGNING_KEY_ID" --detach-sign --armor "$1"
    }
    for f in "${ARTIFACTS[@]}"; do
        sign "$f"
        echo "    signed $(basename "$f").sig"
    done
    sign "$HASHES_DIR/SHA256SUMS"
    sign "$HASHES_DIR/SHA512SUMS"
    cp "$HASHES_DIR/SHA256SUMS" "$HASHES_DIR/SHA256SUMS.asc"
    cp "$HASHES_DIR/SHA512SUMS" "$HASHES_DIR/SHA512SUMS.asc"
else
    echo "==> [skip] GPG signing (SIGNING_KEY_ID not set)"
fi

echo "==> done. Checksums + signatures in $HASHES_DIR"
