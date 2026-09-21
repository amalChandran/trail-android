# Native recording provenance

## Route animation options

The seven `android-route-*.gif` files were generated on 2026-09-22 from **real Android Canvas pixels** on `emulator-5554`. `RouteAnimationRenderingTest` calls the production `TrailRenderer` with each production `TrailRoutePreset`; no separate SVG, browser animation or approximation of the sampler is involved. These are local polyline previews, not Google Maps recordings or frame-rate benchmarks.

Each individual preview is 420 × 236, 144 frames, six seconds at 24 fps. The combined `android-route-options.gif` puts the same six frame sequences in an 840 × 708 grid. Every default period divides six seconds, so the exported loop closes cleanly. Titles, grid, captions and endpoint dots are capture decorations; the base and animated route are drawn by the library. ffmpeg encodes the PNGs with a generated palette; GIF frame timing is quantized to hundredths of a second.

Reproduce with an already-running emulator, Java 17, the Android SDK and ffmpeg:

```sh
TRAIL_ANDROID_SERIAL=emulator-5554 ./scripts/record-route-previews.sh
```

The script builds and installs the playground and test APKs, runs the pixel regressions, exports deterministic PNG frames to the app's external files directory, pulls them into ignored `artifacts/route-previews`, and encodes all seven GIFs. It stops on a failed pixel check. A Maps key is not required for these Canvas captures. The separate `NativeAnchoringTest` checks moving dots in real Google Maps snapshots when a key is configured.

## Journey and loading recordings

Recorded on 2026-09-20 from the actual updated apps, not generated visuals:

- `android-flight.gif`: Google Maps SDK, JFK–Heathrow arc, top-down aircraft, 16-second reveal.
- `android-cab.gif`: Google Maps SDK, full Times Square–Grand Central street geometry, route-following vehicle heading.
- `android-ferry.gif`: Google Maps SDK, Circular Quay–Manly illustrative harbor waypoints, 20-second reveal.
- `android-loading-route.gif`: native Google Maps travelling arc, delayed bundled response, 650 ms geometry morph.
- `ios-loading-route.gif`: native SwiftUI MapKit content and the same request/morph model, recorded during the passing loading/gesture UI test.

Android source recordings used `adb -s emulator-5554 shell screenrecord --size 672x1496`; iOS used `xcrun simctl io … recordVideo`. GIFs crop app chrome while retaining map attribution, then downscale to 560 pixels wide at 16 fps for the three journey loops, or 420–480 pixels at 12–16 fps for loading loops using ffmpeg palette generation. Loading GIFs hold their last real frame for a short pause. They are explanatory animations, not frame-rate benchmarks. Originals remain in ignored `artifacts/`.

`./scripts/release/verify-media.py` checks that README references exist and contain multiple frames. Regenerate after changing the rendering or demo data; never replace these with mock map illustrations while retaining the “actual recording” claim.

The Android journey GIFs were re-exported on 2026-09-21 at 560 px from the original native recordings, using crop `618:665:27:345` to show the title, full map and point count. The README uses larger individual previews. Fresh Swift vehicle loops live in [trail-ios](https://github.com/amalChandran/trail-ios); both READMEs cross-link the SDKs.
