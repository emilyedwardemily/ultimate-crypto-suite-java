#!/usr/bin/env bash
#
# Fix .deb desktop/icon integration for UltimateCryptoSuite.
#
# jpackage's default .deb references the runtime icon by an absolute path
# (/opt/.../lib/UltimateCryptoSuite.png) and ships a bare .desktop with
# Categories=Unknown. Many desktop shells (GNOME/KDE on Kali/Debian) then
# fail to render the launcher icon until caches are manually refreshed.
#
# This script re-packs the .deb so that:
#   * the icon is installed into the hicolor icon theme with a stable name
#   * the .desktop entry uses Icon=ultimatecryptosuite with proper metadata
#   * the postinst refreshes desktop + icon caches on install
#
# Usage:
#   scripts/fix-deb-desktop.sh <path-to.deb> <path-to-icon.png>
#
set -euo pipefail

DEB="$1"
ICON="$2"
[ -f "$DEB" ] || { echo "ERROR: deb not found: $DEB" >&2; exit 1; }
[ -f "$ICON" ] || { echo "ERROR: icon not found: $ICON" >&2; exit 1; }

APP_NAME="ultimatecryptosuite"
ICON_NAME="ultimatecryptosuite"
DISPLAY_NAME="Ultimate Crypto Suite"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "==> Re-packing $DEB for proper icon/desktop integration"

# Extract data (payload) into $WORK/data and control into $WORK/data/DEBIAN
rm -rf "$WORK/data"
mkdir -p "$WORK/data"
dpkg-deb -x "$DEB" "$WORK/data"

# --- 0. Put the control scripts inside DEBIAN/ so dpkg-deb -b reuses them ---
mkdir -p "$WORK/data/DEBIAN"
dpkg-deb -e "$DEB" "$WORK/data/DEBIAN"

# --- 1. Install icon into the hicolor theme (multiple sizes) ---
HAS_CONVERT=0; command -v convert >/dev/null 2>&1 && HAS_CONVERT=1
for SIZE in 512 256 128 64 48 32; do
    DIR="$WORK/data/usr/share/icons/hicolor/${SIZE}x${SIZE}/apps"
    mkdir -p "$DIR"
    if [ "$HAS_CONVERT" = 1 ]; then
        convert "$ICON" -resize "${SIZE}x${SIZE}" "$DIR/$ICON_NAME.png"
    else
        cp "$ICON" "$DIR/$ICON_NAME.png"
    fi
done

# --- 2. Write a proper applications .desktop entry (uses icon NAME) ---
mkdir -p "$WORK/data/usr/share/applications"
cat > "$WORK/data/usr/share/applications/$APP_NAME.desktop" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=Ultimate Crypto Suite
GenericName=Cryptography Workstation
Comment=Ultimate Crypto Suite - secure cryptography workstation
Exec=/opt/ultimatecryptosuite/bin/UltimateCryptoSuite
Icon=${ICON_NAME}
Terminal=false
Categories=Utility;Security;Encryption;
Keywords=crypto;encryption;security;
MimeType=
EOF

# Also correct the runtime copy jpackage installs (used as a fallback)
RUNTIME_DESKTOP="$WORK/data/opt/ultimatecryptosuite/lib/${APP_NAME}-UltimateCryptoSuite.desktop"
if [ -f "$RUNTIME_DESKTOP" ]; then
    cp "$WORK/data/usr/share/applications/$APP_NAME.desktop" "$RUNTIME_DESKTOP"
fi

# --- 3. Patch postinst to refresh caches ---
POSTINST="$WORK/data/DEBIAN/postinst"
if [ -f "$POSTINST" ]; then
    # Remove jpackage's xdg-desktop-menu line (we install our own entry) and
    # insert reliable cache refreshes before the final exit 0.
    sed -i 's|^xdg-desktop-menu install.*||' "$POSTINST"
    if ! grep -q "update-desktop-database" "$POSTINST"; then
        sed -i '/^exit 0/i \
update-desktop-database /usr/share/applications >/dev/null 2>\&1 || true\
gtk-update-icon-cache -f /usr/share/icons/hicolor >/dev/null 2>\&1 || true\
xdg-desktop-menu forceupdate >/dev/null 2>\&1 || true' "$POSTINST"
    fi
fi

# --- 4. Rebuild the .deb from data (control scripts now under data/DEBIAN) ---
NEW="$WORK/$(basename "$DEB")"
dpkg-deb -b "$WORK/data" "$NEW" >/dev/null

mv "$NEW" "$DEB"
echo "==> Patched $DEB"
echo "    icon  -> /usr/share/icons/hicolor/*/apps/$ICON_NAME.png"
echo "    .desktop -> Icon=$ICON_NAME, Categories=Utility;Security;Encryption;"
echo "    postinst now refreshes desktop/icon caches on install"
