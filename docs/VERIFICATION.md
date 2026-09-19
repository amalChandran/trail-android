# Executed release-candidate verification

Local results, 2026-09-20; no remote CI run or SDK publication is implied.

| Suite | Passing cases | Scope |
| --- | ---: | --- |
| Kotlin core | 937 | Shared fixtures, geometry, playback, native geographic poses, 100k-point static paths, morphs and request lifecycle |
| Kotlin effects | 196 | Catalog contracts and native map primitives for 8 × 12 combinations |
| Separate Kotlin plugin | 1 | Public consumer extension contract |
| Android instrumentation | 181 | Native raster/vehicle probes, real Google SDK projection/camera fit/snapshot pixels, UI including loading/reduced motion |
| Swift package | 1,269 | 40 test functions with parameterized contract/catalog cases and request/native geometry checks |
| iOS native host | 57 | MapKit and top-down artwork rendering |
| iOS UI | 4 | API examples, journeys, playback/plugins, loading morph and rapid gestures |
| **Total** | **2,645** | **No failures or skipped cases in the passing runs** |

Counts come from Kotlin JUnit XML and native test logs, not numbers of assertions or source methods. The 720 shared language-neutral fixture IDs run independently in Kotlin and Swift. A 1001-sample sweep counts as one test, not 1001.

Before repository separation, `./scripts/check.sh` passed example/fixture synchronization, Kotlin suites, debug build/lint and Swift package tests. After separation this script runs Android checks; Swift checks live in `trail-ios`. SDK preparation additionally ran optimized APK creation, release lint and Maven staging. A separate Android consumer app compiled with only staged public Maven coordinates plus its platform dependency. Swift package consumption and repository-export checks are recorded with the independent Swift checkout.

Android used emulator-5554 with a configured local Maps key. The Google native-snapshot regression verified magenta route and cyan vehicle pixels at expected geographic anchors under 12 camera configurations; route points and the marker are real map SDK content. Existing projection tests remain coverage for the explicitly advanced screen overlays.

iOS used the iPhone 17 Pro / iOS 26.2 simulator, Xcode 26.2. The initial incremental build after adding Canvas state crashed in generated TrailCanvas destruction code; its failed result bundle is retained. A clean DerivedData build passed all 57 native and four UI cases. Use a clean build when testing package ABI/layout changes; do not treat an old built app as current source verification. Final passing bundle: `ios/build/TrailTests-20260920-041325.xcresult` (local, ignored).

Native GIFs are actual recordings, with map attribution preserved. They illustrate appearance and behavior; they are not frame-time benchmarks.

Remaining SDK acceptance includes physical-device performance and accessibility, and fresh remote consumer resolution after publication. The sample apps are local examples; store release, store signing and store metadata are out of scope.

The independent `trail-ios` checkout then passed its 1,269 package cases and all 61 native/UI cases after a fresh rebuild against the root package. Its final local result is `Examples/build/TrailTests-20260920-043032.xcresult`.

The final optimized local example APK (`dev.trail.playground`, 1,434,079 bytes) passed APK Signature Scheme v2 verification, installed successfully on emulator-5554 and launched successfully. It uses the standard local debug certificate and is provided only as an example app.
