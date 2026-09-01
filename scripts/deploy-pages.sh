#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  ULTIMATE CRYPTO SUITE — DEPLOY TO GITHUB PAGES
#
#  emilyedward.me is served from the separate
#  emilyedwardemily/emilyedwardemily.github.io repository via
#  GitHub Pages.
#
#  DESIGN (for reliability):
#   * This machine's network tunnels large >30MB uploads regardless of
#     HTTPS/SSH transport, so the Pages repo carries NO installers.
#   * All installers (.deb/.run/.msi/.dmg/.pkg/.jar) are served from the
#     GitHub Release assets (v1.0.0) — the website's index.html points the
#     download buttons straight at those release URLs (they return 200).
#   * This script only pushes small site files (HTML/CSS/JS/favicon/
#     manifest/branding) over SSH, so it can never stall on a big pack.
#
#  Prereq: SSH deploy key (~/.ssh/ucs-pages-deploy) registered as a write
#          deploy key on emilyedwardemily/emilyedwardemily.github.io, and a
#          Host alias "githubpages-deploy" in ~/.ssh/config.
#
#  Usage:
#    scripts/deploy-pages.sh
#  ═══════════════════════════════════════════════════════════════

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PAGES_URL="git@githubpages-deploy:emilyedwardemily/emilyedwardemily.github.io.git"
PAGES_DIR="${PAGES_DIR:-$PROJECT_DIR/.pages-deploy}"
SITE_BRANCH="main"
GIT_SSH_COMMAND="${GIT_SSH_COMMAND:-ssh -o BatchMode=yes}"
export GIT_SSH_COMMAND

cd "$PROJECT_DIR"

# sanity: the site files we publish must exist
for f in index.html script.js style.css favicon.ico site.webmanifest \
         assets/branding/logo.png assets/branding/favicon-16x16.png \
         assets/branding/favicon-32x32.png assets/branding/favicon-192.png \
         assets/branding/apple-touch-icon.png assets/branding/icon-512.png; do
    [ -f "$f" ] || { echo "ERROR: missing $f" >&2; exit 1; }
done

echo "==> Ensuring Pages working copy (over SSH)"
if [ -d "$PAGES_DIR/.git" ]; then
    git -C "$PAGES_DIR" fetch --quiet origin "$SITE_BRANCH"
    git -C "$PAGES_DIR" reset --quiet --hard "origin/$SITE_BRANCH"
else
    rm -rf "$PAGES_DIR"
    mkdir -p "$(dirname "$PAGES_DIR")"
    for attempt in 1 2 3 4 5; do
        if git clone --quiet --branch "$SITE_BRANCH" "$PAGES_URL" "$PAGES_DIR" 2>/dev/null; then
            break
        fi
        echo "    clone attempt $attempt failed (SSH). retrying..."
        rm -rf "$PAGES_DIR"
        sleep 3
    done
    [ -d "$PAGES_DIR/.git" ] || { echo "ERROR: could not clone Pages repo" >&2; exit 1; }
fi

echo "==> Syncing site files (no installers — those live on GitHub Release)"
cp index.html script.js style.css favicon.ico site.webmanifest "$PAGES_DIR/"
mkdir -p "$PAGES_DIR/assets/branding"
cp -r assets/branding/. "$PAGES_DIR/assets/branding/"

# Remove any leftover binaries that may have been committed before this
# change so the Pages repo stays small and uploads can never stall.
git -C "$PAGES_DIR" rm -rq --ignore-unmatch dist 2>/dev/null || true

cd "$PAGES_DIR"

# only commit if something actually changed
if git diff --quiet && git diff --cached --quiet; then
    echo "==> No changes to publish."
    exit 0
fi

git add -A
git commit --quiet -m "Deploy UltimateCryptoSuite landing page (rebrand)

- Site: index/script/style + favicon/manifest/assets/branding
- Password: installers moved to GitHub Release assets (v1.0.0)"
echo "==> Pushing to GitHub Pages repo ($SITE_BRANCH) over SSH"
git push --quiet origin "$SITE_BRANCH"
echo "==> Done. https://emilyedward.me will update within ~2 minutes."
