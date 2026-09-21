#!/usr/bin/env bash
# Sample the native Android renderer deterministically, then encode its real PNG frames.
set -euo pipefail
source "$(dirname "$0")/common.sh"
TRAIL_DEVICE="${TRAIL_ANDROID_SERIAL:-emulator-5554}"
TRAIL_FFMPEG="${TRAIL_FFMPEG:-$(command -v ffmpeg || true)}"
if [[ -z "$TRAIL_FFMPEG" && -x /opt/homebrew/bin/ffmpeg ]]; then TRAIL_FFMPEG=/opt/homebrew/bin/ffmpeg; fi
if [[ -z "$TRAIL_FFMPEG" ]]; then echo "Install ffmpeg to encode README previews."; exit 1; fi
(cd "$TRAIL_ROOT/android" && ./gradlew :playground:assembleDebug :playground:assembleDebugAndroidTest --console=plain)
adb -s "$TRAIL_DEVICE" install -r "$TRAIL_ROOT/android/playground/build/outputs/apk/debug/playground-debug.apk"
adb -s "$TRAIL_DEVICE" install -r "$TRAIL_ROOT/android/playground/build/outputs/apk/androidTest/debug/playground-debug-androidTest.apk"
mkdir -p "$TRAIL_ROOT/artifacts/route-previews"
adb -s "$TRAIL_DEVICE" shell am instrument -w -r \
  -e class dev.trail.playground.RouteAnimationRenderingTest \
  -e captureRoutePreviews true \
  dev.trail.playground.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$TRAIL_ROOT/artifacts/route-previews/instrumentation.log"
python3 "$TRAIL_ROOT/scripts/test-summary.py" android "$TRAIL_ROOT/artifacts/route-previews/instrumentation.log"
adb -s "$TRAIL_DEVICE" pull /sdcard/Android/data/dev.trail.playground/files/route-previews/. "$TRAIL_ROOT/artifacts/route-previews/"
TRAIL_PRESETS=(MovingDots MovingDashes Loading Comet RouteSweep DrawAndErase)
TRAIL_NAMES=(moving-dots moving-dashes loading comet route-sweep draw-and-erase)
for i in "${!TRAIL_PRESETS[@]}"; do
  "$TRAIL_FFMPEG" -v error -y -framerate 24 -i "$TRAIL_ROOT/artifacts/route-previews/${TRAIL_PRESETS[$i]}/%03d.png" \
    -filter_complex '[0:v]split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a' \
    -loop 0 "$TRAIL_ROOT/docs/media/android-route-${TRAIL_NAMES[$i]}.gif"
done
TRAIL_INPUTS=()
for preset in "${TRAIL_PRESETS[@]}"; do TRAIL_INPUTS+=(-framerate 24 -i "$TRAIL_ROOT/artifacts/route-previews/$preset/%03d.png"); done
"$TRAIL_FFMPEG" -v error -y "${TRAIL_INPUTS[@]}" \
  -filter_complex '[0:v][1:v][2:v][3:v][4:v][5:v]xstack=inputs=6:layout=0_0|420_0|0_236|420_236|0_472|420_472,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a' \
  -loop 0 "$TRAIL_ROOT/docs/media/android-route-options.gif"
python3 "$TRAIL_ROOT/scripts/release/verify-media.py"
