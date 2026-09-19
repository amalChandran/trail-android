import SwiftUI
import TrailCore

@MainActor @Observable public final class TrailPlayback {
    public private(set) var player: TrailPlayer
    public var progress: Double { player.progress }
    public var isPlaying: Bool { player.status == .playing }
    public init(effect: TrailEffect = TrailEffect(), autoPlay: Bool = true) { player = TrailPlayer(effect: effect, autoPlay: autoPlay) }
    public func play() { player.play() }
    public func pause() { player.pause() }
    public func replay() { player.replay() }
    public func seek(to fraction: Double) { player.seek(to: fraction) }
    public func configure(_ effect: TrailEffect, reset: Bool = false) { player.configure(effect, reset: reset) }
    public func advance(by seconds: Double) { player.advance(by: seconds) }
}

/// Attach one display driver to each controller. A weak target avoids display-link retain cycles.
@MainActor public struct TrailCanvas: View {
    private let path: TrailPath
    private let fit: Bool
    private let forceReducedMotion: Bool
    private let active: Bool
    @State private var ownedPlayback: TrailPlayback
    @State private var geometryCache = FittedPathCache()
    @State private var visible = true
    private let externalPlayback: TrailPlayback?
    private let suppliedEffect: TrailEffect?
    @Environment(\.accessibilityReduceMotion) private var systemReducedMotion
    @Environment(\.scenePhase) private var phase
    private var playback: TrailPlayback { externalPlayback ?? ownedPlayback }

    public init(path: TrailPath, playback: TrailPlayback, fit: Bool = true, reducedMotion: Bool = false, active: Bool = true) {
        self.path = path; self.fit = fit; self.forceReducedMotion = reducedMotion; self.active = active
        self.externalPlayback = playback; self.suppliedEffect = nil; _ownedPlayback = State(initialValue: playback)
    }
    public init(path: TrailPath, effect: TrailEffect = TrailEffect(), fit: Bool = true, reducedMotion: Bool = false) {
        self.path = path; self.fit = fit; self.forceReducedMotion = reducedMotion; self.active = true
        externalPlayback = nil; suppliedEffect = effect; _ownedPlayback = State(initialValue: TrailPlayback(effect: effect))
    }
    public init(path: TrailPath, @TrailEffectBuilder effect: () -> [TrailEffectPart]) { self.init(path: path, effect: TrailEffect(effect)) }

    public var body: some View {
        let reduced = forceReducedMotion || systemReducedMotion
        let player = playback.player
        #if canImport(UIKit)
        let screenBounds = UIScreen.main.bounds
        #endif
        GeometryReader { geometry in
            let measured = geometryCache.resolve(path, size: geometry.size, fit: fit)
            let animating = active && phase == .active && !reduced && playback.isPlaying && measured.length > 0
            Canvas { context, _ in
                context.withCGContext { cg in TrailRenderer.draw(path: measured, effect: player.effect, in: cg) { player.frame(layer: $0, reducedMotion: reduced) } }
            }
            .allowsHitTesting(false)
            #if canImport(UIKit)
            .background(TrailDisplayDriver(playback: playback, active: animating && visible))
            .onGeometryChange(for: Bool.self) { proxy in
                let frame = proxy.frame(in: .global)
                return frame.width > 0 && frame.height > 0 && frame.intersects(screenBounds)
            } action: { visible = $0 }
            #else
            .task(id: animating) {
                guard animating else { return }
                let clock = ContinuousClock(); var previous = clock.now
                while !Task.isCancelled && playback.isPlaying {
                    do { try await Task.sleep(for: .milliseconds(16)) } catch { return }
                    let now = clock.now; let delta = previous.duration(to: now); previous = now
                    playback.advance(by: Double(delta.components.seconds) + Double(delta.components.attoseconds) / 1e18)
                }
            }
            #endif
        }
        .onChange(of: suppliedEffect?.id) { _, _ in if let suppliedEffect { playback.configure(suppliedEffect) } }
        .onChange(of: path.identity) { _, _ in if externalPlayback == nil { playback.replay() } }
    }
}

@MainActor private final class FittedPathCache {
    private var identity: UUID?
    private var size = CGSize.zero
    private var measured = TrailPath([])
    func resolve(_ path: TrailPath, size: CGSize, fit: Bool) -> TrailPath {
        guard fit else { return path }
        if identity != path.identity || self.size != size {
            identity = path.identity; self.size = size
            measured = path.fitted(width: size.width, height: size.height, padding: 24)
        }
        return measured
    }
}

#if canImport(UIKit)
import UIKit

private struct TrailDisplayDriver: UIViewRepresentable {
    let playback: TrailPlayback
    let active: Bool
    func makeUIView(context: Context) -> DriverView { DriverView() }
    func updateUIView(_ view: DriverView, context: Context) { view.playback = playback; view.active = active; view.updateClock() }
    static func dismantleUIView(_ view: DriverView, coordinator: ()) { view.stop() }
}
@MainActor private final class DriverView: UIView {
    weak var playback: TrailPlayback?
    var active = false
    private var link: CADisplayLink?
    private var previous: CFTimeInterval?
    private lazy var target = WeakFrameTarget(view: self)
    override func didMoveToWindow() { super.didMoveToWindow(); updateClock() }
    func updateClock() {
        guard active && window != nil else { stop(); return }
        if link == nil {
            let display = CADisplayLink(target: target, selector: #selector(WeakFrameTarget.tick(_:)))
            display.add(to: .main, forMode: .common); link = display
        }
    }
    func stop() { link?.invalidate(); link = nil; previous = nil }
    func tick(_ display: CADisplayLink) {
        guard active, window != nil, let playback, playback.isPlaying else { stop(); return }
        if let previous { playback.advance(by: max(0, display.timestamp - previous)) }
        previous = display.timestamp
    }
}
@MainActor private final class WeakFrameTarget: NSObject {
    weak var view: DriverView?
    init(view: DriverView) { self.view = view }
    @objc func tick(_ display: CADisplayLink) { guard let view else { display.invalidate(); return }; view.tick(display) }
}
#endif
