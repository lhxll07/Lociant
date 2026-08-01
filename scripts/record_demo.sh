#!/usr/bin/env bash
# Build, install, and record a fast portrait Lociant demo on an Android emulator.
# The script only creates a local video; publishing remains an explicit step.

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
ANDROID_HOME=${ANDROID_HOME:-/home/lhx/Android/Sdk}
ADB=${ADB:-$ANDROID_HOME/platform-tools/adb}
EMULATOR=${EMULATOR:-$ANDROID_HOME/emulator/emulator}
AVD_NAME=${LOCIANT_AVD:-Pixel_9a}
PACKAGE_NAME=io.lociant.android
VERSION_NAME=$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"/\1/p' "$ROOT_DIR/apps/android/app/build.gradle.kts" | head -n 1)
VERSION_NAME=${VERSION_NAME:-demo}
OUTPUT_PATH=${LOCIANT_DEMO_OUTPUT:-$ROOT_DIR/artifacts/lociant-${VERSION_NAME}-demo.mp4}
REMOTE_VIDEO=/sdcard/lociant-demo.mp4
DURATION_SECONDS=${LOCIANT_DEMO_DURATION:-24}
SPEED=${LOCIANT_DEMO_SPEED:-1.25}
CDP_PORT=${LOCIANT_CDP_PORT:-9222}
SKIP_BUILD=0
EMULATOR_PID=
RECORD_PID=
STARTED_EMULATOR=0

usage() {
  cat <<'EOF'
Usage: scripts/record_demo.sh [options]

Options:
  --skip-build       Reuse the existing debug APK.
  --avd NAME         Android virtual device to use.
  --output PATH      Local MP4 output path.
  --duration SECONDS Recording length before post-processing, default 24.
  --speed FACTOR    Final playback speed, default 1.25.
  -h, --help         Show this help.

Environment:
  ANDROID_HOME       Android SDK directory.
  LOCIANT_AVD        Default AVD name.
  LOCIANT_DEMO_OUTPUT Default output path.
  LOCIANT_DEMO_SPEED Default playback speed.
EOF
}

die() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

cleanup() {
  if [[ -n "$RECORD_PID" ]] && kill -0 "$RECORD_PID" 2>/dev/null; then
    kill "$RECORD_PID" 2>/dev/null || true
  fi
  if [[ "$STARTED_EMULATOR" = 1 ]] && [[ -n "$EMULATOR_PID" ]] && kill -0 "$EMULATOR_PID" 2>/dev/null; then
    kill "$EMULATOR_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

while (($#)); do
  case "$1" in
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --avd)
      (($# >= 2)) || die "--avd requires a value"
      AVD_NAME=$2
      shift 2
      ;;
    --output)
      (($# >= 2)) || die "--output requires a value"
      OUTPUT_PATH=$2
      shift 2
      ;;
    --duration)
      (($# >= 2)) || die "--duration requires a value"
      DURATION_SECONDS=$2
      shift 2
      ;;
    --speed)
      (($# >= 2)) || die "--speed requires a value"
      SPEED=$2
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "unknown option: $1"
      ;;
  esac
done

[[ -x "$ADB" ]] || die "adb not found at $ADB"
[[ -x "$EMULATOR" ]] || die "emulator not found at $EMULATOR"
[[ "$DURATION_SECONDS" =~ ^[1-9][0-9]*$ ]] || die "duration must be a positive integer"
((DURATION_SECONDS >= 16)) || die 'duration must be at least 16 seconds for the full demo'
[[ "$SPEED" =~ ^[0-9]+([.][0-9]+)?$ ]] || die 'speed must be a positive number'
(( $(awk "BEGIN { print ($SPEED >= 0.1) ? 1 : 0 }") )) || die 'speed must be at least 0.1'

APK_PATH=$ROOT_DIR/apps/android/app/build/outputs/apk/debug/app-debug.apk
if [[ "$SKIP_BUILD" = 0 ]]; then
  printf '%s\n' 'Building debug APK...'
  (
    cd "$ROOT_DIR/apps/android"
    bash ./gradlew :app:assembleDebug
  )
fi
[[ -f "$APK_PATH" ]] || die "APK not found at $APK_PATH; build it first or remove --skip-build"

DEVICE=$(
  "$ADB" devices |
    awk 'NR > 1 && $2 == "device" { print $1; exit }'
)
if [[ -z "$DEVICE" ]]; then
  printf 'Starting emulator %s...\n' "$AVD_NAME"
  "$EMULATOR" -avd "$AVD_NAME" -no-snapshot -no-boot-anim -gpu swiftshader_indirect >/tmp/lociant-emulator.log 2>&1 &
  EMULATOR_PID=$!
  STARTED_EMULATOR=1
  "$ADB" wait-for-device
  DEVICE=$(
    "$ADB" devices |
      awk 'NR > 1 && $2 == "device" { print $1; exit }'
  )
fi
[[ -n "$DEVICE" ]] || die 'no usable Android device found'

adb() {
  "$ADB" -s "$DEVICE" "$@"
}

cdp_eval() {
  local expression=$1
  CDP_EXPRESSION=$expression CDP_PORT=$CDP_PORT python3 - <<'PY'
import base64
import json
import os
import socket
import struct
import urllib.request


def read_exact(sock, size):
    data = bytearray()
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise RuntimeError("DevTools socket closed")
        data.extend(chunk)
    return bytes(data)


def read_frame(sock):
    first, second = read_exact(sock, 2)
    length = second & 0x7f
    if length == 126:
        length = struct.unpack(">H", read_exact(sock, 2))[0]
    elif length == 127:
        length = struct.unpack(">Q", read_exact(sock, 8))[0]
    masked = second & 0x80
    mask = read_exact(sock, 4) if masked else b""
    payload = bytearray(read_exact(sock, length))
    if masked:
        for index in range(length):
            payload[index] ^= mask[index % 4]
    return first & 0x0f, bytes(payload)


port = int(os.environ["CDP_PORT"])
targets = json.load(urllib.request.urlopen(f"http://127.0.0.1:{port}/json/list"))
target = next(item for item in targets if item.get("type") == "page")
path = target["webSocketDebuggerUrl"].split(f"127.0.0.1:{port}", 1)[1]
sock = socket.create_connection(("127.0.0.1", port), timeout=5)
key = base64.b64encode(os.urandom(16)).decode("ascii")
request = (
    f"GET {path} HTTP/1.1\r\n"
    f"Host: 127.0.0.1:{port}\r\n"
    "Upgrade: websocket\r\n"
    "Connection: Upgrade\r\n"
    f"Sec-WebSocket-Key: {key}\r\n"
    "Sec-WebSocket-Version: 13\r\n\r\n"
).encode("ascii")
sock.sendall(request)
handshake = bytearray()
while b"\r\n\r\n" not in handshake:
    handshake.extend(sock.recv(4096))

payload = json.dumps({
    "id": 1,
    "method": "Runtime.evaluate",
    "params": {
        "expression": os.environ["CDP_EXPRESSION"],
        "returnByValue": True,
    },
}, separators=(",", ":")).encode("utf-8")
mask = os.urandom(4)
masked_payload = bytes(value ^ mask[index % 4] for index, value in enumerate(payload))
length = len(masked_payload)
header = bytearray([0x81])
if length < 126:
    header.append(0x80 | length)
elif length < 65536:
    header.extend((0x80 | 126).to_bytes(1, "big"))
    header.extend(struct.pack(">H", length))
else:
    header.extend((0x80 | 127).to_bytes(1, "big"))
    header.extend(struct.pack(">Q", length))
sock.sendall(bytes(header) + mask + masked_payload)

while True:
    opcode, response = read_frame(sock)
    if opcode == 0x9:
        sock.sendall(b"\x8a" + bytes([len(response)]) + response)
        continue
    if opcode != 0x1:
        continue
    message = json.loads(response.decode("utf-8"))
    if message.get("id") == 1:
        if "error" in message:
            raise RuntimeError(message["error"])
        break
sock.close()
PY
}

printf 'Waiting for Android boot...\n'
for _ in $(seq 1 90); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]]; then
    break
  fi
  sleep 2
done
[[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]] || die 'Android did not finish booting'

printf 'Installing APK...\n'
adb install -r "$APK_PATH" >/dev/null
adb shell am force-stop "$PACKAGE_NAME"
adb shell monkey -p "$PACKAGE_NAME" 1 >/dev/null
sleep 2

# The emulator may restore its previous sensor state when the app starts.
# Lock the demo in portrait so it matches the phone experience.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
sleep 2

APP_PID=$(adb shell pidof "$PACKAGE_NAME" | tr -d '\r')
[[ -n "$APP_PID" ]] || die 'could not find the Android process for WebView automation'
adb forward --remove "tcp:$CDP_PORT" >/dev/null 2>&1 || true
adb forward "tcp:$CDP_PORT" "localabstract:webview_devtools_remote_$APP_PID" >/dev/null
cdp_eval "document.title" || die 'could not connect to the WebView DevTools endpoint'

mkdir -p "$(dirname "$OUTPUT_PATH")"
rm -f "$OUTPUT_PATH" "$OUTPUT_PATH.raw.mp4"
printf 'Recording %ss at %sx to %s...\n' "$DURATION_SECONDS" "$SPEED" "$OUTPUT_PATH"
adb shell screenrecord --bit-rate 6000000 --time-limit "$DURATION_SECONDS" "$REMOTE_VIDEO" >/dev/null 2>&1 &
RECORD_PID=$!

# Use the app's own event handlers through the debug WebView endpoint. This
# keeps portrait recordings deterministic even though the compact sidebar is
# visually outside its fixed mobile rail.
sleep 1
cdp_eval "navigateTo('home')"
sleep 1
cdp_eval "navigateTo('models')"
sleep 0.8
cdp_eval "document.getElementById('modelLocalButton').click()"
sleep 0.9
cdp_eval "document.getElementById('modelLocalBack').click()"
sleep 0.4
cdp_eval "document.getElementById('modelMarketButton').click()"
sleep 1
cdp_eval "document.getElementById('modelMarketBack').click()"
sleep 0.4
cdp_eval "navigateTo('settings')"
sleep 0.8
cdp_eval "document.getElementById('runtimeSettingsButton').click()"
sleep 0.9
cdp_eval "document.getElementById('runtimeSettingsBack').click()"
sleep 0.35
cdp_eval "document.getElementById('runtimeServerButton').click()"
sleep 0.9
cdp_eval "document.getElementById('runtimeServerBack').click()"
sleep 0.35
cdp_eval "document.getElementById('runtimeCapabilitiesButton').click()"
sleep 0.9
cdp_eval "document.getElementById('runtimeCapabilitiesBack').click()"
sleep 0.35
cdp_eval "document.getElementById('runtimeModelButton').click()"
sleep 0.9
cdp_eval "document.getElementById('runtimeModelBack').click()"
sleep 0.35
cdp_eval "document.getElementById('runtimeAdvancedButton').click()"
sleep 0.9
cdp_eval "document.getElementById('runtimeAdvancedBack').click()"
sleep 0.35
cdp_eval "document.getElementById('aboutButton').click()"
sleep 2
sleep "$((DURATION_SECONDS - 16))"
wait "$RECORD_PID" || true
RECORD_PID=

adb pull "$REMOTE_VIDEO" "$OUTPUT_PATH.raw.mp4" >/dev/null
adb shell rm -f "$REMOTE_VIDEO"
adb forward --remove "tcp:$CDP_PORT" >/dev/null 2>&1 || true

# Keep the raw capture long enough to inspect, then make the release-ready
# clip faster without changing its portrait dimensions.
if [[ "$SPEED" = 1 || "$SPEED" = 1.0 ]]; then
  mv "$OUTPUT_PATH.raw.mp4" "$OUTPUT_PATH"
elif command -v ffmpeg >/dev/null 2>&1; then
  ffmpeg -y -loglevel error -i "$OUTPUT_PATH.raw.mp4" \
    -vf "setpts=PTS/$SPEED" -an -c:v libx264 -preset veryfast -crf 23 \
    -movflags +faststart "$OUTPUT_PATH"
  rm -f "$OUTPUT_PATH.raw.mp4"
else
  die 'ffmpeg is required when --speed is not 1'
fi

printf 'Demo ready: %s\n' "$OUTPUT_PATH"
