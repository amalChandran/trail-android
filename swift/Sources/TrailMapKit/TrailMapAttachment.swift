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
    public init(mapView: MKMapView, parent: UIViewController, route: TrailRoute, playback: TrailPlayback) {
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
        let path = model.route.projectIfReady(.appleMaps(mapView, in: host.view))
        model.path = path ?? TrailPath([]); model.ready = path != nil
    }
    public var isAttached: Bool { host != nil }
    public func updateRoute(_ route: TrailRoute, replay: Bool = false) {
        guard host != nil else { return }
        let changed = model.route.key != route.key
        model.route = route
        if changed && replay { model.playback.replay() }
        updateProjection()
    }
    /// Removes the view and its frame driver. Call from the owning screen's teardown.
    public func detach() {
        host?.willMove(toParent: nil); host?.view.removeFromSuperview(); host?.removeFromParent(); host = nil
    }
}
@MainActor @Observable private final class AttachmentModel {
    var route: TrailRoute
    let playback: TrailPlayback
    var path = TrailPath([])
    var ready = false
    init(route: TrailRoute, playback: TrailPlayback) { self.route = route; self.playback = playback }
}
@MainActor private struct AttachmentContent: View {
    let model: AttachmentModel
    var body: some View { TrailCanvas(path: model.path, playback: model.playback, fit: false, active: model.ready) }
}
#endif
