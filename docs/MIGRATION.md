# Java Trail → Kotlin Trail 2

The original Java source remains under `legacy/android`. Version 2 is a new major API, so do not replace the dependency version and expect the old `TrailOverlayView.Builder` to compile.

1. Convert route points to `TrailCoordinate`, preserving every waypoint. Use `TrailRoute(id,points)` or decode the directions response with `encodedPolyline`.
2. Replace the builder with a named `trailEffect { stroke(...); reveal(...) }` value. Add explicit layers/sequences rather than repeated builder calls.
3. For Google Maps Compose, place `GoogleMapsTrail` inside `GoogleMap { }`. Remove the old screen overlay and camera-move projection forwarding for this native path.
4. Supply a controller only when the screen needs pause/replay/seek. A controller belongs to one binding.
5. Replace custom subclasses/global hooks with a `TrailLineStyle` or `TrailAnimation` implementation. Keep networking outside plugins.

Trail Studio is a local example app using `dev.trail.playground`, separate from the archived legacy app. No store update or store signing workflow is part of this SDK migration. The public SDK package imports are `dev.trail.*`; proposed Maven coordinates are `io.github.amalchandran:*`.
