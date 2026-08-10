#!/usr/bin/env bash
# Installs the cross-compiled Rust backend as a systemd service on a headless
# board. Usage: bash deploy/install.sh [path-to-lociant-server-binary]
set -euo pipefail

BIN="${1:-$(dirname "$0")/../apps/rust-backend/target/aarch64-unknown-linux-gnu/release/lociant-server}"
USER_HOME="$(eval echo "~$USER")"

if [[ ! -x "$BIN" ]]; then
  echo "binary not found or not executable: $BIN" >&2
  exit 1
fi

sudo install -m 0755 "$BIN" /usr/local/bin/lociant-server
sudo install -d -o "$USER" -g "$USER" /etc/lociant "$USER_HOME/lociant/data"
sudo install -m 0644 "$(dirname "$0")/lociant.service" /etc/systemd/system/lociant.service

if [[ ! -f /etc/lociant/config.json ]]; then
  sudo install -m 0644 "$(dirname "$0")/config.example.json" /etc/lociant/config.json
  echo "installed default config at /etc/lociant/config.json — edit authToken/cloud settings before use"
fi

sudo systemctl daemon-reload
sudo systemctl enable --now lociant
systemctl --no-pager status lociant --lines=5
