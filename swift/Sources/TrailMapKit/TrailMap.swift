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
    public let playback: TrailPlayback
    public let reducedMotion: Bool
    @State private var projected = TrailPath([])
    @State private var ready = false
    public init(route: TrailRoute, map: MapProxy, cameraRevision: Int, playback: TrailPlayback, reducedMotion: Bool = false) {
        self.route = route; self.map = map; self.cameraRevision = cameraRevision; self.playback = playback; self.reducedMotion = reducedMotion
    }
    public var body: some View {
        let crossesDateLine = zip(route.coordinates, route.coordinates.dropFirst()).contains { abs($0.longitude - $1.longitude) > 180 }
        TrailCanvas(path: projected, playback: playback, fit: false, reducedMotion: reducedMotion, active: ready)
            .allowsHitTesting(false)
            .onChange(of: "\(route.id):\(route.revision):\(cameraRevision)", initial: true) { _, _ in updateProjection() }
            .onGeometryChange(for: CGSize.self) { $0.size } action: { _ in updateProjection() }
            .overlay(alignment: .bottom) {
                if crossesDateLine { Text("Split antimeridian routes before rendering with this alpha.").font(.caption).padding().background(.regularMaterial) }
            }
    }
    private func updateProjection() {
        let points = route.coordinates.compactMap { coordinate -> TrailPoint? in
            guard let point = map.convert(CLLocationCoordinate2D(latitude: coordinate.latitude, longitude: coordinate.longitude), to: .local), point.x.isFinite, point.y.isFinite else { return nil }
            return TrailPoint(point.x, point.y)
        }
        let crosses = zip(route.coordinates, route.coordinates.dropFirst()).contains { abs($0.longitude - $1.longitude) > 180 }
        ready = points.count == route.coordinates.count && !crosses
        projected = TrailPath(ready ? points : [])
    }
}

/// MapKit convenience host; use TrailMapOverlay when your application already owns a SwiftUI Map.
@MainActor public struct TrailMap: View {
    private let route: TrailRoute
    private let playback: TrailPlayback
    private let reducedMotion: Bool
    @State private var position: MapCameraPosition
    @State private var cameraRevision = 0
    public init(route: TrailRoute, playback: TrailPlayback, reducedMotion: Bool = false) {
        self.route = route; self.playback = playback; self.reducedMotion = reducedMotion
        _position = State(initialValue: .region(Self.region(for: route)))
    }
    public var body: some View {
        MapReader { proxy in
            Map(position: $position)
                .mapStyle(.standard(elevation: .flat))
                .onMapCameraChange(frequency: .continuous) { _ in cameraRevision &+= 1 }
                .overlay { TrailMapOverlay(route: route, map: proxy, cameraRevision: cameraRevision, playback: playback, reducedMotion: reducedMotion) }
        }
        .onChange(of: route.id + ":" + String(route.revision)) { _, _ in position = .region(Self.region(for: route)); playback.replay() }
    }
    private static func region(for route: TrailRoute) -> MKCoordinateRegion {
        let lats = route.coordinates.map(\.latitude), lons = route.coordinates.map(\.longitude)
        let minLat = lats.min() ?? 37.78, maxLat = lats.max() ?? 37.79
        let minLon = lons.min() ?? -122.42, maxLon = lons.max() ?? -122.41
        return MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: (minLat + maxLat) / 2, longitude: (minLon + maxLon) / 2),
                                  span: MKCoordinateSpan(latitudeDelta: max(0.01, (maxLat - minLat) * 1.5), longitudeDelta: max(0.01, (maxLon - minLon) * 1.5)))
    }
}
