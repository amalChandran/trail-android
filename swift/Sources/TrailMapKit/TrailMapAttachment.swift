#if canImport(UIKit)
import UIKit
import MapKit
import SwiftUI
import TrailCore
import TrailUI

/// Explicit attachment for existing MKMapView screens. Retain this value and call updateProjection()
/// from your existing mapViewDidChangeVisibleRegion delegate and after layout. Never replaces that delegate.
@MainActor public final class TrailMapAttachment {
    private weak var mapView: MKMapView?
    private var host: UIHostingController<AttachmentContent>?
    private let model: AttachmentModel
    public init(mapView: MKMapView, parent: UIViewController, route: TrailRoute, playback: TrailPlayback) throws {
        if zip(route.coordinates, route.coordinates.dropFirst()).contains(where: { abs($0.longitude - $1.longitude) > 180 }) {
            throw TrailError.invalidRoute("Split antimeridian routes before attaching in this alpha")
        }
        self.mapView = mapView
        model = AttachmentModel(route: route, playback: playback)
        let controller = UIHostingController(rootView: AttachmentContent(model: model))
        parent.addChild(controller)
        controller.view.backgroundColor = .clear; controller.view.isUserInteractionEnabled = false
        controller.view.frame = mapView.bounds; controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mapView.addSubview(controller.view); controller.didMove(toParent: parent)
        self.host = controller
        updateProjection()
    }
    public func updateProjection() {
        guard let mapView, let host else { return }
        host.view.frame = mapView.bounds
        model.path = model.route.project {
            let point = mapView.convert(CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude), toPointTo: host.view)
            return TrailPoint(point.x, point.y)
        }
    }
    /// Removes the view and its frame driver. Call from the owning screen's teardown.
    public func detach() {
        host?.willMove(toParent: nil); host?.view.removeFromSuperview(); host?.removeFromParent(); host = nil
    }
}
@MainActor @Observable private final class AttachmentModel {
    let route: TrailRoute
    let playback: TrailPlayback
    var path = TrailPath([])
    init(route: TrailRoute, playback: TrailPlayback) { self.route = route; self.playback = playback }
}
@MainActor private struct AttachmentContent: View {
    let model: AttachmentModel
    var body: some View { TrailCanvas(path: model.path, playback: model.playback, fit: false) }
}
#endif
