#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$TRAIL_ROOT/android"
./gradlew :playground:assembleDebug --console=plain
TRAIL_DEVICE="${TRAIL_ANDROID_SERIAL:-$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')}"
if [[ -z "$TRAIL_DEVICE" ]]; then
  TRAIL_AVD="${TRAIL_ANDROID_AVD:-$(emulator -list-avds | head -1)}"
  if [[ -z "$TRAIL_AVD" ]]; then echo "Create an Android virtual device in Android Studio, then rerun this script."; exit 1; fi
  nohup emulator -avd "$TRAIL_AVD" > /tmp/trail-emulator.log 2>&1 &
  adb -e wait-for-device
  TRAIL_DEVICE="$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')"
fi
for attempt in {1..120}; do
  [[ "$(adb -s "$TRAIL_DEVICE" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] && break
  sleep 1
done
adb -s "$TRAIL_DEVICE" install -r playground/build/outputs/apk/debug/playground-debug.apk
adb -s "$TRAIL_DEVICE" shell am start -n dev.trail.playground/.MainActivity
