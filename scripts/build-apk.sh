#!/usr/bin/env bash
# Build the Lociant debug APK without needing root or sudo.
#
# Usage:
#   bash scripts/build-apk.sh
#
# The script prefers writable cache directories. Set any of the variables
# below to override the defaults:
#   JAVA_HOME, GRADLE_USER_HOME, CARGO_HOME, PUB_CACHE, ANDROID_HOME
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/apps/android"

# The current Android/Flutter toolchain supports JDK 17 or 21. Use JAVA_HOME
# if already set, otherwise prefer an installed compatible JDK.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-21-openjdk; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "JDK 17 or 21 is required; set JAVA_HOME to a compatible JDK" >&2
  exit 1
fi
JAVA_MAJOR=$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
if [[ "$JAVA_MAJOR" != "17" && "$JAVA_MAJOR" != "21" ]]; then
  echo "JDK 17 or 21 is required; found Java ${JAVA_MAJOR:-unknown} at $JAVA_HOME" >&2
  exit 1
fi

# Keep the generated Flutter Gradle configuration tied to the SDK that is
# actually being used. Do not copy a second SDK into /tmp: that leaves stale
# paths behind and makes the Android build depend on an ephemeral directory.
FLUTTER_PROPS="$ROOT/apps/flutter/.android/local.properties"
FLUTTER_SDK_LINE=$(grep '^flutter.sdk=' "$FLUTTER_PROPS" 2>/dev/null || true)
FLUTTER_SDK=${FLUTTER_SDK_LINE#flutter.sdk=}
DETECTED_FLUTTER_SDK=""
if [[ -n "${FLUTTER_ROOT:-}" && -x "$FLUTTER_ROOT/bin/flutter" ]]; then
  DETECTED_FLUTTER_SDK="$(cd "$FLUTTER_ROOT" && pwd -P)"
else
  FLUTTER_BIN="$(command -v flutter 2>/dev/null || true)"
  if [[ -n "$FLUTTER_BIN" && -x "$FLUTTER_BIN" ]]; then
    DETECTED_FLUTTER_SDK="$(cd "$(dirname "$FLUTTER_BIN")/.." && pwd -P)"
  elif [[ -x "$HOME/flutter/bin/flutter" ]]; then
    DETECTED_FLUTTER_SDK="$(cd "$HOME/flutter" && pwd -P)"
  fi
fi

if [[ -n "$DETECTED_FLUTTER_SDK" ]]; then
  if [[ "$FLUTTER_SDK" != "$DETECTED_FLUTTER_SDK" ]]; then
    echo "Using Flutter SDK at $DETECTED_FLUTTER_SDK"
    if [[ ! -f "$FLUTTER_PROPS" ]]; then
      echo "missing Flutter local.properties: $FLUTTER_PROPS; run flutter pub get first" >&2
      exit 1
    fi
    if grep -q '^flutter.sdk=' "$FLUTTER_PROPS"; then
      sed -i "s#^flutter.sdk=.*#flutter.sdk=$DETECTED_FLUTTER_SDK#" "$FLUTTER_PROPS"
    else
      printf '\nflutter.sdk=%s\n' "$DETECTED_FLUTTER_SDK" >> "$FLUTTER_PROPS"
    fi
    FLUTTER_SDK="$DETECTED_FLUTTER_SDK"
  fi
fi

if [[ -z "$FLUTTER_SDK" || ! -x "$FLUTTER_SDK/bin/flutter" ]]; then
  echo "Flutter SDK not found; set FLUTTER_ROOT or run flutter pub get" >&2
  exit 1
fi

# Prefer user-owned cache directories; fall back to /tmp when HOME is read-only.
if [[ -z "${GRADLE_USER_HOME:-}" ]]; then
  if [[ -w "${HOME:-/tmp}" ]]; then
    export GRADLE_USER_HOME="${HOME}/.gradle"
  else
    export GRADLE_USER_HOME="/tmp/lociant-build/gradle"
  fi
fi
if [[ -z "${CARGO_HOME:-}" ]]; then
  if [[ -w "${HOME:-/tmp}" ]]; then
    export CARGO_HOME="${HOME}/.cargo"
  else
    export CARGO_HOME="/tmp/lociant-build/cargo"
  fi
fi
if [[ -z "${PUB_CACHE:-}" ]]; then
  if [[ -w "${HOME:-/tmp}" ]]; then
    export PUB_CACHE="${HOME}/.pub-cache"
  else
    export PUB_CACHE="/tmp/lociant-build/pub"
  fi
fi
if [[ -z "${ANDROID_HOME:-}" ]]; then
  export ANDROID_HOME="${HOME}/Android/Sdk"
fi

# Some read-only filesystems report as writable by permission bits but still
# reject writes. Gradle/Cargo need writable caches; PUB_CACHE only needs to be
# readable for the already-resolved Flutter plugins.
for cache_dir in "$GRADLE_USER_HOME" "$CARGO_HOME"; do
  if ! mkdir -p "$cache_dir" 2>/dev/null || [[ ! -w "$cache_dir" ]]; then
    base="/tmp/lociant-build/$(basename "$cache_dir")"
    echo "$cache_dir is not writable; falling back to $base"
    mkdir -p "$base"
    if [[ "$cache_dir" == "$GRADLE_USER_HOME" ]]; then
      export GRADLE_USER_HOME="$base"
    else
      export CARGO_HOME="$base"
    fi
  fi
done

mkdir -p "$GRADLE_USER_HOME" "$CARGO_HOME" "$PUB_CACHE"

# Gradle's Flutter included build needs org.gradle.kotlin.kotlin-dsl:5.2.0.
# If the selected GRADLE_USER_HOME does not have it yet, copy it from the
# user's existing Gradle cache (if present).
KDS_MARKER="$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0"
if [[ ! -d "$KDS_MARKER" && -d "$HOME/.gradle/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0" ]]; then
  echo "Seeding Gradle cache with gradle-kotlin-dsl-plugins 5.2.0"
  mkdir -p     "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0"     "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0"     "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0"     "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0"
  cp -a "$HOME/.gradle/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0/." "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0/"
  cp -a "$HOME/.gradle/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0/." "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0/"
  cp -a "$HOME/.gradle/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0/." "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/5.2.0/"
  cp -a "$HOME/.gradle/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0/." "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/5.2.0/"
fi

echo "JAVA_HOME=$JAVA_HOME"
echo "GRADLE_USER_HOME=$GRADLE_USER_HOME"
echo "CARGO_HOME=$CARGO_HOME"
echo "PUB_CACHE=$PUB_CACHE"
echo "ANDROID_HOME=$ANDROID_HOME"

# Use `bash gradlew` so the wrapper does not need execute permission.
bash gradlew :app:assembleDebug --console=plain

APK="$ROOT/apps/android/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK" ]]; then
  echo
  echo "APK built: $APK"
  echo "Install with: adb install -r \"$APK\""
else
  echo
  echo "APK not found at $APK" >&2
  exit 1
fi
