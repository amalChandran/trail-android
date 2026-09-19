import Foundation

public enum TrailPlaybackStatus: Sendable { case playing, paused, finished }
/// Value-type deterministic player. UI ownership and frame scheduling belong to platform adapters.
public struct TrailPlayer: Sendable {
    public private(set) var effect: TrailEffect
    public private(set) var elapsedSeconds = 0.0
    public private(set) var status: TrailPlaybackStatus
    private var seekEndpoint = false
    public init(effect: TrailEffect = TrailEffect(), autoPlay: Bool = true) {
        self.effect = effect; self.status = autoPlay && effect.durationSeconds > 0 ? .playing : .paused
    }
    public var progress: Double {
        guard effect.durationSeconds > 0 else { return 1 }
        let cycles = elapsedSeconds / effect.durationSeconds
        return effect.repeats && !seekEndpoint ? cycles - floor(cycles) : min(1, max(0, cycles))
    }
    public mutating func play() {
        guard effect.durationSeconds > 0 else { return }
        seekEndpoint = false; if status == .finished { elapsedSeconds = 0 }; status = .playing
    }
    public mutating func pause() { if status == .playing { status = .paused } }
    public mutating func replay() { elapsedSeconds = 0; seekEndpoint = false; status = effect.durationSeconds > 0 ? .playing : .paused }
    public mutating func seek(to fraction: Double) { fractionCheck(fraction); seekEndpoint = fraction == 1; elapsedSeconds = fraction * effect.durationSeconds; status = .paused }
    public mutating func advance(by seconds: Double) {
        precondition(seconds.isFinite && seconds >= 0)
        guard status == .playing else { return }
        let next = elapsedSeconds + seconds; precondition(next.isFinite)
        elapsedSeconds = effect.repeats ? next : min(next, effect.durationSeconds)
        if !effect.repeats && elapsedSeconds >= effect.durationSeconds { status = .finished }
    }
    public mutating func configure(_ effect: TrailEffect, reset: Bool = false) {
        let normalized = self.effect.durationSeconds == 0 ? 0 : elapsedSeconds / self.effect.durationSeconds
        self.effect = effect
        if reset { seekEndpoint = false }
        elapsedSeconds = reset ? 0 : normalized * effect.durationSeconds
        if effect.durationSeconds == 0 { status = .paused } else if reset { status = .playing }
    }
    public func frame(layer: Int, reducedMotion: Bool = false) -> TrailVisualState {
        if !reducedMotion, seekEndpoint, let spec = effect.layers[layer].animation, elapsedSeconds == spec.durationSeconds {
            return spec.sampler.sample(at: TrailTime(progress: 1, elapsedSeconds: spec.durationSeconds))
        }
        return effect.sample(layer: layer, elapsed: elapsedSeconds, reducedMotion: reducedMotion)
    }
}
