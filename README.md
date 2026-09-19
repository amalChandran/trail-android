# Trail 2.0 — native playgrounds

Local alpha prototype for the Kotlin / Swift relaunch of [trail-android](https://github.com/amalChandran/trail-android). The original Java project is preserved in [`legacy/android`](legacy/android/README.md). Work is on branch `trail-2-native`; nothing has been published.

**Selected API: typed Kotlin DSL / Swift result builder with named presets.** Read the [API guide](docs/API_GUIDE.md), or start from the compiler-checked [Android](docs/examples/Android.md) and [iOS](docs/examples/iOS.md) integration examples. [Decision record](docs/API_CHOICES.md).

## Run the playgrounds

From this repository:

```sh
./scripts/run-android.sh
./scripts/run-ios.sh
```

Android needs Java 17, Android SDK 36, and an emulator. The script selects an emulator, or boots the first configured AVD. A physical device is used only if you explicitly set `TRAIL_ANDROID_SERIAL`. Open the `android` directory in Android Studio and run `playground` if you prefer the IDE.

iOS needs Xcode with an iOS simulator runtime. Open [`ios/TrailPlayground.xcodeproj`](ios/TrailPlayground.xcodeproj) and run `TrailPlayground`, or use the script. The Xcode project is checked in; XcodeGen is only needed when changing `ios/project.yml`. Set `TRAIL_IOS_SIMULATOR` to choose a simulator UDID.

The Canvas playground works offline with no credentials. Google Maps is enabled by adding `MAPS_API_KEY=your-key` to `android/local.properties`, alongside `sdk.dir=...`, then rebuilding. MapKit is available in the iOS playground without a key. No location permission is requested; journeys use bundled coordinates.

**Google and Apple Maps first:** open **Map journeys** for JFK → Heathrow, a 119-point Times Square → Grand Central cab route, and a Circular Quay → Manly ferry illustration. Compare Full route, Two points, Arc and Great circle, then change styles/motions and scrub the moving vehicle. Read the [map architecture and adapter contract](docs/MAPS.md) and [fixture provenance](samples/README.md).

The journeys now have top-down vehicles with smooth steering, shadows, and eased departure/arrival. [Use the same provider-neutral pose API with your own artwork](docs/VEHICLES.md).

## Try this flow

1. Pause, scrub the progress slider, play, and replay.
2. Select a line style and an animation. Change duration and looping.
3. Enable **Use my Metro plugin** to exercise a separately compiled plugin.
4. Enable **Reduced motion**. System Reduce Motion / disabled Android animations are also respected.
5. On iOS enable **MapKit preview**, then pan and zoom. Android's equivalent requires a Maps key.
6. Background and return to the app. Playback holds while inactive.
7. Open **API examples**. Try named presets, runtime color, custom plugins, playback controls, sequences, layers, and native adapters. The visible code is extracted from the same source files the apps compile.

Eight styles: solid, cased, dashed, dotted, along-route gradient, layered glow, chevrons, tapered. Twelve motion presets: reveal, erase, ping pong, comet, multi-comet, dash flow, pulse, breathe, spotlight, segment chase, reveal + flow, draw + erase. Dash flow is visually meaningful on dashed, dotted, and chevron styles.

## Project layout

| Module | Responsibility |
| --- | --- |
| `android/trail-core` | Geometry, pure samplers, playback, public plugins, DSL; Kotlin/JVM only |
| `android/trail-effects` | Optional expressive preset catalog |
| `android/trail-android` | Canvas executor and lifecycle-aware `TrailView`; no Compose |
| `android/trail-compose` | Compose surface and observable playback adapter |
| `android/trail-google-maps` | Explicit Google Maps projection adapter |
| `android/sample-plugin` | Independent public-API consumer |
| `android/playground` | Interactive Android sample and UI tests |
| `swift` | Swift 6 package: Core, Effects, UI, MapKit, SamplePlugin products |
| `ios` | Native SwiftUI playground and XCTest UI flow |

Rendering uses Android Canvas / Core Graphics. Compose and SwiftUI host the drawing surface and controls. Neither runtime loads plugin classes by reflection. Styles record validated bounded commands during effect construction; each animation is a deterministic sampler. Reusing an effect does not reuse its playback position. Expressive effects, map adapters, sample code and UI frameworks are separate dependencies.

### Small Kotlin integration

```kotlin
import dev.trail.core.*
import dev.trail.compose.TrailCanvas
import kotlin.time.Duration.Companion.seconds

// Keep immutable geometry and reusable effects outside per-frame work.
val deliveryTrail = trailEffect {
    stroke(TrailColor.Blue, width = 6.0)
    reveal(duration = 2.seconds)
}
// Inside Compose:
TrailCanvas(path, effect = deliveryTrail)
```

For an existing Google Maps Compose map, place `GoogleMapsTrailOverlay(route, cameraPositionState, effect = deliveryTrail)` above `GoogleMap` in a `Box`, with identical bounds. Keep map controls and attribution unobscured. Route ID/revision controls replacement; projection comes from the map SDK. This surface draws over map content and cannot interleave with native map labels.

### Small Swift integration

```swift
import TrailCore
import TrailUI

let deliveryTrail = TrailEffect {
    Stroke(.blue, width: 6)
    Reveal(duration: .seconds(2))
}
// Inside a SwiftUI View:
TrailCanvas(path: path, effect: deliveryTrail)
```

Add the local `swift` directory as a Swift package in Xcode and select only the products your app needs. `TrailMap(route:effect:)` is the MapKit convenience host. `TrailMapOverlay(route:map:cameraRevision:effect:)` attaches to an existing SwiftUI `MapReader`/`Map`; increment `cameraRevision` in `onMapCameraChange(frequency: .continuous)`. Both offer controller overloads. `TrailMapAttachment` attaches to an existing `MKMapView` without replacing its delegate: forward camera/layout changes to `updateProjection()` and call `detach()` on teardown.

## Verify

```sh
./scripts/check.sh
# Android UI flows, after starting an emulator:
./scripts/test-android-ui.sh
# MapKit contracts and native iOS UI flows:
./scripts/test-ios.sh
# Verify documentation and in-app code panels match compiled sources:
python3 scripts/sync-examples.py --check
```

In Xcode use Product > Test for MapKit contracts and UI flows. Both languages consume 720 named shared fixtures, plus preset/plugin tests, a 144-case native line pixel matrix and a 24-case vehicle raster matrix per platform. UI and lifecycle tests exercise controls and real bindings. Counts distinguish parameterized cases from test functions; see [verification notes](docs/VERIFICATION.md) for executed results. Regenerate documentation with `python3 scripts/sync-examples.py` and fixture copies with `python3 scripts/sync-fixtures.py`.

## Alpha boundaries

This is a working foundation for testing the approved integration API, not a production-release claim. [The relaunch plan](docs/DESIGN.md) lists consumer testing, additional camera/world-copy stress, visual goldens, device profiling, application-size measurements and compatibility/publishing gates. Date-line splitting and discontinuous rendering are implemented; provider-specific globe/extreme-pitch behavior still needs acceptance testing. Large-route simplification and expensive gradient/comet combinations need profiling. Swift offers recoverable structural builder validation; numeric/plugin programmer errors still use preconditions. External geographic input uses throwing validation. Annotation/macro tooling is deferred.

Build pins: Kotlin 2.3.20, AGP 8.13.2, Gradle 8.13, Compose BOM 2026.03.00; Swift tools 6.0, iOS 17+. These are working compatibility pins, not a claim that every dependency is the newest available release. Google Maps and Material are excluded from the lean core/Canvas products.

The upstream MIT license and attribution are retained in [`license.md`](license.md). License metadata in the historical project differs; resolve provenance before publishing a coordinated release.
