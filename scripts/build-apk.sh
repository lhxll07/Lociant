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

# The Android/Flutter build runs on JDK 25. Android modules still emit JVM 17
# bytecode for device compatibility; that target is configured in Gradle.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in /usr/lib/jvm/java-25-openjdk /usr/lib/jvm/java-25-openjdk-amd64 \
    /usr/lib/jvm/jdk-25-openjdk /usr/lib/jvm/jdk-25 /usr/lib/jvm/default; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_BIN=$(command -v java 2>/dev/null || true)
  if [[ -n "$JAVA_BIN" ]]; then
    JAVA_BIN=$(readlink -f "$JAVA_BIN" 2>/dev/null || printf '%s' "$JAVA_BIN")
    JAVA_CANDIDATE=$(cd "$(dirname "$JAVA_BIN")/.." && pwd -P)
    if [[ -x "$JAVA_CANDIDATE/bin/java" ]]; then
      export JAVA_HOME="$JAVA_CANDIDATE"
    fi
  fi
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "JDK 25 is required; set JAVA_HOME to a JDK 25 installation" >&2
  exit 1
fi
JAVA_MAJOR=$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
if [[ "$JAVA_MAJOR" != "25" ]]; then
  echo "JDK 25 is required; found Java ${JAVA_MAJOR:-unknown} at $JAVA_HOME" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"

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

# Gradle 9.1's Flutter included build needs org.gradle.kotlin.kotlin-dsl:6.2.0.
# If the selected GRADLE_USER_HOME does not have it yet, copy it from the
# user's existing Gradle cache (if present).
KDS_VERSION="6.2.0"
KDS_MARKER="$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION"
KDS_SOURCE="$HOME/.gradle/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION"
if [[ ! -d "$KDS_MARKER" && -d "$KDS_SOURCE" ]]; then
  echo "Seeding Gradle cache with gradle-kotlin-dsl-plugins $KDS_VERSION"
  mkdir -p \
    "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION" \
    "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION" \
    "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION" \
    "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION"
  cp -a "$HOME/.gradle/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION/." \
    "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION/"
  cp -a "$KDS_SOURCE/." "$KDS_MARKER/"
  cp -a "$HOME/.gradle/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION/." \
    "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin.kotlin-dsl/org.gradle.kotlin.kotlin-dsl.gradle.plugin/$KDS_VERSION/"
  cp -a "$HOME/.gradle/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION/." \
    "$GRADLE_USER_HOME/caches/modules-2/metadata-2.107/descriptors/org.gradle.kotlin/gradle-kotlin-dsl-plugins/$KDS_VERSION/"
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
