# Local verification — 19 September 2026

The approved typed DSL / Swift result-builder API is implemented and tested locally. This is a runnable alpha, not a completed production relaunch. The [decision record](API_CHOICES.md) and [API guide](API_GUIDE.md) describe the canonical syntax.

| Check | Result |
| --- | --- |
| Kotlin core tests | 19 passed, including sequence boundaries, invalid composition, layers, independent players and replacement intent |
| Kotlin optional effects tests | 3 passed, including all 96 style/motion combinations at multiple seek positions |
| Separate Kotlin plugin consumer | 1 passed; public contracts only |
| Android debug app + test APK | Compiled successfully |
| Android lintDebug | Passed |
| Android emulator UI flows | 3 passed in 24.349 s: gallery controls, compiled examples with pixel checks for runtime colors/plugins, native View replacement semantics |
| Swift package tests | 17 test functions passed; one expands to 96 preset cases |
| iOS simulator app + UI-test bundle | Compiled successfully |
| iOS simulator UI flows | Gallery flow passed in 30.395 s; examples flow passed in 45.770 s on targeted rerun after the accessibility fix |
| Compiled example synchronization | 8 Android and 9 iOS runnable examples; generated docs and app code panels match their source files |
| Gradle wrapper, script syntax and git whitespace | Passed |

Android verification used the existing Pixel 9 Pro XL AVD on `emulator-5554`. No APK was installed onto the attached physical phone. iOS verification used the iPhone 17 Pro simulator with the iOS 26.2 runtime and Xcode 26.2 / Swift 6.2.3. Swift tests ran on macOS. Android builds used Java 17 and the pinned Gradle/AGP/Kotlin versions in source.

Regression tests verify deterministic out-of-order seeking through a two-step sequence, exact sequence transitions, whole-sequence repetition, duplicate/mixed-layer diagnostics, independent layer durations, shared presets with separate players, and loop-to-one-shot replacement. Both languages use the same reference expectations. Floating-point interior values use a tolerance where needed; exact endpoint checks remain exact.

The Android flow checks rendered blue/coral pixels after ordinary style and external-plugin updates. The iOS flow opens all three map examples (convenience MapKit, existing SwiftUI Map, existing MKMapView), checks source panels, and exercises the playback controls. Its first run caught a preview-container accessibility identifier overriding the child slider's identifier; removing that container identifier made the targeted rerun pass. Visual review caught and corrected Android text contrast in the examples screen.

The map API adapter code compiles on both platforms. The iOS UI flow proves the three integration paths can open, but does not prove geographic alignment under every camera transform or repeated detach/leak behavior. Android Google Maps runtime behavior has **not** been tested because no new Maps API key was supplied. Neither platform has yet had full map camera/bearing/tilt golden tests, antimeridian support, release ABI validation, incremental application-size measurement, or real-device frame-time/power profiling. No comparative human/LLM API-usability evaluation has been run.

No performance, zero-allocation, minimum-APK-size, or production-readiness claim is made. In particular, complex comet/gradient combinations issue many strokes, Core Graphics currently extracts visible path points per stroke, and Android dash-phase changes can allocate path effects. These are explicit profiling targets.

Local reports/artifacts (ignored by Git):

- Android: `android/trail-core/build/reports/tests/test`, `android/trail-core/build/reports/jacoco`, `android/playground/build/reports/lint-results-debug.html`.
- Swift: `swift/.build/arm64-apple-macosx/debug/codecov`.
- iOS UI runs: `ios/build/TrailDSLTests.xcresult` (gallery passed; initial examples failure) and `ios/build/TrailDSLTests-fixed.xcresult` (examples passed).
- Simulator screenshots: `artifacts/android-dsl-examples.png`, `artifacts/ios-dsl-examples/`.
- Android APK: `android/playground/build/outputs/apk/debug/playground-debug.apk`.

Reproduce core/lint checks with `./scripts/check.sh`; Android UI flows with `./scripts/test-android-ui.sh` after starting an emulator; iOS flows with Product > Test in Xcode. UI tests currently run locally. The Android UI script explicitly selects one emulator by default and validates the instrumentation success marker.

The GitHub workflow is provided but has not run remotely; this branch has not been pushed or published.
