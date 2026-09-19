# Java Trail → Kotlin Trail 2

The original Java source remains under `legacy/android`. Version 2 is a new major API, so do not replace the dependency version and expect the old `TrailOverlayView.Builder` to compile.

1. Convert route points to `TrailCoordinate`, preserving every waypoint. Use `TrailRoute(id,points)` or decode the directions response with `encodedPolyline`.
2. Replace the builder with a named `trailEffect { stroke(...); reveal(...) }` value. Add explicit layers/sequences rather than repeated builder calls.
3. For Google Maps Compose, place `GoogleMapsTrail` inside `GoogleMap { }`. Remove the old screen overlay and camera-move projection forwarding for this native path.
4. Supply a controller only when the screen needs pause/replay/seek. A controller belongs to one binding.
5. Replace custom subclasses/global hooks with a `TrailLineStyle` or `TrailAnimation` implementation. Keep networking outside plugins.

The Play **app** keeps package `com.amalbit.animationongooglemap` and the existing upload signing identity. Its development install remains `dev.trail.playground`. The public SDK package imports are `dev.trail.*`; proposed Maven coordinates are `io.github.amalchandran:*`.
