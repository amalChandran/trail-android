#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
python3 "$TRAIL_ROOT/scripts/sync-examples.py" --check
python3 "$TRAIL_ROOT/scripts/generate-contract-fixtures.py" --check
python3 "$TRAIL_ROOT/scripts/generate-vehicle-artwork.py" --check
python3 "$TRAIL_ROOT/scripts/sync-fixtures.py" --check
(cd "$TRAIL_ROOT/android" && ./gradlew :trail-core:test :trail-effects:test :sample-plugin:test :playground:assembleDebug :playground:lintDebug --console=plain)
"$TRAIL_ROOT/scripts/test-swift.sh"
