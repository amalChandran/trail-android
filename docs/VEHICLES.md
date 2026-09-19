# Vehicles that follow the route

The Google Maps and Apple Maps journeys use original top-down car, aircraft and ferry vectors. Each has glazing, body details and a light contact shadow; the ferry also has a small wake. There is no emoji or circular badge on the moving map marker. Artwork is prepared once and drawn with translation and rotation, without a second animation timer.

The car takes 18 seconds, aircraft 16 seconds and ferry 20 seconds per demo loop. A quintic easing curve gives each trip a gentle departure and arrival. The same eased time reaches the selected line animation and its vehicle. Pause, scrub, replay and background suspension therefore remain synchronized. Reduced motion displays a stationary vehicle at the destination. These are illustrative journeys, not a GPS interpolation or navigation SDK.

## Use your own artwork

The reusable part lives in **TrailCore**, independently of map providers and UI frameworks:

```kotlin
val frame = playback.frame(layer = 1)
val pose = projectedPath.poseAt(
    fraction = frame.head ?: playback.progress,
    headingWindow = 16.0,
    direction = frame.headDirection,
)
// Draw your marker at pose.point; rotate it by pose.headingRadians.
```

```swift
let frame = playback.frame(layer: 1)
let pose = projectedPath.pose(
    at: frame.head ?? playback.progress,
    headingWindow: 16,
    direction: frame.headDirection
)
// Draw your marker at pose.point; rotate it by pose.headingRadians.
```

`poseAt` / `pose(at:)` returns the **exact distance-based position** on the route. It smooths only the heading, using a chord across a short distance window. A car stays on every supplied street segment; smoothing does not cut through a corner. The window is clipped to the current continuous contour, so date-line splits never contribute a false diagonal direction.

- Heading is radians clockwise from local +x in the map overlay's y-down coordinate system. Up-facing artwork needs **+π/2** when drawn. The sample renderers apply that offset.
- `headingWindow` is in Android dp / Apple points. Zero uses the local segment tangent. The samples use 14 for cars, 24 for aircraft and 30 for ferries.
- `headDirection` lets an animation/plugin identify forward or reverse travel. Ping pong reports reverse travel on its return leg. Effects that finish drawing and then animate the line keep their head at the destination.
- Empty paths return no pose. Stationary paths return their position with a stable heading. Exact folded-back segments use their local tangent when the smoothing chord has no direction.
- Sampling is stateless. Seeking in a different order, camera rotation, pause or frame cadence cannot accumulate steering drift. Heading is applied as a transform, without a tween that might take the long way around ±180°.
- `contourIndex` identifies discontinuities for applications that need their own visibility policy. A discontinuity changes position immediately; it is never animated as a connection across the map.

Use the projected path from the active adapter. Switching Google Maps for another provider changes the projection, while this pose API and the artwork renderer stay the same. Draw the vehicle in the map's clipped overlay, with matching bounds, and leave hit testing to the map.

## Artwork and verification

`scripts/generate-vehicle-artwork.py` owns the original vector geometry. It generates `samples/vehicles.json`; `scripts/sync-fixtures.py` copies identical data into both sample apps. The three vectors total 10,521 bytes of JSON, stored as a 3,314-byte compressed entry in the current debug APK. This measures that asset only, not total SDK overhead. The artwork and its loader belong to the playground apps; consumers of TrailCore do not acquire these assets or a rendering dependency.

The shared contracts include 176 analytic pose cases: eight orientations, both travel directions, corner approach/apex/exit, seam boundaries, repeated/empty paths and zero heading windows. Each platform also runs 24 native artwork raster cases: three vehicles × four headings × two densities, with front-facing color probes to catch wrong pivots or axis offsets. Smooth-corner and animation-head regressions cover continuity, exact route positions, reverse motion and arrival state. See [VERIFICATION.md](VERIFICATION.md) for executed results.
