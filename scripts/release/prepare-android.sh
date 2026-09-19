#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/../common.sh"
if [[ $# -ne 0 ]]; then echo "Usage: $0 (local sample APK and SDK staging only)" >&2; exit 2; fi
cd "$TRAIL_ROOT/android"
./gradlew :playground:assembleRelease :playground:lintRelease \
  :trail-core:publishAllPublicationsToStagingRepository \
  :trail-effects:publishAllPublicationsToStagingRepository \
  :trail-android:publishAllPublicationsToStagingRepository \
  :trail-compose:publishAllPublicationsToStagingRepository \
  :trail-google-maps:publishAllPublicationsToStagingRepository --console=plain
mkdir -p "$TRAIL_ROOT/artifacts/release"
cp playground/build/outputs/apk/release/playground-release.apk "$TRAIL_ROOT/artifacts/release/trail-studio-example.apk"
python3 "$TRAIL_ROOT/scripts/release/audit-artifacts.py"
echo "Prepared installable example APK and local Maven SDK artifacts. Nothing published."
