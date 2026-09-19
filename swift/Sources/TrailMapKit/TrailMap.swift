import SwiftUI
import MapKit
import TrailCore
import TrailUI

/// Explicit overlay for an existing SwiftUI Map inside MapReader. Increment cameraRevision in
/// onMapCameraChange(frequency: .continuous). Bounds must match the map. Does not intercept gestures.
@MainActor public struct TrailMapOverlay: View {
    public let route: TrailRoute
    public let map: MapProxy
    public let cameraRevision: Int
    private let externalPlayback: TrailPlayback?
    private let suppliedEffect: TrailEffect?
    @State private var ownedPlayback: TrailPlayback
    private var playback: TrailPlayback { externalPlayback ?? ownedPlayback }
    public let reducedMotion: Bool
    private var onProjected: (TrailPath?) -> Void = { _ in }
    public init(route: TrailRoute, map: MapProxy, cameraRevision: Int, playback: TrailPlayback, reducedMotion: Bool = false, onProjected: @escaping (TrailPath?) -> Void = { _ in }) {
        self.route = route; self.map = map; self.cameraRevision = cameraRevision; self.reducedMotion = reducedMotion
        externalPlayback = playback; suppliedEffect = nil; _ownedPlayback = State(initialValue: playback)
        self.onProjected = onProjected
    }
    public init(route: TrailRoute, map: MapProxy, cameraRevision: Int, effect: TrailEffect = TrailEffect(), reducedMotion: Bool = false) {
        self.route = route; self.map = map; self.cameraRevision = cameraRevision; self.reducedMotion = reducedMotion
        externalPlayback = nil; suppliedEffect = effect; _ownedPlayback = State(initialValue: TrailPlayback(effect: effect))
    }
    public var body: some View {
        TrailProjectedOverlay(route: route, projection: TrailProjection { coordinate in
            let longitude = max(-180 + 1e-7, min(180 - 1e-7, coordinate.longitude))
            guard let point = map.convert(CLLocationCoordinate2D(latitude: coordinate.latitude, longitude: longitude), to: .local), point.x.isFinite, point.y.isFinite else { return nil }
            return TrailPoint(point.x, point.y)
        }, projectionRevision: cameraRevision, playback: playback, reducedMotion: reducedMotion, onProjected: onProjected)
            .onChange(of: route.key) { _, _ in if externalPlayback == nil { playback.replay() } }
            .onChange(of: suppliedEffect?.id) { _, _ in if let suppliedEffect { playback.configure(suppliedEffect) } }
    }
}

/// MapKit convenience host; use TrailMapOverlay when your application already owns a SwiftUI Map.
@MainActor public struct TrailMap: View {
    private let route: TrailRoute
    private let externalPlayback: TrailPlayback?
    private let suppliedEffect: TrailEffect?
    @State private var ownedPlayback: TrailPlayback
    private var playback: TrailPlayback { externalPlayback ?? ownedPlayback }
    private let reducedMotion: Bool
    @State private var position: MapCameraPosition
    @State private var cameraRevision = 0
    public init(route: TrailRoute, playback: TrailPlayback, reducedMotion: Bool = false) {
        self.route = route; self.reducedMotion = reducedMotion
        externalPlayback = playback; suppliedEffect = nil; _ownedPlayback = State(initialValue: playback)
        _position = State(initialValue: .region(Self.region(for: route)))
    }
    public init(route: TrailRoute, effect: TrailEffect = TrailEffect(), reducedMotion: Bool = false) {
        self.route = route; self.reducedMotion = reducedMotion
        externalPlayback = nil; suppliedEffect = effect; _ownedPlayback = State(initialValue: TrailPlayback(effect: effect))
        _position = State(initialValue: .region(Self.region(for: route)))
    }
    public init(route: TrailRoute, @TrailEffectBuilder effect: () -> [TrailEffectPart]) { self.init(route: route, effect: TrailEffect(effect)) }
    public var body: some View {
        MapReader { proxy in
            Map(position: $position)
                .mapStyle(.standard(elevation: .flat))
                .onMapCameraChange(frequency: .continuous) { _ in cameraRevision &+= 1 }
                .overlay { TrailMapOverlay(route: route, map: proxy, cameraRevision: cameraRevision, playback: playback, reducedMotion: reducedMotion) }
        }
        .onChange(of: route.key) { _, _ in
            position = .region(Self.region(for: route))
            if externalPlayback == nil { playback.replay() }
        }
        .onChange(of: suppliedEffect?.id) { _, _ in if let suppliedEffect { playback.configure(suppliedEffect) } }
    }
    private static func region(for route: TrailRoute) -> MKCoordinateRegion {
        guard let bounds = route.bounds else { return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 0, longitude: 0), span: MKCoordinateSpan(latitudeDelta: 60, longitudeDelta: 60)) }
        return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: bounds.center.latitude, longitude: bounds.center.longitude),
                                  span: MKCoordinateSpan(latitudeDelta: min(170,max(0.01,bounds.latitudeSpan*1.5)), longitudeDelta: min(359,max(0.01,bounds.longitudeSpan*1.5))))
    }
}
