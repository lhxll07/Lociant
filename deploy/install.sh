#!/usr/bin/env bash
# Installs the cross-compiled Rust backend as a systemd service on a headless
# board. Usage: bash deploy/install.sh [path-to-lociant-server-binary]
set -euo pipefail

BIN="${1:-$(dirname "$0")/../apps/rust-backend/target/aarch64-unknown-linux-gnu/release/lociant-server}"
SERVICE_USER="${SUDO_USER:-${USER:-$(id -un)}}"
SERVICE_GROUP="$(id -gn "$SERVICE_USER")"
USER_HOME="$(getent passwd "$SERVICE_USER" | cut -d: -f6)"
if [[ -z "$USER_HOME" ]]; then
  echo "could not resolve home directory for service user: $SERVICE_USER" >&2
  exit 1
fi
DATA_DIR="$USER_HOME/lociant/data"

if [[ ! -x "$BIN" ]]; then
  echo "binary not found or not executable: $BIN" >&2
  exit 1
fi

sudo install -m 0755 "$BIN" /usr/local/bin/lociant-server
sudo install -d -o "$SERVICE_USER" -g "$SERVICE_GROUP" /etc/lociant "$DATA_DIR"
SERVICE_TMP="$(mktemp)"
trap 'rm -f "$SERVICE_TMP"' EXIT
sed \
  -e "s|@LOCIANT_USER@|$SERVICE_USER|g" \
  -e "s|@LOCIANT_GROUP@|$SERVICE_GROUP|g" \
  -e "s|@LOCIANT_DATA_DIR@|$DATA_DIR|g" \
  "$(dirname "$0")/lociant.service" > "$SERVICE_TMP"
sudo install -m 0644 "$SERVICE_TMP" /etc/systemd/system/lociant.service

if [[ ! -f /etc/lociant/config.json ]]; then
  sudo install -m 0644 "$(dirname "$0")/config.example.json" /etc/lociant/config.json
  echo "installed default config at /etc/lociant/config.json — edit authToken/cloud settings before use"
fi

sudo systemctl daemon-reload
sudo systemctl enable --now lociant
systemctl --no-pager status lociant --lines=5
