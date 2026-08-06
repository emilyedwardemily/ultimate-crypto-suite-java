#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  ULTIMATE CRYPTO SUITE — GRAPHICAL INSTALLER (Linux)
#
#  Self-extracting installer: runs a zenity/kdialog wizard ending
#  on a "Finish" button, then installs the app + bundled Java 25
#  runtime into the current user's ~/.local (no root required).
#
#  Layout produced (XdgBaseDir friendly):
#    ~/.local/share/UltimateCryptoSuite/{bin,lib,share}
#    ~/.local/share/applications/ultimatecryptosuite.desktop
#    ~/.local/bin/ultimatecryptosuite          (launcher symlink)
#  ═══════════════════════════════════════════════════════════════

set -u

APP_NAME="Ultimate Crypto Suite"
APP_VERSION="1.0.0"
DEFAULT_DEST="${HOME}/.local/share/UltimateCryptoSuite"
BIN_LINK="${HOME}/.local/bin/ultimatecryptosuite"

PAYLOAD_MARKER="__UC_SUITE_PAYLOAD_END_7f3a9c__"

tmpdir=""
deb_file=""
dest="$DEFAULT_DEST"

# ── cleanup ────────────────────────────────────────────────────
cleanup() {
    [ -n "$tmpdir" ] && [ -d "$tmpdir" ] && rm -rf "$tmpdir"
}
trap cleanup EXIT

# ── dialog backend ─────────────────────────────────────────────
# Override with UC_DIALOG=none for headless/scripted installs.
DIALOG="none"
if [ "${UC_DIALOG:-auto}" != "none" ]; then
    if command -v zenity >/dev/null 2>&1; then
        DIALOG="zenity"
    elif command -v kdialog >/dev/null 2>&1; then
        DIALOG="kdialog"
    fi
fi

dlg_error() { # msg
    case "$DIALOG" in
        zenity) zenity --error --title="$APP_NAME Installer" --text="$1" --width=420 ;;
        kdialog) kdialog --error "$1" ;;
        *) echo "ERROR: $1" >&2 ;;
    esac
}

dlg_question() { # text ok_label
    local text="$1" ok="$2"
    case "$DIALOG" in
        zenity) zenity --question --title="$APP_NAME Installer" --text="$text" --ok-label="$ok" --cancel-label="Cancel" --width=460 ;;
        kdialog) kdialog --yesno "$text" --yes-label "$ok" --no-label "Cancel" ;;
        *)
            echo "────────────────────────────────────────────"
            echo "$text"
            read -r -p "Press Enter to $ok or type 'q' to quit: " ans
            if [ "${ans:-}" = "q" ]; then return 1; else return 0; fi
            ;;
    esac
}

dlg_directory() { # returns chosen dir or empty for default
    case "$DIALOG" in
        zenity) zenity --file-selection --directory --title="Choose installation folder" --filename="$DEFAULT_DEST/" 2>/dev/null ;;
        kdialog) kdialog --getexistingdirectory "$DEFAULT_DEST" ;;
        *) echo "$DEFAULT_DEST" ;;
    esac
}

dlg_progress() { # msg  → run extraction in background while pulsing
    local msg="$1" start=$(( $(date +%s) ))
    case "$DIALOG" in
        zenity)
            (
                while [ -e "$tmpdir/.installing" ]; do
                    echo "30"
                    sleep 0.4
                done
                echo "100"
            ) | zenity --progress --pulsate --auto-close --no-cancel \
                  --title="$APP_NAME Installer" --text="$msg" --width=460 2>/dev/null
            ;;
        kdialog)
            while [ -e "$tmpdir/.installing" ]; do sleep 0.4; done
            kdialog --progressbar "$msg" 0 >/dev/null 2>&1 &
            ;;
        *) echo "$msg" ;;
    esac
}

dlg_finish() { # msg  → the "Finish" button
    local msg="$1"
    case "$DIALOG" in
        zenity) zenity --info --title="$APP_NAME Installer" --text="$msg" --ok-label="Finish" --width=460 ;;
        kdialog) kdialog --msgbox "$msg" --title "Installation complete" ;;
        *) echo "$msg" ;;
    esac
}

# ── self-extraction ────────────────────────────────────────────
extract_payload() {
    local offset
    offset=$(grep -abo "$PAYLOAD_MARKER" "$0" | tail -1 | cut -d: -f1)
    if [ -z "$offset" ]; then
        dlg_error "This installer appears to be corrupted (payload marker missing)."
        exit 1
    fi
    tmpdir="$(mktemp -d)"
    tail -c +$((offset + ${#PAYLOAD_MARKER} + 2)) "$0" > "$tmpdir/package.deb"
    [ -s "$tmpdir/package.deb" ] || { dlg_error "Installer payload is empty."; exit 1; }
    deb_file="$tmpdir/package.deb"
}

# ── unpack .deb without root ───────────────────────────────────
unpack_deb() { # src.deb destdir
    local src="$1" out="$2"
    mkdir -p "$out"
    if command -v dpkg-deb >/dev/null 2>&1; then
        dpkg-deb -x "$src" "$out"
    else
        # Fallback: ar + data.tar
        ( cd "$out" && ar x "$src" data.tar.* 2>/dev/null && \
          for t in data.tar.*; do tar -xf "$t" -C "$out" 2>/dev/null && rm -f "$t"; done )
    fi
}

# ── install steps ──────────────────────────────────────────────
create_launchers() {
    local base="$dest/opt/ultimatecryptosuite"
    local exe="$base/bin/UltimateCryptoSuite"
    local icon="$base/lib/UltimateCryptoSuite.png"

    # desktop entry for the app menu
    mkdir -p "${HOME}/.local/share/applications"
    cat > "${HOME}/.local/share/applications/ultimatecryptosuite.desktop" <<EOF
[Desktop Entry]
Name=Ultimate Crypto Suite
Comment=Ultimate Crypto Suite - secure cryptography workstation
Exec=${exe}
Icon=${icon}
Terminal=false
Type=Application
Categories=Utility;Security;
MimeType=
EOF
    chmod +x "${HOME}/.local/share/applications/ultimatecryptosuite.desktop"

    # launcher on PATH
    mkdir -p "${HOME}/.local/bin"
    ln -sf "$exe" "$BIN_LINK"

    command -v update-desktop-database >/dev/null 2>&1 && \
        update-desktop-database "${HOME}/.local/share/applications" 2>/dev/null
}

# ── main wizard ────────────────────────────────────────────────
extract_payload

if ! dlg_question "Welcome to the $APP_NAME ${APP_VERSION} installer.\n\nThis will install the application with its bundled Java 25 runtime — no Java, terminal or code required.\n\nInstallation size: ~350 MB\nLocation: ${DEFAULT_DEST}\n\nContinue?" "Next"; then
    exit 1
fi

case "$DIALOG" in
    zenity|kdialog)
        chosen="$(dlg_directory)"
        [ -n "$chosen" ] && dest="$chosen"
        ;;
    *) dest="$DEFAULT_DEST" ;;
esac

# confirm destination
if ! dlg_question "Installation folder:\n  ${dest}\n\nA desktop entry and a 'ultimatecryptosuite' command will also be added." "Install"; then
    exit 1
fi

touch "$tmpdir/.installing"
dlg_progress "Installing $APP_NAME... please wait." &
dlg_pid=$!

if ! unpack_deb "$deb_file" "$dest" 2>/dev/null; then
    rm -f "$tmpdir/.installing"
    wait "$dlg_pid" 2>/dev/null
    dlg_error "Installation failed while unpacking. Try again or install the .deb with a package manager."
    exit 1
fi

create_launchers
rm -f "$tmpdir/.installing"
wait "$dlg_pid" 2>/dev/null

# smoke-test the bundled launcher is executable
if [ -x "$dest/opt/ultimatecryptosuite/bin/UltimateCryptoSuite" ]; then
    dlg_finish "Installation complete!\n\nUltimate Crypto Suite was installed to:\n  ${dest}\n\nLaunch it from your applications menu or with:\n  ${BIN_LINK}\n\nClick Finish to close this installer."
    exit 0
fi

dlg_error "Installer finished but the application launcher was not found.\nPlease re-run the installer."
exit 1
