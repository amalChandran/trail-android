import TrailCore

/// A separate package target consuming only Trail's public interfaces.
public struct MetroStyle: TrailLineStyle {
    public let color: TrailColor
    public init(color: TrailColor = .coral) { self.color = color }
    public func draw(in context: inout TrailDrawContext) {
        context.stroke(color: .white, width: 12)
        context.stroke(color: color, width: 7)
        context.chevrons(color: .white, size: 4, spacing: 32)
    }
}
public struct QuadraticReveal: TrailAnimation {
    public init() {}
    public func sample(at time: TrailTime) -> TrailVisualState { .reveal(to: time.progress * time.progress) }
}
public extension TrailEffect {
    static func metro() -> TrailEffect { TrailEffect { Style(MetroStyle()); Animate(TrailAnimations.custom(QuadraticReveal())) } }
}
