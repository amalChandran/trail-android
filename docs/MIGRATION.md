# Migrate Java Trail to Kotlin Trail 2

Trail 2 replaces the legacy Java API with immutable routes, named effects and native map content. It is a source migration, not a dependency-version-only upgrade. The original project is preserved in [`legacy/android`](../legacy/android), and existing release tags remain available.

The archived implementation uses `RouteOverlayView` and `OverlayPolyline.Builder`. Some older published examples call the builder `Route.Builder`; use the API in your installed version when locating the old integration. There is no `TrailOverlayView.Builder` in the archived source.

## 1. Choose the integration boundary

| Existing screen | Trail 2 integration |
| --- | --- |
| Google Maps Compose | Add `GoogleMapsTrail` inside your existing `GoogleMap { }` |
| XML / `SupportMapFragment` with `RouteOverlayView` | Migrate the map surface to Google Maps Compose, hosted in a `ComposeView` if the surrounding screen still uses Views |
| A line drawn in an ordinary Android View | Use `TrailView` from `trail-android`; no Compose or map dependency is needed |
| A Compose Canvas | Use `TrailCanvas` from `trail-compose` |
| A custom map renderer | Implement a native provider adapter using `TrailMapGeometry`; the explicit screen-overlay API is also available with different camera-timing limits |

The supplied native Google adapter requires Google Maps Compose. `TrailView` is an ordinary Canvas View, **not** a drop-in native Google Map overlay. Keep the old map screen until its replacement is ready if adopting Compose is a separate migration for your app.

Use Java 17 and minimum Android API 24 for the current Android SDK. The sample uses Kotlin 2.3.20 and compile SDK 36. Select `trail-google-maps` for the native Google integration, adding `trail-effects` only when you need its preset catalog.

Maven Central publication is pending. Follow [local candidate installation](DISTRIBUTION.md#android-maven-central) to build a staged repository and test it in your app. Do not replace the old JitPack coordinate with a proposed Central coordinate before it is published. Imports change from `com.amalbit.trail.*` to `dev.trail.*`.

## 2. Preserve the route; replace the paints

The actual legacy [sample](../legacy/android/app/src/main/java/com/amalbit/animationongooglemap/projectionBased/OverlayRouteActivity.java) builds a projected overlay:

```java
OverlayPolyline route = new OverlayPolyline.Builder(mRouteOverlayView)
    .setRouteType(RouteType.PATH)
    .setCameraPosition(mMap.getCameraPosition())
    .setProjection(mMap.getProjection())
    .setLatLngs(mRoute)
    .setBottomLayerColor(Color.YELLOW)
    .setTopLayerColor(Color.RED)
    .create();
```

In Kotlin, convert at the boundary where Google coordinates enter your app's route model. Preserve **all** waypoints, in their original order:

```kotlin
import com.google.android.gms.maps.model.LatLng
import dev.trail.core.TrailCoordinate
import dev.trail.core.TrailRoute

fun migrateLegacyPath(
    id: String, points: List<LatLng>, revision: Long = 0,
): TrailRoute = TrailRoute(
    id, points.map { TrailCoordinate(it.latitude, it.longitude) }, revision,
)
```

This makes an immutable snapshot. Later mutations to the old `MutableList<LatLng>` do not move an already-visible route. Use a stable trip ID; increment `revision` when replacing that trip's directions. Create a new `TrailRoute` for new data instead of mutating its coordinates. Empty routes safely draw nothing.

Define the replacement appearance once:

```kotlin
import dev.trail.core.TrailColor
import dev.trail.core.trailEffect
import kotlin.time.Duration.Companion.seconds

val migratedRouteEffect = trailEffect {
    layer { stroke(TrailColor.White, width = 10.0) }
    layer {
        stroke(TrailColor.Blue, width = 6.0)
        reveal(duration = 2.seconds)
    }
}
```

This example deliberately chooses a white casing and blue reveal. Use `TrailColor(oldArgbInt)` to retain your own colors. Widths are logical **dp**, not Android Canvas pixels; divide any old pixel measurement by the density before passing it through. Durations use Kotlin `Duration`, not an unlabelled millisecond number.

For the Java helper's repeated foreground-over-background animation, use the optional catalog's ready-made route sweep:

```kotlin
import dev.trail.effects.TrailRoutePreset

val sweepingRoute = TrailRoutePreset.RouteSweep.effect(
    color = TrailColor.Blue,      // old topLayerColor
    baseColor = TrailColor.White, // old bottomLayerColor
    width = 6.0,
)
```

It draws, settles and fades the foreground before repeating, while retaining the full base. It is inspired by the Java behavior rather than reproducing its hard color reset and animator restarts. `MovingDots`, `MovingDashes`, `Loading`, `Comet` and `DrawAndErase` provide other complete pairings, all compatible with Canvas, View and native Google Maps. See the [animated comparison](../README.md#route-animation-options).

The full [compiled migration example](../android/playground/src/main/kotlin/dev/trail/playground/examples/MigrationExamples.kt) contains imports, conversion helpers, the preset and this map wrapper:

```kotlin
@Composable fun MigratedGoogleRoute(
    route: TrailRoute,
    camera: CameraPositionState,
    modifier: Modifier = Modifier,
) {
    GoogleMap(modifier, cameraPositionState = camera) {
        GoogleMapsTrail(route, camera, effect = migratedRouteEffect)
    }
}
```

Call it with `migrateLegacyPath("cab/current-trip", oldPoints, revision)` and the camera state owned by your screen. If the screen already contains `GoogleMap`, add only its inner `GoogleMapsTrail` call. Avoid creating a second map.

## 3. Make geometry and drawing separate choices

| Legacy choice | Trail 2 geometry | Trail 2 appearance / motion |
| --- | --- | --- |
| `RouteType.PATH` | `migrateLegacyPath` / `TrailRoute(id, allPoints, revision)` | `stroke`, a custom style, or explicit layers |
| `RouteType.ARC` | `TrailRoute.arc(id, origin, destination)` | Any style; `reveal` or another motion |
| `RouteType.DASH` | Keep the full `TrailRoute` | `style(TrailStylePreset.Dashed.style())` from optional `trail-effects` |
| Point-to-point flight | `arc` for a decorative curve; `greatCircle` for a sampled shortest spherical connection | Flight marker and your chosen animation |
| An encoded directions response | `TrailRoute.encodedPolyline(id, encoded, precision = 5)` | The same named effect; select precision 6 when that is what your service returns |

The compiled `migrateLegacyArc` helper requires **exactly two endpoints** and rejects a full directions route. If you intentionally want an arc between the first and last points of an old route, explicitly pass `listOf(oldPoints.first(), oldPoints.last())` after validating that the list is nonempty. This prevents silent loss of road turns.

`direct`, `arc` and `greatCircle` do not fetch directions. A road route still comes from your app's chosen service. The examples include a complete 119-point cab route and illustrative flight/ferry paths with [data provenance](../samples/README.md).

## 4. Remove Trail's old camera forwarding

For the **native** Google adapter, remove the old `RouteOverlayView` from the layout and stop forwarding camera events to `mRouteOverlayView.onCameraMove()`. Do not delete camera callbacks that your app uses for other purposes.

There is no `setProjection` or `setCameraPosition` call on an effect. Trail emits native geographic polylines and, optionally, a centered flat vehicle marker. Google Maps transforms those objects with the camera. Your screen retains its camera, bounds-fitting policy, gestures, credentials and map attribution.

The separately named `GoogleMapsTrailOverlay` remains a screen-space tool for custom Canvas use. Its projection and UI clocks are distinct from the native map renderer. Choose `GoogleMapsTrail` for the anchored routes demonstrated in the README.

## 5. Move playback and cleanup to the binding

| Old responsibility | New responsibility |
| --- | --- |
| Stop/restart an overlay animator | `rememberTrailPlayback(effect, route.key)`, then `pause()`, `play()`, `replay()` or `seek(fraction)` |
| Remove an `OverlayPolyline` | Remove its `GoogleMapsTrail` from composition; the binding removes its own provider objects |
| Clear every route on the overlay | Stop composing the corresponding route bindings; do not clear the entire app-owned Google Map |
| Advance animation manually | Let Trail's lifecycle-aware binding drive one controller; use `active = false` for a retained offscreen map |
| Share an animator across surfaces | Share the immutable effect; give each visible binding its own playback controller |
| Wait for map tiles before starting | Pass your `onMapLoaded` readiness as `active` when this is your screen's policy |

Seeking pauses playback; `replay()` resets to the start and plays. Configuration and playback are separate: rebuilding a color preset is not an implicit request to restart the trip. Use `replay()` when restarting is the intended behavior. See [compiled playback examples](examples/Android.md#playback-controls).

For a cached, up-facing vehicle icon, pass `GoogleMapsTrailVehicle(icon = cabIcon)`. It follows the route head and rotates by geographic bearing. The app supplies its artwork once; the SDK does not bundle the sample fleet. [Vehicle integration](VEHICLES.md)

## 6. Replace extension hooks with public contracts

Implement `TrailLineStyle.draw(context)` to emit strokes and chevrons. Implement `TrailAnimation.sample(time)` to return deterministic visual state, and wrap it with `TrailAnimations.custom`. Use the same implementations in the DSL through `style(...)` and `animation(...)`. The [separate plugin module](../android/sample-plugin/src/main/kotlin/dev/trail/plugin/MetroStyle.kt) imports only public core APIs.

One layer accepts one style and one animation. Use `layer { }` for simultaneous drawing or `sequence { }` for ordered motion. Duplicate declarations are rejected instead of silently replacing one another. Keep map handles, directions calls, clocks and global registration out of plugins.

Legacy projected shadows and private `Paint`/animator manipulation are not mechanically preserved. Express line casing/glow with supported drawing commands, or keep provider-specific decoration in your app. See [native provider capabilities](MAPS.md#provider-extensions-and-capability-limits).

For loading directions, use `rememberTrailRouteTransition`, capture its request token, then resolve/fail/cancel that token. The loading arc morphs into the returned route, keeping every final corner; stale responses cannot replace a newer trip. [Complete loading example](examples/Android.md#loading-arc-to-directions-route)

## 7. Check the migrated screen

Run the old and new screen against the same captured route. Compare waypoint order, endpoints, density-scaled widths, date-line handling, pause/seek, background/resume, rapid camera gestures, route replacement and screen removal. Verify reduced motion and late/failed directions responses. [Migration regression tests](../android/playground/src/androidTest/kotlin/dev/trail/playground/MigrationTest.kt) cover preservation, snapshot ownership, revisions and explicit endpoint arcs.

Trail Studio uses `dev.trail.playground` as a local example app. There is no Play update or store-signing step in this SDK migration.

The independent [Swift/iOS SDK](https://github.com/amalChandran/trail-ios) uses Swift result builders and MapKit. It shares concepts and fixture contracts, not an Android runtime or a Java compatibility layer. Google Maps Android and Apple Maps iOS are the current native adapters; additional map providers require their own adapter.
