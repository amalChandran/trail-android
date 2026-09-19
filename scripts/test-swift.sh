#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
mkdir -p "$TRAIL_ROOT/artifacts"
TRAIL_SWIFT_LOG="$TRAIL_ROOT/artifacts/swift-tests.log"
(cd "$TRAIL_ROOT/swift" && swift test --enable-code-coverage) 2>&1 | tee "$TRAIL_SWIFT_LOG"
python3 "$TRAIL_ROOT/scripts/test-summary.py" swift "$TRAIL_SWIFT_LOG"
