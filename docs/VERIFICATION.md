# Local verification — 19 September 2026

Google Maps Android and Apple MapKit are the first map adapters. The typed DSL / Swift result-builder API, provider-neutral binding and flight/cab/ferry playground are implemented locally on `trail-2-native`. No release or remote push has been made.

## Executed results

Counts below are **executed cases**, including named parameterized cases; they are not hundreds of duplicated test functions. Both implementations consume the same 544 fixture IDs. The case dimensions exercise different geometry, malformed inputs, timing, camera transforms and raster output.

| Suite | Passed cases | Evidence |
| --- | ---: | --- |
| Kotlin core | 567 | JUnit XML: 544 shared cases + 23 geometry/DSL/budget regressions |
| Kotlin optional effects | 99 | 96 style/motion combinations + 3 catalog tests |
| Kotlin external plugin consumer | 1 | Separately compiled consumer, public contracts only |
| Android instrumentation | 153 | 144 native Canvas pixel cases, seam clipping, 4 binding/lifecycle/API tests and 4 app/consumer flows |
| Live Google SDK contracts | 2 | Loaded real maps; 12 camera configurations, density conversion, preserved playback, date-line contours, and full-geometry camera fitting for flight/cab/ferry |
| Swift package | 805 | 30 Swift Testing functions, expanded into named cases; includes 544 shared cases, 96 catalog combinations, 144 native Core Graphics raster cases and 21 other regressions |
| Native MapKit | 33 | 4 Swift Testing functions; 30 real camera conversions + readiness/lifetime, attachment and date-line tests |
| iOS app UI | 3 | Geographic journeys, compiled integration examples, gallery playback/plugins |
| Android build and lintDebug | Passed | Debug application and instrumentation APK compile; lint passes |
| Example/fixture synchronization | Passed | Generated documentation and both apps match their compiled source and canonical JSON |
| Script syntax and Git whitespace | Passed | Bash parsing, Python execution and `git diff --check` |

**Totals: Android 822 passed, 0 skipped; Swift/iOS 841 passed — 1,663 passing cases.** Swift's package runner reports 30 test functions; the report script counts each function's actually executed parameter cases from its passing output. Android instrumentation reports `OK (155 tests)`: 153 non-Google cases plus two live Google SDK tests. The 12 camera combinations and three journey fits are assertions within those two tests, not additional counted cases. The report parser removes the previous JSON report before validating a new log, so failed validation cannot leave a stale green summary.

Android used the Pixel 9 Pro XL AVD on `emulator-5554` (API 36). No APK was installed on the connected physical phone. Apple used Xcode 26.2 / Swift 6.2.3, the iPhone 17 Pro simulator with iOS 26.2, and macOS for package/raster tests. Android uses Java 17 and the source-pinned Gradle, AGP, Kotlin and Compose versions.

## What the tests prove

| Area | Coverage |
| --- | --- |
| Shared geometry | 182 samples and 112 slices: empty/singleton/repeated points, unequal segment lengths, endpoints, disconnected contours, no gap interpolation; independent slow linear reference |
| Geographic construction | 51 route cases, 12 wrapped bounds cases and 51 polyline cases: direct/arc/great-circle endpoints, poles, antipodes, ±180° crossings, precision 5/6, seeded valid data, truncated/invalid/overflowing encodings |
| Limits and distance | Known spherical distance anchors, invalid bend/sampling/identity/revision values, maximum 100,000-point input and rejected over-budget data, defensive/value copies |
| Playback | 64 shared command sequences plus explicit sequence/layer/preset regressions: pause, seek, replay, repeat/terminal boundaries, independent players, preserved intent after effect replacement |
| Provider boundary | 72 shared projection cases: rotations, densities, missing middle point, empty routes and seams. These are boundary tests, not a substitute for native SDK tests |
| Native pixels | Each renderer: 3 widths × 3 colors × 2 opacities × 2 motions × 4 seek positions = 144 cases. Independent pixel assertions verify interiors, clipping, width, alpha, color and empty windows. Separate seam tests reject a stroke across disconnected contours |
| Android binding | Missing projection, reduced motion, background/return and disposal freeze/resume the clock; camera/route updates preserve external pause/seek state; projected drawing is clipped to the map viewport; controller-only integration preserves and updates its effect while paused |
| Google SDK | Live basemap readiness; zoom 14/16 × bearing 0°/45°/180° × tilt 0°/40°; screen-pixel to dp alignment within 1 dp, retained external pause/seek state, and short separated date-line contours. All coordinates in each flight/cab/ferry route fit inside 44 dp insets, with at least one dimension filling the available viewport |
| Apple SDK | 5 geographic centers × 3 headings × 2 spans = 30 real MKMapView conversion/round-trip checks. Also verifies nil for detached/zero-size/released maps, weak capture, retained app delegate/callbacks, explicit route reset policy, idempotent detach, and short separated date-line contours |
| User flows | Flight/cab/ferry switching; 119-point road route vs 2-point/direct and 129-point curves; playback, seeking, styles, motions, reduced motion; Apple map pan/zoom; compiled API, plugin and runtime-color examples |

The Google contracts ran with a key configured only in ignored `android/local.properties`. Tests wait for native camera callbacks as well as Compose recomposition; advancing Compose's virtual clock alone does not settle the map. Without a configured key, both Google tests still report explicit assumption skips. See [MAPS.md](MAPS.md) to reproduce with your own credentials.

Live screenshot review exposed clipped flight endpoints: Maps Compose defaults to minimum zoom 3. The journey map now permits zoom 0 so fitting a transatlantic route can zoom out sufficiently. The new camera-fit regression uses the actual sample map composable, checks every waypoint and rejects an unfitted world overview. Android build, lint and all 155 instrumentation cases passed after the fix.

The pixel suite exposed an implicit Apple color-space dependency; ARGB now explicitly means sRGB. Final API review also found that a supplied Android controller could be overwritten by a default effect; omitted effect arguments now use the controller, with a pixel regression for color replacement while paused. Screenshot review confirmed the flight connection across the Atlantic, the Manhattan route following streets, and the illustrative ferry line remaining in the harbor. A stalled Android emulator required a cold boot. Xcode initially reused stale package modules; a fresh derived-data directory fixed that build. UI test failures from a sheet-dismiss gesture and partially/offscreen switches were corrected and rerun. These failures were not removed or counted as passes.

## Reproduce and inspect

```sh
./scripts/check.sh                 # contracts, sync, Kotlin, Android lint/build, Swift
./scripts/test-android-ui.sh       # selected emulator; Google test skips without a key
./scripts/test-ios.sh              # MapKit and all iOS UI tests
```

Set `TRAIL_ANDROID_SERIAL` or `TRAIL_IOS_SIMULATOR` to select a test device. Use `TRAIL_IOS_TEST_ONLY=TrailMapKitTests ./scripts/test-ios.sh` for a targeted native-map run. Each test script propagates failures.

Local artifacts are ignored by Git:

- `android/*/build/test-results/test/TEST-*.xml`: individual JUnit results.
- `android/trail-core/build/reports/tests/test` and `build/reports/jacoco`: core reports.
- `android/playground/build/reports/lint-results-debug.html`: lint.
- `artifacts/android-instrumentation.log` and `.json`: individual native test statuses and actual pass/skip counts.
- `artifacts/swift-tests.log` and `.json`: Swift function and expanded-case counts; no nonexistent Swift xUnit file is advertised.
- `artifacts/ios-tests.log` and `.json`; `ios/build/TrailTests-*.xcresult`: native MapKit/UI results.
- `artifacts/ios-flight.png`, `ios-cab.png`, `ios-ferry.png`: reviewed journey screenshots.
- `artifacts/android-flight.png`, `android-cab.png`, `android-ferry.png`: live Google Maps journey screenshots.
- `android/playground/build/outputs/apk/debug/playground-debug.apk`: runnable Android sample.
- `swift/.build/arm64-apple-macosx/debug/codecov`: Swift package coverage data.

The provided GitHub workflow includes fixture checks, core/lint builds, emulator/simulator runs and artifact uploads. It has not run remotely. CI also skips the Google live-map contract when credentials are absent.

## Remaining release evidence

This is a tested native alpha, not proof of universal map compatibility or a stable release. Remaining work includes minimum-supported-OS and real-device coverage, more extreme pitch/globe/world-copy and inset/resize stress, larger teardown/leak stress, image goldens for complex dashes/gradients/joins, external published-artifact consumers, ABI checks and provenance review. The ferry is a hand-authored illustration; see [data provenance](../samples/README.md).

No minimum-APK-size, zero-allocation, 60/120-fps, battery or production-readiness claim is made. Release-host size deltas and physical-device profiles are still required. Swift extracts partial path points per stroke; Android dash-phase changes allocate path effects; gradient/comet combinations can issue many strokes. The [relaunch plan](DESIGN.md) keeps those measurements separate from correctness tests. Xcode also emits a package dependency-scan warning despite the explicit Core→UI dependency in the manifest; native linking and all executed tests succeed.
