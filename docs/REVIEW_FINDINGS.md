# Code review findings

Static review of the Android modules (`trail-core`, `trail-effects`, `trail-android`, `trail-compose`, `trail-google-maps`), the Gradle files and the CI workflow. Date: 2026-10-08.

**Limits of this review:** nothing was built or run. The Gradle build could not fetch the Android plugin in the review sandbox, so every finding below comes from reading the code and is unconfirmed by tests. Not covered: `scripts/`, `legacy/`, playground tests, iOS docs.

Status legend: `open` = not yet addressed.

## Likely bugs

### 1. Morphed routes never reach the overlay (open)
- `TrailRouteTransition.routeAt()` builds each frame as `TrailRoute(to.id, …, to.revision)` (`trail-core/.../RouteTransition.kt:32`), so every morph frame has the same `TrailRouteKey` with different geometry.
- `TrailProjectedOverlay` caches the projected path on `route.key` (`trail-compose/.../TrailProjectedOverlay.kt:27`).
- Effect: feeding `transition.route` into `GoogleMapsTrailOverlay` / `TrailProjectedOverlay` projects the first morph frame, then freezes. The final resolved route is never projected either.
- Native `GoogleMapsTrail` caches on the route object and works; docs only show that path.
- Fix: key on the route instance, or give each morph frame a distinct revision.

### 2. Dotted lines on Google Maps probably jump at loop wrap (open, verify on device)
- `nativeDashPattern` replaces any dash ≤ 0.01 with `Dot()` (`trail-google-maps/.../GoogleMapsTrail.kt:73`).
- A Maps `Dot` is as long as the stroke width, but the phase maths treats it as 0.01 long. Real period ≈ `gap + width`; the animation advances by `gap` per loop.
- Effect: `MovingDots` visibly jumps each cycle on Maps only.

## Smaller correctness issues

### 3. `seek(1.0)` then `play()` does not restart (open)
`TrailPlayer.seek` leaves status `Paused`, so `play()` skips the `Finished` reset (`trail-core/.../Playback.kt:16`). The clip resumes at the end and finishes immediately, unlike replay after a natural finish.

### 4. Polar endpoints throw (open)
`TrailRouteTransition(from, to)` and `begin()` build a Mercator arc that requires latitude within ±85.05° (`trail-core/.../Geography.kt:31`). Any polar coordinate throws from the constructor.

### 5. `TrailCanvas(path) { … }` rebuilds its effect every recomposition (open)
`trailEffect(content)` is not wrapped in `remember` (`trail-compose/.../TrailCanvas.kt:143`). It reruns plugin style code and calls `playback.configure` each time.

### 6. Playback restarts when the path object changes (open)
Default playback in `TrailCanvas` is keyed on `path` identity (`TrailCanvas.kt:98`). A caller building `TrailPath` inline restarts the animation on every recomposition.

### 7. View and Compose adapters fit paths differently (open)
`TrailView` uses default padding 16 (`trail-android/.../TrailView.kt:41`); `TrailCanvas` uses 24 (`TrailCanvas.kt:109`). The same path lands in different places.

### 8. `TrailRouteTransitionState.resolve` has no `reducedMotion` argument (open)
(`TrailRouteTransitionState.kt:18`). The core supports it. Reduced motion only works via the clock coroutine, so it does nothing when `active` is false or the lifecycle is not STARTED.

### 9. `GoogleMapsTrailOverlay` has no `active` parameter (open)
Unlike `GoogleMapsTrail`, a retained offscreen overlay cannot be paused.

## Performance

### 10. Every camera move rebuilds the renderer (open)
The overlay re-projects the whole route, then `TrailCanvas` builds a new `TrailRenderer` with fresh `Path`/`PathMeasure` objects (`trail-android/.../TrailRenderer.kt:10`). Routes near 100k points will drop frames during gestures.

### 11. `TrailView` reads `Settings.Global` every frame (open)
`motionReduced()` is called per layer in `onDraw` and again in `schedule()` (`TrailView.kt:38`, `:48`). Cache it and update from the existing `ContentObserver`.

### 12. Dash cache barely caches (open)
One entry per stroke keyed on phase, so several windows/contours per frame recreate `DashPathEffect` every draw. The `size > 512` clear can never trigger (`TrailRenderer.kt:19`, `:51`).

### 13. Maps can create thousands of `Polyline`s per frame (open)
Up to 8192 primitives are allowed (`trail-core/.../MapGeometry.kt:37`). `Gradient` and `Tapered` use 64 strokes each; comets add windows. `TrailVisualState` has no `equals`, so the `remember` at `GoogleMapsTrail.kt:40` misses on every animated frame.

## Minor

- `TrailView` has no `onMeasure` (`wrap_content` gives 0×0) and `onSizeChanged` does not call `super`.
- Adapter modules have no JVM unit tests; CI only compiles them via the playground. `nativeDashPattern` is `internal` and easy to test (a test would have caught #2).
- `size-probe` is not built in CI.
- `data class TrailStroke internal constructor(...)` still exposes a public `copy()` on older Kotlin versions, bypassing validation.

## Checked and fine

- No committed secrets: `legacy/` Maps key files hold placeholders; `local.properties` is gitignored.
- Playground release signs with the debug key, which is commented as deliberate.
- CI trigger branch (`master`) matches the repo default.
- Preset opacity maths (comet, pulse) stays within [0, 1]; polyline decoder bounds are sound.

## Suggested order

1. #1 (overlay + morph), 2. #2 (Maps dots), then #3–#9 as small fixes, then performance items #10–#13 with measurements.
