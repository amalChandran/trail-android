# Trail's integration API

Use **a named effect, then a surface**. The effect can be shared across screens; playback position is local to each binding.

```kotlin
val deliveryTrail = trailEffect {
    stroke(TrailColor.Blue, width = 6.0)
    reveal(duration = 2.seconds)
}

TrailCanvas(path, effect = deliveryTrail)
```

```swift
let deliveryTrail = TrailEffect {
    Stroke(.blue, width: 6)
    Reveal(duration: .seconds(2))
}

TrailCanvas(path: path, effect: deliveryTrail)
```

These declarations come from the compiled integration examples. See [Android examples](examples/Android.md) and [iOS examples](examples/iOS.md) for complete imports, geometry, runtime colors, plugins, controls, sequences, layers, and map setup. In either playground, open **API examples** to run them and read the source.

## Local adoption

Trail 2 Android artifacts are staged locally; Maven Central publication is pending. See [distribution](DISTRIBUTION.md) for the verified Maven consumer flow and independent Swift package. The Android playground consumes Gradle project modules; open `android/` to run or adapt it. A Compose consumer uses `trail-compose`, which includes the core and Canvas adapter transitively. A View consumer uses `trail-android` and sets `TrailView.path` and `TrailView.effect`. Add `trail-effects` for the larger preset catalog or `trail-google-maps` for that provider. Material and the gallery belong to the sample app.

For Swift, use the independent `trail-ios` root package from its public GitHub repository or a local sibling checkout. Select `TrailUI` for a SwiftUI Canvas or `TrailMapKit` for maps; `TrailEffects` is optional. A plugin needs only `TrailCore`. `TrailSamplePlugin` demonstrates an independent consumer and is not required by the engine.

Widths, dash lengths and chevron sizes are logical units: Android dp, iOS points, represented by `Double`. `TrailColor` contains sRGB ARGB. `TrailPath` is local Cartesian geometry, fitted into the surface by default. `TrailRoute` is validated geographic geometry for map adapters, with a stable ID and revision.

## Composition rules

| Declaration | Meaning |
| --- | --- |
| Empty effect | Static solid blue line, width 6 |
| One style + one animation | Animate that style; declaration order does not change meaning |
| Two styles or two animations in one layer | Configuration error; no implicit replacement or chaining |
| `sequence { }` / `Sequence { }` | Run 1–64 finite clips in order; each receives its own duration and local time |
| Repeat on a sequence | Repeat the entire sequence; repeating inner clips are rejected |
| `layer { }` / `Layer { }` | Draw 1–16 independent layers in declaration order, back to front |
| Layer blocks mixed with top-level style/animation | Configuration error; put all declarations in explicit layers |

The compiled examples show the actual sequence and layer syntax. A layer without an animation stays static. Layers use their own durations; the overall finite playback duration is the longest layer. Any repeating layer keeps the binding running. A sequence's reduced-motion presentation is the last step's declared static state.

Kotlin reports structural mistakes with `IllegalArgumentException` and an actionable message. Swift's ordinary builder treats them as programmer errors with a descriptive precondition failure. A configurable editor can use `try TrailEffect.validating { ... }` to recover a `TrailConfigurationError` for duplicate, mixed-layer, layer-count and sequence errors. This does not turn every numeric/plugin precondition into a recoverable error. External geographic input uses throwing validation in Swift.

## Playback and updates

The simple effect overload owns its controller. Create an explicit controller only for controls: `rememberTrailPlayback(effect)` on Android, or `@State private var playback = TrailPlayback(effect: ...)` in SwiftUI. The compiled playback examples show the binding and slider code. `TrailPlayer` is the deterministic core for tests and custom hosts.

With an explicit controller, Kotlin can omit the effect argument: `TrailCanvas(path, playback = playback)` and `GoogleMapsTrail(route, camera, playback = playback)` use the controller's configured effect. Supplying an effect explicitly updates that controller. Swift's playback overload likewise uses the controller's effect.

- `play` resumes; a completed one-shot starts again. `pause` holds position. `replay` starts at zero.
- `seek` accepts 0 through 1 and pauses. For a single animation or sequence, explicit seek to 1 displays its terminal frame even when repeating. Independent layers retain their own time domains. Ordinary loop boundaries begin the next cycle at zero.
- Effect changes preserve normalized progress and play/pause intent. New colors do not replay a finished reveal. Static-to-animated changes honor the original autoplay request unless explicitly paused.
- Use stable path/effect values where possible. Kotlin's runtime-color example uses `remember(brandColor)`. Rebuild an effect when its configuration changes, not on each frame.
- A controller belongs to **one visible binding**. Share immutable effects across bindings, not controllers.
- Automatically owned map bindings replay when route ID/revision changes; camera changes only reproject. If you supply a controller, own its route-replacement/reset policy on both platforms. `TrailMapAttachment.updateRoute` preserves position by default; opt into a restart with `replay: true`.
- Background/inactive/reduced-motion surfaces do not accrue elapsed time. Reduced motion draws a full reveal route or a hidden terminal erase; resume continues the held logical position.

## Write a plugin

Implement `TrailLineStyle.draw` to record strokes or chevrons. Implement `TrailAnimation.sample` to return visual state for the supplied `TrailTime`. Both are ordinary public interfaces/protocols; there is no registration, manifest or build processor.

The separate [Kotlin plugin](../android/sample-plugin/src/main/kotlin/dev/trail/plugin/MetroStyle.kt) and [Swift plugin](https://github.com/amalChandran/trail-ios/blob/main/Sources/TrailSamplePlugin/MetroStyle.swift) contain complete small implementations: a white casing, colored stroke, chevrons, and a quadratic reveal. Their sample consumers use `style(MetroStyle(...))` / `Style(MetroStyle(...))` and `TrailAnimations.custom(QuadraticReveal(), ...)` inside the canonical builder.

Styles run during effect preparation. Drawing order is call order. Kotlin rejects retaining and reusing a closed draw context; Swift's context is a scoped value. Capture immutable configuration. Samplers must be deterministic and independent of call order: seeking directly to 80% must produce the same state as normal playback at 80%. Do not start a timer, fetch a map or retain a view in a sampler. Swift plugins conform to `Sendable`.

Visual state supplies visible windows, opacity, width scale, dash phase and an optional head fraction. Windows and fractions are bounded; at most 256 windows, 512 commands per style, 16 layers, and 64 sequence steps are allowed. These are safeguards, not claims that the maximum combination is performant. Custom raw graphics, GPU sessions and arbitrary path commands are not public plugin capabilities in this alpha.

## Attach to maps

Use native map content by default. Android places `GoogleMapsTrail(route, camera, effect = effect)` inside `GoogleMap { }`. Swift offers `TrailMap(route:effect:)`; existing maps use `TrailMapContent(geometry:playback:)` inside `Map { }` plus `.trailPlayback(playback)`. These geographic objects move with the map renderer rather than a separate screen projection clock.

Supply an app-owned up-facing bitmap to `GoogleMapsTrailVehicle` on Android. SwiftUI hosts use a native `Annotation` at `TrailMapGeometry.pose(at:)`; the compiled journey sample demonstrates artwork, heading and camera rotation.

`TrailRoute` preserves full geographic geometry. Its `direct`, `arc`, `greatCircle` and `encodedPolyline` factories have explicit meanings and do not fetch directions. A complete guide to geography, loading transitions, native provider capabilities and the advanced screen-space overlay APIs is in [MAPS.md](MAPS.md).

## Loading directions

Create `rememberTrailRouteTransition(from,to)` in Compose or a `TrailRouteTransitionState` in SwiftUI. Capture its `request` token before awaiting your directions service. Call `resolve(request,route)` on success, `fail` on failure and `cancel` on cancellation. Use the state's `route` in the native map binding. While `Loading`, use `TrailAnimations.loading()`; while `Morphing`, show a full stroke; on `Ready`, use your chosen reveal preset. The full compiling examples include exception handling and clocks.

The app owns networking and retry UI. Stale, duplicate and foreign tokens return false. Invalid routes throw without changing loading state. Requests require matching endpoints within 150 meters for road snapping. The default morph is 650 ms, retains target corners, unwraps world seams, stops in the background and settles immediately with reduced motion. Do not run your own second timer over this binding.
