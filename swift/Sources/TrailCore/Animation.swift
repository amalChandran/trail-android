import Foundation

public struct TrailWindow: Sendable, Equatable {
    public let start: Double
    public let end: Double
    public let opacity: Double
    public init(_ start: Double, _ end: Double, opacity: Double = 1) {
        fractionCheck(start); fractionCheck(end); fractionCheck(opacity); precondition(start <= end)
        self.start = start; self.end = end; self.opacity = opacity
    }
}

public struct TrailVisualState: Sendable, Equatable {
    public let windows: [TrailWindow]
    public let opacity: Double
    public let widthScale: Double
    public let dashPhase: Double
    public let head: Double?
    public let headDirection: TrailDirection
    public init(windows: [TrailWindow] = [TrailWindow(0, 1)], opacity: Double = 1, widthScale: Double = 1, dashPhase: Double = 0, head: Double? = nil, headDirection: TrailDirection = .forward) {
        precondition(windows.count <= 256); fractionCheck(opacity); fractionCheck(dashPhase)
        precondition(widthScale.isFinite && (0...16).contains(widthScale))
        if let head { fractionCheck(head) }
        self.windows = windows; self.opacity = opacity; self.widthScale = widthScale; self.dashPhase = dashPhase; self.head = head
        self.headDirection = headDirection
    }
    public static let full = TrailVisualState()
    public static let hidden = TrailVisualState(windows: [])
    public static func reveal(to fraction: Double, direction: TrailDirection = .forward) -> Self {
        Self(windows: [TrailWindow(0, fraction)], head: fraction, headDirection: direction)
    }
}

public struct TrailTime: Sendable {
    public let progress: Double
    public let cycle: Int
    public let elapsedSeconds: Double
    public init(progress: Double, cycle: Int = 0, elapsedSeconds: Double = 0) {
        fractionCheck(progress); precondition(cycle >= 0 && elapsedSeconds.isFinite && elapsedSeconds >= 0)
        self.progress = progress; self.cycle = cycle; self.elapsedSeconds = elapsedSeconds
    }
}

public protocol TrailAnimation: Sendable { func sample(at time: TrailTime) -> TrailVisualState }
public struct TrailSampler: TrailAnimation {
    private let body: @Sendable (TrailTime) -> TrailVisualState
    public init(_ body: @escaping @Sendable (TrailTime) -> TrailVisualState) { self.body = body }
    public func sample(at time: TrailTime) -> TrailVisualState { body(time) }
}

public struct TrailAnimationSpec: Sendable {
    public let sampler: any TrailAnimation
    public let durationSeconds: Double
    public let repeats: Bool
    public let reducedMotion: TrailVisualState
    public init(sampler: any TrailAnimation, duration: Duration = .seconds(2), repeats: Bool = false, reducedMotion: TrailVisualState = .full) {
        let seconds = Double(duration.components.seconds) + Double(duration.components.attoseconds) / 1e18
        precondition(seconds.isFinite && seconds > 0)
        self.sampler = sampler; self.durationSeconds = seconds; self.repeats = repeats; self.reducedMotion = reducedMotion
    }
}

public enum TrailAnimations {
    public static func custom(_ sampler: any TrailAnimation, duration: Duration = .seconds(2), repeats: Bool = false, reducedMotion: TrailVisualState = .full) -> TrailAnimationSpec {
        TrailAnimationSpec(sampler: sampler, duration: duration, repeats: repeats, reducedMotion: reducedMotion)
    }
    public static func reveal(duration: Duration = .seconds(2), repeats: Bool = false) -> TrailAnimationSpec {
        custom(TrailSampler { .reveal(to: $0.progress) }, duration: duration, repeats: repeats)
    }
    public static func erase(duration: Duration = .seconds(2), repeats: Bool = false) -> TrailAnimationSpec {
        custom(TrailSampler { TrailVisualState(windows: [TrailWindow($0.progress, 1)]) }, duration: duration, repeats: repeats, reducedMotion: .hidden)
    }

    /// Finite steps play in order; looping applies to the entire sequence.
    public static func sequence(_ clips: [TrailAnimationSpec], repeats: Bool = false) throws -> TrailAnimationSpec {
        guard (1...64).contains(clips.count) else { throw TrailConfigurationError.sequenceCount }
        guard clips.allSatisfy({ !$0.repeats }) else { throw TrailConfigurationError.repeatingSequenceStep }
        let total = clips.reduce(0.0) { $0 + $1.durationSeconds }
        guard total.isFinite && total < Double(Int64.max / 2) else { throw TrailConfigurationError.sequenceDuration }
        let sampler = TrailSampler { time in
            if time.progress == 1, let last = clips.last {
                return last.sampler.sample(at: TrailTime(progress: 1, cycle: time.cycle, elapsedSeconds: last.durationSeconds))
            }
            let position = time.progress * total
            var start = 0.0
            for clip in clips {
                if position < start + clip.durationSeconds {
                    let local = min(clip.durationSeconds, max(0, position - start))
                    return clip.sampler.sample(at: TrailTime(progress: local / clip.durationSeconds, cycle: time.cycle, elapsedSeconds: local))
                }
                start += clip.durationSeconds
            }
            let last = clips[clips.count - 1]
            return last.sampler.sample(at: TrailTime(progress: 1, cycle: time.cycle, elapsedSeconds: last.durationSeconds))
        }
        return custom(sampler, duration: .seconds(total), repeats: repeats, reducedMotion: clips[clips.count - 1].reducedMotion)
    }
}
