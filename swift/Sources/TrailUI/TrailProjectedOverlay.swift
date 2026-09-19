import SwiftUI
import TrailCore

/// Provider-neutral map binding. Return local points, with exactly the map's bounds.
/// Increment projectionRevision after camera/inset/projection changes; layout is observed here.
/// A nil projection stops the display driver without losing playback. Never consumes gestures.
@MainActor public struct TrailProjectedOverlay: View {
    private let route: TrailRoute
    private let projection: TrailProjection?
    private let projectionRevision: Int
    private let externalPlayback: TrailPlayback?
    private let suppliedEffect: TrailEffect?
    private let reducedMotion: Bool
    private let onProjected: (TrailPath?) -> Void
    @State private var ownedPlayback: TrailPlayback
    @State private var projected = TrailPath([])
    @State private var ready = false
    @State private var viewport = CGSize.zero
    private var playback: TrailPlayback { externalPlayback ?? ownedPlayback }
    private struct ProjectionKey: Equatable {
        let route: TrailRouteKey
        let revision: Int
        let available: Bool
    }

    public init(route: TrailRoute, projection: TrailProjection?, projectionRevision: Int,
                playback: TrailPlayback, reducedMotion: Bool = false,
                onProjected: @escaping (TrailPath?) -> Void = { _ in }) {
        self.route = route; self.projection = projection; self.projectionRevision = projectionRevision
        self.externalPlayback = playback; suppliedEffect = nil; self.reducedMotion = reducedMotion
        self.onProjected = onProjected; _ownedPlayback = State(initialValue: playback)
    }
    public init(route: TrailRoute, projection: TrailProjection?, projectionRevision: Int,
                effect: TrailEffect = TrailEffect(), reducedMotion: Bool = false,
                onProjected: @escaping (TrailPath?) -> Void = { _ in }) {
        self.route = route; self.projection = projection; self.projectionRevision = projectionRevision
        externalPlayback = nil; suppliedEffect = effect; self.reducedMotion = reducedMotion
        self.onProjected = onProjected; _ownedPlayback = State(initialValue: TrailPlayback(effect: effect))
    }
    public var body: some View {
        TrailCanvas(path: projected, playback: playback, fit: false, reducedMotion: reducedMotion, active: ready)
            .clipped()
            .allowsHitTesting(false)
            .onChange(of: ProjectionKey(route: route.key, revision: projectionRevision, available: projection != nil), initial: true) { _, _ in updateProjection() }
            .onGeometryChange(for: CGSize.self) { $0.size } action: { viewport = $0; updateProjection() }
            .onChange(of: route.key) { _, _ in if externalPlayback == nil { playback.replay() } }
            .onChange(of: suppliedEffect?.id) { _, _ in if let suppliedEffect { playback.configure(suppliedEffect) } }
    }
    private func updateProjection() {
        let path = viewport.width > 0 && viewport.height > 0 ? projection.flatMap { route.projectIfReady($0) } : nil
        ready = path != nil; projected = path ?? TrailPath([])
        onProjected(path)
    }
}
