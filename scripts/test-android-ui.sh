#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
TRAIL_DEVICE="${TRAIL_ANDROID_SERIAL:-$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')}"
if [[ -z "$TRAIL_DEVICE" ]]; then echo "Run scripts/run-android.sh to start an emulator first."; exit 1; fi
cd "$TRAIL_ROOT/android"
./gradlew :playground:assembleDebug :playground:assembleDebugAndroidTest --console=plain
adb -s "$TRAIL_DEVICE" install -r playground/build/outputs/apk/debug/playground-debug.apk
adb -s "$TRAIL_DEVICE" install -r playground/build/outputs/apk/androidTest/debug/playground-debug-androidTest.apk
mkdir -p "$TRAIL_ROOT/artifacts"
TRAIL_TEST_LOG="$TRAIL_ROOT/artifacts/android-instrumentation.log"
adb -s "$TRAIL_DEVICE" shell am instrument -w -r dev.trail.playground.test/androidx.test.runner.AndroidJUnitRunner | tee "$TRAIL_TEST_LOG"
python3 "$TRAIL_ROOT/scripts/test-summary.py" android "$TRAIL_TEST_LOG"
