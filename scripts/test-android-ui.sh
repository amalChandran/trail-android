#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
TRAIL_DEVICE="${TRAIL_ANDROID_SERIAL:-$(adb devices | awk '$1 ~ /^emulator-/ && $2 == "device" {print $1; exit}')}"
if [[ -z "$TRAIL_DEVICE" ]]; then echo "Run scripts/run-android.sh to start an emulator first."; exit 1; fi
cd "$TRAIL_ROOT/android"
./gradlew :playground:assembleDebug :playground:assembleDebugAndroidTest --console=plain
adb -s "$TRAIL_DEVICE" install -r playground/build/outputs/apk/debug/playground-debug.apk
adb -s "$TRAIL_DEVICE" install -r playground/build/outputs/apk/androidTest/debug/playground-debug-androidTest.apk
TRAIL_TEST_LOG="$(mktemp -t trail-ui.XXXXXX)"
trap 'rm -f "$TRAIL_TEST_LOG"' EXIT
adb -s "$TRAIL_DEVICE" shell am instrument -w dev.trail.playground.test/androidx.test.runner.AndroidJUnitRunner | tee "$TRAIL_TEST_LOG"
python3 - "$TRAIL_TEST_LOG" <<'PY'
import pathlib, re, sys
output = pathlib.Path(sys.argv[1]).read_text()
if not re.search(r"OK \(\d+ tests?\)", output):
    raise SystemExit("Android instrumentation did not report a passing test run.")
PY
