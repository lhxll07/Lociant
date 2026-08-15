#!/usr/bin/env bash
# Build the debug APK, install it on the connected device, and launch Lociant.
# Usage: bash scripts/dev-install.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/build-apk.sh"

APK="$ROOT/apps/android/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK"
adb shell am start -n io.lociant.android/.MainActivity
