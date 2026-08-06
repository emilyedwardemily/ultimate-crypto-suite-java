#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  Builds the self-extracting Linux GUI installer (.run)
#  by concatenating the .deb payload onto the wizard template.
#
#  Usage:  scripts/build-run.sh
#  Output: dist/native/UltimateCryptoSuite-1.0.0-linux.run
#  ═══════════════════════════════════════════════════════════════

set -eu

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATE="$PROJECT_DIR/packaging/linux/installer-wizard.sh"
DEB="$(ls -1 "$PROJECT_DIR"/dist/native/ultimatecryptosuite_*_amd64.deb 2>/dev/null | head -1)"
OUT="$PROJECT_DIR/dist/native/UltimateCryptoSuite-1.0.0-linux.run"

if [ ! -f "$DEB" ]; then
    echo "ERROR: no .deb found in dist/native/ — run build-native.sh first" >&2
    exit 1
fi

cp "$TEMPLATE" "$OUT"
chmod +x "$OUT"
printf '\n%s\n' "__UC_SUITE_PAYLOAD_END_7f3a9c__" >> "$OUT"
cat "$DEB" >> "$OUT"

echo "==> $OUT"
ls -la "$OUT"
echo "    payload: $(basename "$DEB")"
