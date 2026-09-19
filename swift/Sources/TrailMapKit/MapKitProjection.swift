#if canImport(UIKit)
import UIKit
import MapKit
import TrailCore

public extension TrailProjection {
    /// Projects into the overlay's local point coordinate space. Does not retain either view.
    /// Call again after layout/camera changes. A detached or zero-size map is not ready.
    @MainActor static func appleMaps(_ mapView: MKMapView, in overlay: UIView) -> TrailProjection {
        TrailProjection { [weak mapView, weak overlay] coordinate in
            guard let mapView, let overlay, mapView.window != nil,
                  mapView.bounds.width > 0, mapView.bounds.height > 0 else { return nil }
            let longitude = max(-180 + 1e-7, min(180 - 1e-7, coordinate.longitude))
            let point = mapView.convert(CLLocationCoordinate2D(latitude: coordinate.latitude, longitude: longitude), toPointTo: overlay)
            guard point.x.isFinite, point.y.isFinite else { return nil }
            return TrailPoint(point.x, point.y)
        }
    }
}
#endif
