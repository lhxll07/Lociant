#!/usr/bin/env bash
# Build the debug APK, install it on the connected device, and launch Lociant.
# Usage: bash scripts/dev-install.sh
set -euo pipefail

cd "$(dirname "$0")/../apps/android"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
if ! command -v flutter >/dev/null 2>&1; then
  export PATH="${PATH}:${FLUTTER_BIN:-/home/lhx/flutter/bin}"
fi

bash gradlew :app:assembleDebug --console=plain

APK="app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK"
adb shell am start -n io.lociant.android/.MainActivity
