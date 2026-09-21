# Executed release-candidate verification

## Android route animations — 2026-09-22

`./scripts/check.sh` passed with **938 core, 200 effects and 1 external-plugin tests**, plus example/fixture synchronization, the debug build and lint. The new cases cover six complete route presets, deterministic seeking, playback, reduced motion, compatible flow defaults, translucent comet colors and chevron loop boundaries (including the primitive budget).

The full Android instrumentation run passed **191 cases**, including native Google Maps snapshots proving that the new dots are round and advance in the route direction, geographic anchoring through camera changes, and the preset picker. One capture-only case was intentionally skipped in this ordinary run. After the final comet-tail adjustment, the focused native Canvas/capture run passed **all five cases**, including a monotonic tail-brightness regression and all six presets' actual animated pixels. These counts overlap; they are not additive.

Final logs: ignored `artifacts/route-animation-check.log`, `artifacts/route-animation-ui.log`, and `artifacts/route-previews/instrumentation.log`. The reproducible capture generated six individual GIFs and one combined comparison, each verified to contain multiple frames. See [capture provenance](media/README.md#route-animation-options). The previews are Android Canvas frames; the live Maps snapshots are separate validation. No physical-device performance claim is made by these checks.

## Earlier release-candidate runs

Local results from 2026-09-20, with Android instrumentation rerun on 2026-09-21 after the migration helpers and UI-clock fix. SDK publication remains pending.

| Suite | Passing cases | Scope |
| --- | ---: | --- |
| Kotlin core | 937 | Shared fixtures, geometry, playback, native geographic poses, 100k-point static paths, morphs and request lifecycle |
| Kotlin effects | 196 | Catalog contracts and native map primitives for 8 × 12 combinations |
| Separate Kotlin plugin | 1 | Public consumer extension contract |
| Android instrumentation | 186 | Native raster/vehicle probes, real Google SDK projection/camera fit/snapshot pixels, UI including loading/reduced motion; five Java migration regressions |
| Swift package | 1,269 | 40 test functions with parameterized contract/catalog cases and request/native geometry checks |
| iOS native host | 57 | MapKit and top-down artwork rendering |
| iOS UI | 4 | API examples, journeys, playback/plugins, loading morph and rapid gestures |
| **Total** | **2,650** | **No failures or skipped cases in the passing runs** |

Counts come from Kotlin JUnit XML and native test logs, not numbers of assertions or source methods. The 720 shared language-neutral fixture IDs run independently in Kotlin and Swift. A 1001-sample sweep counts as one test, not 1001.

Before repository separation, `./scripts/check.sh` passed example/fixture synchronization, Kotlin suites, debug build/lint and Swift package tests. After separation this script runs Android checks; Swift checks live in `trail-ios`. SDK preparation additionally ran optimized APK creation, release lint and Maven staging. A separate Android consumer app compiled with only staged public Maven coordinates plus its platform dependency. Swift package consumption and repository-export checks are recorded with the independent Swift checkout.

Android used emulator-5554 with a configured local Maps key. The Google native-snapshot regression verified magenta route and cyan vehicle pixels at expected geographic anchors under 12 camera configurations; route points and the marker are real map SDK content. Existing projection tests remain coverage for the explicitly advanced screen overlays.

iOS used the iPhone 17 Pro / iOS 26.2 simulator, Xcode 26.2. The initial incremental build after adding Canvas state crashed in generated TrailCanvas destruction code; its failed result bundle is retained. A clean DerivedData build passed all 57 native and four UI cases. Use a clean build when testing package ABI/layout changes; do not treat an old built app as current source verification. Final passing bundle: `ios/build/TrailTests-20260920-041325.xcresult` (local, ignored).

Native GIFs are actual recordings, with map attribution preserved. They illustrate appearance and behavior; they are not frame-time benchmarks.

Remaining SDK acceptance includes physical-device performance and accessibility, and fresh remote consumer resolution after publication. The sample apps are local examples; store release, store signing and store metadata are out of scope.

The independent `trail-ios` checkout then passed its 1,269 package cases and all 61 native/UI cases after a fresh rebuild against the root package. Its final local result is `Examples/build/TrailTests-20260920-043032.xcresult`.

The final optimized local example APK (`dev.trail.playground`, 1,434,079 bytes) passed APK Signature Scheme v2 verification, installed successfully on emulator-5554 and launched successfully. It uses the standard local debug certificate and is provided only as an example app.

On 2026-09-21 all 186 Android instrumentation cases passed with zero skips and the local Maps key. The migration regression tests preserve all waypoints and list ownership, retain revisions and date-line endpoints, and reject using a full route as a two-endpoint arc. The UI suite now disables repeating playback before clock-driven scrolling, and instrumentation plus CI have explicit time limits.
