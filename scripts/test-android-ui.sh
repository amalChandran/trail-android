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
python3 - "$TRAIL_DEVICE" <<'PY' | tee "$TRAIL_TEST_LOG"
import subprocess
import sys

device = sys.argv[1]
try:
    result = subprocess.run([
        "adb", "-s", device, "shell", "am", "instrument", "-w", "-r",
        "-e", "timeout_msec", "120000",
        "dev.trail.playground.test/androidx.test.runner.AndroidJUnitRunner",
    ], timeout=720)
    sys.exit(result.returncode)
except subprocess.TimeoutExpired:
    print("Instrumentation exceeded 12 minutes; stopping the example app.", flush=True)
    subprocess.run(["adb", "-s", device, "shell", "am", "force-stop", "dev.trail.playground"], timeout=15)
    sys.exit(124)
PY
python3 "$TRAIL_ROOT/scripts/test-summary.py" android "$TRAIL_TEST_LOG"
