# Local verification — 19 September 2026

This is the tested provisional DSL prototype, not a completed production relaunch. The final public syntax remains a user decision in [API_CHOICES.md](API_CHOICES.md).

| Check | Result |
| --- | --- |
| Kotlin core tests | 12 passed |
| Kotlin optional effects tests | 3 passed, including all 96 style/motion combinations at multiple seek positions |
| Separate Kotlin plugin consumer | 1 passed; public contracts only |
| Android debug app + test APK | Compiled successfully |
| Android lintDebug | Passed |
| Android emulator UI flow | 1 passed in 12.367 s: pause, seek, replay, change styles/motion, reduced motion, custom plugin |
| Swift package tests | 10 test functions passed; one expands to 96 preset cases |
| iOS simulator app + UI-test bundle | Compiled successfully |
| iOS simulator UI flow | 1 passed in 26.121 s: pause, scrub, replay, styles, motion, custom plugin, reduced motion, MapKit switch |
| Script syntax and git whitespace | Passed |

Android verification used the existing Pixel 9 Pro XL AVD on `emulator-5554`. No APK was installed onto the attached physical phone. iOS verification used the iPhone 17 Pro simulator with the iOS 26.2 runtime and Xcode 26.2 / Swift 6.2.3. Swift tests ran on macOS. Android builds used Java 17 and the pinned Gradle/AGP/Kotlin versions in source.

The UI tests initially exposed test-clock synchronization issues. The Android test now advances the manual frame clock for playback assertions and enables automatic advancement for scrolling after pausing playback. The iOS test pauses before opening preset menus; playback controls were separated so frame updates do not redraw the whole gallery. A screenshot review also caught and corrected Android text contrast outside a Material surface.

The map API adapter code compiles on both platforms. The iOS UI test switches to MapKit, but does not prove geographic alignment under every camera transform. Android Google Maps runtime behavior has **not** been tested because no new Maps API key was supplied. Neither platform has yet had full map camera/bearing/tilt golden tests, antimeridian support, release ABI validation, incremental application-size measurement, or real-device frame-time/power profiling.

No performance, zero-allocation, minimum-APK-size, or production-readiness claim is made. In particular, complex comet/gradient combinations issue many strokes, Core Graphics currently extracts visible path points per stroke, and Android dash-phase changes can allocate path effects. These are explicit profiling targets.

Local reports/artifacts (ignored by Git):

- Android: `android/trail-core/build/reports/tests/test`, `android/trail-core/build/reports/jacoco`, `android/playground/build/reports/lint-results-debug.html`.
- Swift: `swift/.build/arm64-apple-macosx/debug/codecov`.
- iOS UI run: `ios/build/TrailUITests-rerun.xcresult`.
- Simulator screenshots: `artifacts/android-flow.png`, `artifacts/ios-flow.png`.
- Android APK: `android/playground/build/outputs/apk/debug/playground-debug.apk`.

The GitHub workflow is provided but has not run remotely; this branch has not been pushed or published.
