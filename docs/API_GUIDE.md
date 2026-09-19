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

No Trail 2 artifacts are published yet. The Android playground consumes Gradle project modules; open `android/` to run or adapt it. A Compose consumer uses `trail-compose`, which includes the core and Canvas adapter transitively. A View consumer uses `trail-android` and sets `TrailView.path` and `TrailView.effect`. Add `trail-effects` for the larger preset catalog or `trail-google-maps` for that provider. Material and the gallery belong to the sample app.

For Swift, add the repository's `swift/` directory as a local Swift package in Xcode. Select `TrailUI` for a SwiftUI Canvas or `TrailMapKit` for maps; `TrailEffects` is optional. A plugin needs only `TrailCore`. `TrailSamplePlugin` demonstrates an independent consumer and is not required by the engine.

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

- `play` resumes; a completed one-shot starts again. `pause` holds position. `replay` starts at zero.
- `seek` accepts 0 through 1 and pauses. For a single animation or sequence, explicit seek to 1 displays its terminal frame even when repeating. Independent layers retain their own time domains. Ordinary loop boundaries begin the next cycle at zero.
- Effect changes preserve normalized progress and play/pause intent. New colors do not replay a finished reveal. Static-to-animated changes honor the original autoplay request unless explicitly paused.
- Use stable path/effect values where possible. Kotlin's runtime-color example uses `remember(brandColor)`. Rebuild an effect when its configuration changes, not on each frame.
- A controller belongs to **one visible binding**. Share immutable effects across bindings, not controllers.
- Automatically owned map bindings replay when route ID/revision changes; camera changes only reproject. If you supply an Android controller, also own its route-replacement/reset policy. Swift map bindings replay on route changes even with a supplied controller.
- Background/inactive/reduced-motion surfaces do not accrue elapsed time. Reduced motion draws a full reveal route or a hidden terminal erase; resume continues the held logical position.

## Write a plugin

Implement `TrailLineStyle.draw` to record strokes or chevrons. Implement `TrailAnimation.sample` to return visual state for the supplied `TrailTime`. Both are ordinary public interfaces/protocols; there is no registration, manifest or build processor.

The separate [Kotlin plugin](../android/sample-plugin/src/main/kotlin/dev/trail/plugin/MetroStyle.kt) and [Swift plugin](../swift/Sources/TrailSamplePlugin/MetroStyle.swift) contain complete small implementations: a white casing, colored stroke, chevrons, and a quadratic reveal. Their sample consumers use `style(MetroStyle(...))` / `Style(MetroStyle(...))` and `TrailAnimations.custom(QuadraticReveal(), ...)` inside the canonical builder.

Styles run during effect preparation. Drawing order is call order. Kotlin rejects retaining and reusing a closed draw context; Swift's context is a scoped value. Capture immutable configuration. Samplers must be deterministic and independent of call order: seeking directly to 80% must produce the same state as normal playback at 80%. Do not start a timer, fetch a map or retain a view in a sampler. Swift plugins conform to `Sendable`.

Visual state supplies visible windows, opacity, width scale, dash phase and an optional head fraction. Windows and fractions are bounded; at most 256 windows, 512 commands per style, 16 layers, and 64 sequence steps are allowed. These are safeguards, not claims that the maximum combination is performant. Custom raw graphics, GPU sessions and arbitrary path commands are not public plugin capabilities in this alpha.

## Attach to maps

Android: place `GoogleMapsTrailOverlay` in the same `Box` as the existing `GoogleMap`, with matching bounds and its `cameraPositionState`. The [compiled example](examples/Android.md) includes normal map setup. A Maps key is necessary to run it.

iOS: use `TrailMap(route:effect:)` for a convenience host. For an existing SwiftUI map, use `MapReader` and `TrailMapOverlay`, advancing `cameraRevision` from continuous camera callbacks. For an existing `MKMapView`, use `TrailMapAttachment`, forward camera/layout changes to `updateProjection()`, and call `detach()` on teardown. Your screen continues to own the map delegate. All three have [compiled examples](examples/iOS.md).

These are overlays above map content; they cannot interleave with native labels. Antimeridian-crossing routes are explicitly unsupported. Camera-transform accuracy and broader lifecycle stress remain release work; see [verification limits](VERIFICATION.md).
