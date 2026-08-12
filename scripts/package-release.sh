#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-2.0.1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$ROOT/dist"
ANDROID_APK="$ROOT/apps/android/app/build/outputs/apk/release/app-release.apk"
FLUTTER_BUNDLE="$ROOT/apps/flutter/build/linux/x64/release/bundle"
RUST_TARGET="$ROOT/apps/rust-backend/target"
X64_SERVER="$RUST_TARGET/x86_64-unknown-linux-gnu/release/lociant-server"
ARM64_SERVER="$RUST_TARGET/aarch64-unknown-linux-gnu/release/lociant-server"
ARM64_TUI="$RUST_TARGET/aarch64-unknown-linux-gnu/release/lociant-tui"

for path in "$ANDROID_APK" "$FLUTTER_BUNDLE/lociant_flutter" \
    "$X64_SERVER" "$ARM64_SERVER" "$ARM64_TUI"; do
    if [[ ! -e "$path" ]]; then
        echo "missing release artifact: $path" >&2
        exit 1
    fi
done

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$DIST"

APK_NAME="lociant-$VERSION-arm64-v8a-release.apk"
X64_NAME="lociant-$VERSION-linux-x86_64"
ARM64_NAME="lociant-$VERSION-linux-aarch64"
DESKTOP_DEB="lociant_${VERSION}_amd64.deb"
NODE_DEB="lociant-node_${VERSION}_arm64.deb"

install -m 0644 "$ANDROID_APK" "$WORK/$APK_NAME"

mkdir -p "$WORK/$X64_NAME"
cp -a "$FLUTTER_BUNDLE/." "$WORK/$X64_NAME/"
install -m 0755 "$X64_SERVER" "$WORK/$X64_NAME/bin/lociant-server"
tar -C "$WORK" -czf "$WORK/$X64_NAME.tar.gz" "$X64_NAME"

mkdir -p "$WORK/$ARM64_NAME/deploy"
install -m 0755 "$ARM64_SERVER" "$WORK/$ARM64_NAME/lociant-server"
install -m 0755 "$ARM64_TUI" "$WORK/$ARM64_NAME/lociant-tui"
install -m 0755 "$ROOT/deploy/install.sh" "$WORK/$ARM64_NAME/deploy/install.sh"
install -m 0644 "$ROOT/deploy/lociant.service" "$WORK/$ARM64_NAME/deploy/lociant.service"
install -m 0644 "$ROOT/deploy/config.example.json" "$WORK/$ARM64_NAME/deploy/config.example.json"
tar -C "$WORK" -czf "$WORK/$ARM64_NAME.tar.gz" "$ARM64_NAME"

build_deb() {
    local package_root="$1"
    local output="$2"
    local deb_work
    deb_work="$(mktemp -d "$WORK/deb.XXXXXX")"
    printf '2.0\n' > "$deb_work/debian-binary"
    tar -C "$package_root/DEBIAN" --owner=0 --group=0 -cJf "$deb_work/control.tar.xz" .
    tar -C "$package_root" --exclude=DEBIAN --owner=0 --group=0 -cJf "$deb_work/data.tar.xz" .
    (cd "$deb_work" && ar r "$WORK/$output" debian-binary control.tar.xz data.tar.xz)
}

DESKTOP_ROOT="$WORK/desktop-deb"
mkdir -p "$DESKTOP_ROOT/DEBIAN" "$DESKTOP_ROOT/opt/lociant" \
    "$DESKTOP_ROOT/usr/bin" "$DESKTOP_ROOT/usr/share/applications"
cp -a "$FLUTTER_BUNDLE/." "$DESKTOP_ROOT/opt/lociant/"
install -m 0755 "$X64_SERVER" "$DESKTOP_ROOT/opt/lociant/bin/lociant-server"
ln -s /opt/lociant/lociant_flutter "$DESKTOP_ROOT/usr/bin/lociant"
install -m 0644 "$ROOT/packaging/linux/io.lociant.Lociant.desktop" \
    "$DESKTOP_ROOT/usr/share/applications/io.lociant.Lociant.desktop"
cat > "$DESKTOP_ROOT/DEBIAN/control" <<EOF
Package: lociant
Version: $VERSION
Section: utils
Priority: optional
Architecture: amd64
Maintainer: Lociant <noreply@lociant.io>
Depends: libc6, libstdc++6, libgtk-3-0 | libgtk-3-0t64
Installed-Size: $(du -sk "$DESKTOP_ROOT" | cut -f1)
Homepage: https://github.com/lhxll07/Lociant
Description: Local agent runtime and desktop client
 Lociant connects local models, device tools, and networked agent nodes.
EOF
build_deb "$DESKTOP_ROOT" "$DESKTOP_DEB"

NODE_ROOT="$WORK/node-deb"
mkdir -p "$NODE_ROOT/DEBIAN" "$NODE_ROOT/usr/bin" \
    "$NODE_ROOT/lib/systemd/system" "$NODE_ROOT/etc/lociant"
install -m 0755 "$ARM64_SERVER" "$NODE_ROOT/usr/bin/lociant-server"
install -m 0755 "$ARM64_TUI" "$NODE_ROOT/usr/bin/lociant-tui"
install -m 0644 "$ROOT/packaging/node/lociant.service" \
    "$NODE_ROOT/lib/systemd/system/lociant.service"
install -m 0640 "$ROOT/deploy/config.example.json" "$NODE_ROOT/etc/lociant/config.json"
install -m 0755 "$ROOT/packaging/node/postinst" "$NODE_ROOT/DEBIAN/postinst"
install -m 0755 "$ROOT/packaging/node/prerm" "$NODE_ROOT/DEBIAN/prerm"
install -m 0755 "$ROOT/packaging/node/postrm" "$NODE_ROOT/DEBIAN/postrm"
printf '/etc/lociant/config.json\n' > "$NODE_ROOT/DEBIAN/conffiles"
cat > "$NODE_ROOT/DEBIAN/control" <<EOF
Package: lociant-node
Version: $VERSION
Section: utils
Priority: optional
Architecture: arm64
Maintainer: Lociant <noreply@lociant.io>
Depends: adduser, libc6, libgcc-s1, systemd
Installed-Size: $(du -sk "$NODE_ROOT" | cut -f1)
Homepage: https://github.com/lhxll07/Lociant
Description: Lociant headless agent node for arm64
 Runs the Rust server, RKLLM integration, and terminal client as a systemd service.
EOF
build_deb "$NODE_ROOT" "$NODE_DEB"

for name in "$APK_NAME" "$X64_NAME.tar.gz" "$ARM64_NAME.tar.gz" \
    "$DESKTOP_DEB" "$NODE_DEB"; do
    install -m 0644 "$WORK/$name" "$DIST/$name"
done

(cd "$DIST" && sha256sum "$APK_NAME" "$X64_NAME.tar.gz" \
    "$ARM64_NAME.tar.gz" "$DESKTOP_DEB" "$NODE_DEB") > "$WORK/SHA256SUMS"
install -m 0644 "$WORK/SHA256SUMS" "$DIST/SHA256SUMS"

echo "release artifacts written to $DIST"
cat "$DIST/SHA256SUMS"
