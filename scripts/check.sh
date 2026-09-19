#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
(cd "$TRAIL_ROOT/android" && ./gradlew :trail-core:test :trail-effects:test :sample-plugin:test :playground:assembleDebug :playground:lintDebug --console=plain)
(cd "$TRAIL_ROOT/swift" && swift test --enable-code-coverage)
