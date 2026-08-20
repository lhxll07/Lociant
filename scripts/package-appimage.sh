#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-2.0.2}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="${DIST_DIR:-$ROOT/dist}"
BUNDLE="${FLUTTER_BUNDLE:-$ROOT/apps/flutter/build/linux/x64/release/bundle}"
APPIMAGETOOL="${APPIMAGETOOL:-}"

if [[ -z "$APPIMAGETOOL" ]]; then
    echo "APPIMAGETOOL must point to an executable appimagetool binary" >&2
    exit 1
fi
if [[ ! -x "$APPIMAGETOOL" ]]; then
    echo "appimagetool is not executable: $APPIMAGETOOL" >&2
    exit 1
fi
if [[ ! -f "$BUNDLE/lociant_flutter" || ! -d "$BUNDLE/data" || ! -d "$BUNDLE/lib" ]]; then
    echo "missing Flutter Linux release bundle: $BUNDLE" >&2
    exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
APPDIR="$WORK/Lociant.AppDir"
mkdir -p "$APPDIR/usr/lib/lociant" "$APPDIR/usr/bin" \
    "$APPDIR/usr/share/applications"
cp -a "$BUNDLE/." "$APPDIR/usr/lib/lociant/"

cat > "$APPDIR/AppRun" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec "$HERE/usr/lib/lociant/lociant_flutter" "$@"
EOF
chmod 0755 "$APPDIR/AppRun"

cat > "$APPDIR/usr/bin/lociant" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
HERE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
exec "$HERE/lib/lociant/lociant_flutter" "$@"
EOF
chmod 0755 "$APPDIR/usr/bin/lociant"

cp "$ROOT/packaging/linux/io.lociant.Lociant.desktop" \
    "$APPDIR/usr/share/applications/io.lociant.Lociant.desktop"

mkdir -p "$DIST"
OUTPUT="$DIST/lociant-$VERSION-linux-x86_64.AppImage"
rm -f "$OUTPUT"
APPIMAGE_EXTRACT_AND_RUN=1 "$APPIMAGETOOL" "$APPDIR" "$OUTPUT"
chmod 0755 "$OUTPUT"
echo "AppImage written to $OUTPUT"
