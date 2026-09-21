# Native recording provenance

Recorded on 2026-09-20 from the actual updated apps, not generated visuals:

- `android-flight.gif`: Google Maps SDK, JFK–Heathrow arc, top-down aircraft, 16-second reveal.
- `android-cab.gif`: Google Maps SDK, full Times Square–Grand Central street geometry, route-following vehicle heading.
- `android-ferry.gif`: Google Maps SDK, Circular Quay–Manly illustrative harbor waypoints, 20-second reveal.
- `android-loading-route.gif`: native Google Maps travelling arc, delayed bundled response, 650 ms geometry morph.
- `ios-loading-route.gif`: native SwiftUI MapKit content and the same request/morph model, recorded during the passing loading/gesture UI test.

Android source recordings used `adb -s emulator-5554 shell screenrecord --size 672x1496`; iOS used `xcrun simctl io … recordVideo`. GIFs crop app chrome while retaining map attribution, then downscale to 560 pixels wide at 16 fps for the three journey loops, or 420–480 pixels at 12–16 fps for loading loops using ffmpeg palette generation. Loading GIFs hold their last real frame for a short pause. They are explanatory animations, not frame-rate benchmarks. Originals remain in ignored `artifacts/`.

`./scripts/release/verify-media.py` checks that README references exist and contain multiple frames. Regenerate after changing the rendering or demo data; never replace these with mock map illustrations while retaining the “actual recording” claim.

The Android journey GIFs were re-exported on 2026-09-21 at 560 px from the original native recordings, using crop `618:665:27:345` to show the title, full map and point count. The README uses larger individual previews. Fresh Swift vehicle loops live in [trail-ios](https://github.com/amalChandran/trail-ios); both READMEs cross-link the SDKs.
