# Vehicles that follow the route

Trail supplies position and heading; your app supplies cached, up-facing artwork. The demo has original top-down car, aircraft and ferry vectors, with glazing, body details and a light contact shadow. Artwork is sample-only and adds no asset dependency to the SDK.

## Native map placement

For Google Maps, pass `GoogleMapsTrailVehicle(icon = cachedBitmapDescriptor)` to `GoogleMapsTrail` inside `GoogleMap { }`. Trail creates a centered, flat native marker and rotates it with the route's north-relative bearing. The SDK moves the marker and route in the same map render pipeline during camera gestures.

For Apple Maps, put a SwiftUI `Annotation` inside `Map { }`, using the coordinate from `TrailMapGeometry.pose(at:)`. The journey example caches its `UIImage` and applies `pose.bearing - cameraHeading`. SwiftUI annotations retain MapKit's billboard presentation; they do not gain Google's flat-marker perspective behavior.

`TrailMapGeometry.poseAt(fraction, headingWindow, direction)` in Kotlin and `pose(at:headingWindow:direction:)` in Swift return an exact route position and a bearing in degrees clockwise from north. Distance and heading windows use Mercator meters. Zero heading window uses the local segment tangent; the default is two percent of prepared path length. A short chord smooths **only the heading**: position retains every street corner. Date-line seams are unwrapped before evaluation.

Use the animation frame's `head` when present, or playback progress otherwise; pass `headDirection` for reverse movement. Sampling is stateless, so seeking, frame cadence and pause do not accumulate drift. There is no independent marker tween or second timer.

The demo takes 18 seconds for a car, 16 for an aircraft and 20 for a ferry. Quintic easing gives each a gentle departure and arrival. The line and vehicle share the same eased progress. Reduced motion displays a stationary arrival state. These are illustrative journeys, not a GPS interpolation or navigation SDK.

## Canvas decorations

For an ordinary Canvas or an explicitly chosen screen overlay, use `TrailPath.poseAt` / `pose(at:)`. That API returns a local point and radians clockwise from +x; up-facing artwork needs an extra π/2. Its heading window uses local logical units (Android dp / Apple points). Do not mix those units or rotation conventions with native map poses.

Screen overlays have separate camera-projection and drawing clocks; use native geographic content for tightly anchored map routes. See [map integration and provider limits](MAPS.md).

## Artwork and verification

`scripts/generate-vehicle-artwork.py` owns the original vector geometry and emits `samples/vehicles.json`. Each repository synchronizes it into its own example app. The three vectors total 10,521 bytes of JSON; the SDK packages none of them. Cache raster icons instead of decoding and rasterizing on every animation frame.

Each language runs 176 local-path analytic pose cases and 144 native geographic pose cases. Native artwork probes cover three vehicles × four headings × two densities. Android also checks actual native SDK route/marker pixels across 12 camera configurations; iOS exercises loading, seeking and rapid alternating map gestures. [Executed verification](VERIFICATION.md)
