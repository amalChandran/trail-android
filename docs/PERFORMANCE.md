# Performance and size evidence

Release candidate `2.0.0-alpha02`, measured locally on 2026-09-20. Results describe these fixtures and toolchains, not universal size, frame-rate or battery guarantees.

## Work removed

- Native map polylines/markers replace camera-callback-driven screen overlays in the primary adapters and demos. Geographic geometry is prepared independently of camera pan/bearing/tilt.
- Google journeys fit from the SDK-ready/layout callback before tile loading. Playback waits for initial map readiness; it no longer continuously invalidates the map while waiting for its first complete load.
- Full native stroke/casing layers reuse the original coordinate buffers, tested with 100,000 road vertices. Pose lookup uses measured-distance binary search.
- Kotlin caches dash interval arrays; animated phase changes no longer rebuild those arrays.
- Swift Canvas keeps prepared Core Graphics paths for fixed stroke ranges, with a bounded cache. Moving partial reveals remain dynamic.
- Artwork paths and native vehicle icons are prepared once. A map owns one playback driver; Canvas has its own driver only when it is the selected binding. Paused, backgrounded and reduced-motion bindings stop clocks. The Google journey also stops when scrolled out of view.
- Effects, artwork, Material UI and sample fixtures are optional or app-only. R8/resource shrinking is enabled for the release app; Swift release dead stripping is enabled in its size probes. There is no runtime plugin discovery, code generation or directions client.

## Reproduce size measurements

```sh
cd android
./gradlew :size-probe:assembleRelease
cd ..
./scripts/release/prepare-android.sh
python3 scripts/release/audit-artifacts.py
(cd ../trail-ios && ./scripts/release/measure-ios.sh)
```

The Android probes have matching platform dependencies and SDK settings. `viewBase` draws one native static line; `viewTrail` uses TrailView and a two-point reveal. `mapBase` embeds Google Maps and a native polyline; `mapTrail` embeds the same Maps host and a Trail two-point reveal. Both builds use R8 and resource shrinking. Compiler-only Compose dependencies for the View probe are not packaged.

Initial observed APK archive deltas: **13,728 bytes (13.4 KiB)** for the View example and **16,384 bytes (16 KiB)** for Google Maps. See `artifacts/release/audit.json` for exact matched APK sizes and hashes. Adding the catalog, complex plugins, loading transitions or vehicle assets can retain more code. APK ZIP alignment also affects byte deltas. These are not all-features measurements or Play download/install estimates.

The Swift probe compares unsigned arm64 iPhoneOS **Release executable** bytes for a native MapPolyline host and a TrailMap two-point reveal, using dead stripping. Observed default Release increment: **590,296 bytes (576.5 KiB)**. Its report is `artifacts/ios-size-probe/size.json`. Executable deltas exclude App Store encryption, thinning, signing and compression. A local source package is not an embedded binary of a fixed size.

## Anchoring and timing evidence

`NativeAnchoringTest` asks the Google Maps SDK for its own snapshot, then finds route/vehicle pixels at coordinates projected by that SDK. A sibling Canvas cannot appear in this snapshot. It checks 12 alternating camera positions with zoom, bearing and tilt while playback remains paused at 65%.

The iOS native UI test resolves a loading arc, seeks, then performs six rapid alternating map swipes and verifies retained playback state and map content. Core tests analytically check geographic head position/bearing, seam-safe geometry, request lifecycle and all catalog pairs. These do not measure per-frame physical-device pixel drift.

Baseline profiles from dependencies are packaged by AGP. No new app-specific startup profile or physical-device macrobenchmark has been claimed. Map network/SDK initialization and tile latency remain provider/device dependent. Before production rollout, measure cold startup, frame pacing and battery on representative physical devices, including release builds on 16 KB-page Android hardware and older supported iPhones. Very long animated routes and dense gradient/comet combinations deserve application-specific budgets.

The optimized, locally signed example APK is 1,434,079 bytes. It includes the playground UI, catalog and map integration, whereas the matched-host deltas above isolate a basic library integration.
