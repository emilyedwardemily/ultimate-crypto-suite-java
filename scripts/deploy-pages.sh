#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  ULTIMATE CRYPTO SUITE — DEPLOY TO GITHUB PAGES
#
#  emilyedward.me is served from the separate
#  emilyedwardemily/emilyedwardemily.github.io repository via
#  GitHub Pages. This script syncs the landing page + installers
#  from this repo to the Pages repo and pushes them live.
#
#  Usage:
#    scripts/deploy-pages.sh              sync current dist/ artifacts
#    scripts/deploy-pages.sh --build      rebuild installers first
#  ═══════════════════════════════════════════════════════════════

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PAGES_REPO="https://github.com/emilyedwardemily/emilyedwardemily.github.io.git"
PAGES_DIR="${PAGES_DIR:-$PROJECT_DIR/.pages-deploy}"
SITE_BRANCH="main"

BUILD=0
[ "${1:-}" = "--build" ] && BUILD=1

cd "$PROJECT_DIR"

if [ "$BUILD" = 1 ]; then
    echo "==> Rebuilding native installers (this takes a few minutes)..."
    JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}" ./scripts/build-native.sh linux
    ./scripts/build-run.sh
    echo "==> Copying portable jar from Maven output"
    JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}" mvn -q -DskipTests package
    cp target/UltimateCryptoSuite-1.0-SNAPSHOT.jar dist/UltimateCryptoSuite.jar
fi

# sanity: the artifacts we are about to publish must exist
for f in index.html script.js style.css \
         dist/UltimateCryptoSuite.jar \
         dist/native/ultimatecryptosuite_1.0.0_amd64.deb \
         dist/native/UltimateCryptoSuite-1.0.0-linux.run; do
    [ -f "$f" ] || { echo "ERROR: missing $f — run with --build first" >&2; exit 1; }
done

echo "==> Ensuring Pages working copy (first clone is large: ~110 MB installers)"
if [ -d "$PAGES_DIR/.git" ]; then
    git -C "$PAGES_DIR" fetch --quiet origin "$SITE_BRANCH"
    git -C "$PAGES_DIR" reset --quiet --hard "origin/$SITE_BRANCH"
else
    rm -rf "$PAGES_DIR"
    mkdir -p "$(dirname "$PAGES_DIR")"
    # the first clone pulls ~110 MB; GitHub's pack transfer can drop the
    # connection, so retry a few times before giving up.
    for attempt in 1 2 3 4 5; do
        if git clone --quiet --branch "$SITE_BRANCH" "$PAGES_REPO" "$PAGES_DIR" 2>/dev/null; then
            break
        fi
        echo "    clone attempt $attempt failed (network). retrying..."
        rm -rf "$PAGES_DIR"
        sleep 3
    done
    [ -d "$PAGES_DIR/.git" ] || { echo "ERROR: could not clone Pages repo" >&2; exit 1; }
fi

echo "==> Syncing landing page + installers"
cp index.html script.js style.css "$PAGES_DIR/"
mkdir -p "$PAGES_DIR/dist/native"
cp dist/UltimateCryptoSuite.jar "$PAGES_DIR/dist/"
cp dist/native/ultimatecryptosuite_1.0.0_amd64.deb "$PAGES_DIR/dist/native/"
cp dist/native/UltimateCryptoSuite-1.0.0-linux.run "$PAGES_DIR/dist/native/"

cd "$PAGES_DIR"

# only commit if something actually changed
if git diff --quiet && git diff --cached --quiet; then
    echo "==> No changes to publish."
    exit 0
fi

git add -A
git commit --quiet -m "Deploy UltimateCryptoSuite landing page + installers

- Landing page (index/script/style)
- Portable .jar (credential-free)
- Native .deb + .run wizard installer (bundled Java 25)"
echo "==> Pushing to GitHub Pages repo ($SITE_BRANCH)"
git push --quiet origin "$SITE_BRANCH"
echo "==> Done. https://emilyedward.me will update within ~2 minutes."
