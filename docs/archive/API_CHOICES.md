> Historical proposal. The selected, implemented API is documented in [the current API guide](../API_GUIDE.md). Examples here may be unimplemented.

# Choose Trail's integration API

These are three candidate developer experiences for the same native engines. The local Kotlin DSL and Swift result-builder prototype compiles. Fluent factories and annotation/macro processors below are proposals, not implemented or compiler-validated APIs. Selecting one does not change the public style and animation plugin contracts.

All examples assume `path` is already available. `TrailCanvas` is the map-independent surface; the same `TrailEffect` goes into the Google Maps and MapKit adapters. Geometry acquisition, imports, and normal map setup are shared overhead and omitted equally. Widths below are logical display units: Android dp / iOS points. These examples deliberately show both the reusable declaration and its screen call site.

## A. Typed DSL / Swift result builder + named presets

Describe the effect in a small block. Kotlin uses a receiver lambda; Swift uses a result builder. Ordinary source code, compiler type checking, IDE completion, and no code-generation setup.

### 1. A blue route that reveals in two seconds

```kotlin
TrailCanvas(path) {
    stroke(TrailColor.Blue, width = 6.0)
    reveal(duration = 2.seconds)
}
```

```swift
TrailCanvas(path: path) {
    Stroke(.blue, width: 6)
    Reveal(duration: .seconds(2))
}
```

### 2. One reusable branded preset

```kotlin
val DeliveryTrail = trailEffect {
    style(TrailStyles.cased(TrailColor.Mint, width = 6.0))
    reveal(duration = 2.seconds, repeat = true)
}

// Every screen:
TrailCanvas(path, effect = DeliveryTrail)
```

```swift
let deliveryTrail = TrailEffect {
    Style(TrailStyles.cased(.mint, width: 6))
    Reveal(duration: .seconds(2), repeats: true)
}

// Every screen:
TrailCanvas(path: path, effect: deliveryTrail)
```

### 3. Use a third-party style and animation, with runtime values

```kotlin
val effect = trailEffect {
    style(MetroStyle(color = brandColor))
    animation(TrailAnimations.custom(
        QuadraticReveal(), duration = 2.seconds,
    ))
}
TrailCanvas(path, effect = effect)
```

```swift
let effect = TrailEffect {
    Style(MetroStyle(color: brandColor))
    Animate(TrailAnimations.custom(
        QuadraticReveal(), duration: .seconds(2)
    ))
}
TrailCanvas(path: path, effect: effect)
```

A plugin may also export its own vocabulary. The prototype already has Kotlin `trailEffect { metro() }` and Swift `TrailEffect.metro()`. Applications import the plugin and call it; they do not register it.

**Best fit:** approachable customization, expressive layers, and plugins that feel native to Trail. Nested blocks are a tradeoff when an effect becomes complicated; name and reuse it instead of nesting a large configuration in a screen.

## B. Fluent methods + named presets

Build an immutable effect by chaining ordinary functions. The proposed return value of each operation is another `TrailEffect`; configuration happens before rendering, not through a chain interpreted each frame.

### 1. The same two-second reveal

```kotlin
TrailCanvas(path, effect =
    TrailEffect.stroke(TrailColor.Blue, width = 6.0)
        .reveal(duration = 2.seconds)
)
```

```swift
TrailCanvas(path: path, effect:
    TrailEffect.stroke(.blue, width: 6)
        .reveal(duration: .seconds(2))
)
```

### 2. The same reusable branded preset

```kotlin
val DeliveryTrail = TrailEffect
    .style(TrailStyles.cased(TrailColor.Mint, width = 6.0))
    .reveal(duration = 2.seconds, repeat = true)

TrailCanvas(path, effect = DeliveryTrail)
```

```swift
let deliveryTrail = TrailEffect
    .style(TrailStyles.cased(.mint, width: 6))
    .reveal(duration: .seconds(2), repeats: true)

TrailCanvas(path: path, effect: deliveryTrail)
```

### 3. The same third-party plugins and runtime color

```kotlin
val effect = TrailEffect
    .style(MetroStyle(color = brandColor))
    .animate(QuadraticReveal(), duration = 2.seconds)

TrailCanvas(path, effect = effect)
```

```swift
let effect = TrailEffect
    .style(MetroStyle(color: brandColor))
    .animate(QuadraticReveal(), duration: .seconds(2))

TrailCanvas(path: path, effect: effect)
```

Plugins may add extension methods/factories such as `TrailEffect.metro(color)` without any generator. The design must define whether a second animation replaces the first or is invalid; my recommendation is to reject ambiguous duplicates and use explicit layers for simultaneous animations, as the DSL prototype does.

**Best fit:** compact effects, familiar IDE dot completion, and a small public surface. Layering and sequencing need explicit methods; a long chain can conceal ordering rules if we design them poorly.

## C. Kotlin annotations / Swift macros + generated presets

Define the reusable effect declaratively. A build-time tool emits ordinary effect-building code. The short screen call includes generated code behind it. KSP setup on Android and a compiler macro target on Swift are additional integration and maintenance costs.

### 1. A generated two-second reveal

```kotlin
// Declare once. These annotation names/signatures are proposed.
@TrailEffects
interface AppTrails {
    @Stroke(argb = 0xFF2563EB, widthDp = 6)
    @Reveal(durationMillis = 2000)
    fun delivery(): TrailEffect
}

// Generated implementation, created once and reused:
val trails = AppTrailsGenerated()

// Every screen:
TrailCanvas(path, effect = trails.delivery())
```

```swift
// Proposed attached macro generates DeliveryTrail.effect.
@TrailPreset(stroke: .blue, width: 6, revealSeconds: 2)
struct DeliveryTrail {}

// Every screen:
TrailCanvas(path: path, effect: DeliveryTrail.effect)
```

### 2. Runtime brand color

```kotlin
@TrailEffects
interface AppTrails {
    @Stroke(widthDp = 6)
    @Reveal(durationMillis = 2000, repeat = true)
    fun delivery(@StrokeColor color: TrailColor): TrailEffect
}

val trails = AppTrailsGenerated()
TrailCanvas(path, effect = trails.delivery(brandColor))
```

```swift
// Proposed macro convention: this stored color supplies the generated stroke.
// Requires a compiler prototype before we commit to this spelling.
@TrailPreset(width: 6, revealSeconds: 2, repeats: true)
struct DeliveryTrail {
    let color: TrailColor
}

TrailCanvas(path: path, effect: DeliveryTrail(color: brandColor).effect)
```

### 3. A generated preset using custom plugins

```kotlin
@TrailEffects
interface AppTrails {
    @UseStyle(MetroStyle::class)
    @UseAnimation(QuadraticReveal::class, durationMillis = 2000)
    fun metro(): TrailEffect
}

val trails = AppTrailsGenerated()
TrailCanvas(path, effect = trails.metro())
```

```swift
@TrailPreset(
    style: MetroStyle(),
    animation: QuadraticReveal(),
    durationSeconds: 2
)
struct MetroTrail {}

TrailCanvas(path: path, effect: MetroTrail.effect)
```

The Kotlin plugin-class version above assumes accessible zero-argument constructors. Passing dependencyful plugin instances needs a documented generated-parameter contract or ordinary effect construction. The macro examples similarly need a compiler experiment for expression handling and diagnostics. These limits are part of the comparison, not solved features.

**Best fit:** applications with a large catalog of mostly static named effects and teams that value declarative specifications. It adds little to a one-off route compared with simply naming a DSL/fluent value. My recommendation is an optional layer over A or B if you choose it; regular values should remain usable without the processor.

## What stays the same

Third-party plugin authors implement these tiny contracts regardless of the app-facing syntax:

```kotlin
class MetroStyle(private val color: TrailColor) : TrailLineStyle {
    override fun draw(context: TrailDrawContext) {
        context.stroke(TrailColor.White, width = 12.0)
        context.stroke(color, width = 7.0)
    }
}
```

```swift
struct MetroStyle: TrailLineStyle {
    let color: TrailColor
    func draw(in context: inout TrailDrawContext) {
        context.stroke(color: .white, width: 12)
        context.stroke(color: color, width: 7)
    }
}
```

Playback remains separate from the reusable effect. One preset can be used on multiple screens without sharing playback position. Kotlin's `rememberTrailPlayback(effect)` and Swift's `@State`-owned `TrailPlayback(effect:)` expose play, pause, replay, and seek. Maps remain optional adapters, and no option needs a runtime plugin registry.

| Decision criterion | A: DSL / builder | B: Fluent | C: Generated presets |
| --- | --- | --- | --- |
| Shortest one-off declaration | Very short | Very short | More declaration/setup |
| Reusing a named preset | One call | One call | One call |
| Runtime values | Direct parameters/expressions | Direct parameters/expressions | Generator parameter rules |
| Plugin distribution | Ordinary library | Ordinary library | Ordinary library; extra generator rules when referenced by annotations |
| Build integration | Normal dependencies | Normal dependencies | KSP or macro support |
| Runtime reflection needed | No | No | No, under proposed generation design |
| Size / frame-rate winner | Not measured | Not measured | Not measured |
| Current local status | Native prototype compiles | Proposed syntax | Proposed syntax/tooling |

**Recommendation: A with named presets.** B is equally credible if you prefer chains. C is attractive for a preset catalog but should be selected for its declaration model, not an unmeasured claim that it makes the APK smaller or the animation faster.

All three can normalize to the same prepared commands and renderer. Dependency boundaries, shrinking, geometry caching, frame scheduling, and effect complexity are more consequential than braces versus dots. We should measure actual release artifacts after selecting the API.
