#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
cd "$TRAIL_ROOT/ios"
TRAIL_SIM="${TRAIL_IOS_SIMULATOR:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; devices=[d for group in json.load(sys.stdin)["devices"].values() for d in group if "iPhone" in d["name"]]; devices.sort(key=lambda d: d["state"] != "Booted"); print(devices[0]["udid"] if devices else "")')}"
if [[ -z "$TRAIL_SIM" ]]; then echo "Install an iOS simulator runtime in Xcode Settings > Components."; exit 1; fi
if ! xcrun simctl list devices booted | grep -q "$TRAIL_SIM"; then xcrun simctl boot "$TRAIL_SIM"; fi
open -a Simulator
xcrun simctl bootstatus "$TRAIL_SIM" -b
xcodebuild -project TrailPlayground.xcodeproj -scheme TrailPlayground -destination "platform=iOS Simulator,id=$TRAIL_SIM" -derivedDataPath build CODE_SIGNING_ALLOWED=NO build -quiet
xcrun simctl install "$TRAIL_SIM" build/Build/Products/Debug-iphonesimulator/TrailPlayground.app
xcrun simctl launch "$TRAIL_SIM" dev.trail.playground
