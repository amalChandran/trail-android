#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
TRAIL_SIM="${TRAIL_IOS_SIMULATOR:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; devices=[d for group in json.load(sys.stdin)["devices"].values() for d in group if "iPhone" in d["name"]]; devices.sort(key=lambda d: d["state"] != "Booted"); print(devices[0]["udid"] if devices else "")')}"
if [[ -z "$TRAIL_SIM" ]]; then echo "Install an iOS simulator runtime in Xcode Settings > Components."; exit 1; fi
mkdir -p "$TRAIL_ROOT/artifacts" "$TRAIL_ROOT/ios/build"
TRAIL_RESULT="$TRAIL_ROOT/ios/build/TrailTests-$(date +%Y%m%d-%H%M%S).xcresult"
set --
if [[ -n "${TRAIL_IOS_TEST_ONLY:-}" ]]; then set -- "-only-testing:$TRAIL_IOS_TEST_ONLY"; fi
xcodebuild -project "$TRAIL_ROOT/ios/TrailPlayground.xcodeproj" -scheme TrailPlayground \
    -destination "platform=iOS Simulator,id=$TRAIL_SIM" -parallel-testing-enabled NO \
    -derivedDataPath "$TRAIL_ROOT/ios/build/native" -resultBundlePath "$TRAIL_RESULT" \
    "$@" test 2>&1 | tee "$TRAIL_ROOT/artifacts/ios-tests.log"
python3 "$TRAIL_ROOT/scripts/test-summary.py" ios "$TRAIL_ROOT/artifacts/ios-tests.log"
echo "Native test report: $TRAIL_RESULT"
