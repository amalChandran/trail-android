import Foundation
import TrailCore

public enum TrailStylePreset: String, CaseIterable, Sendable {
    case solid = "Solid", cased = "Cased", dashed = "Dashed", dotted = "Dotted", gradient = "Gradient", glow = "Glow", chevrons = "Chevrons", tapered = "Tapered"
    public func style(color: TrailColor = .mint, width: Double = 6) -> any TrailLineStyle {
        switch self {
        case .solid: return TrailStyles.solid(color, width: width)
        case .cased: return TrailStyles.cased(color, width: width, casing: TrailColor(0xFF132B36))
        case .dashed: return TrailStyles.dashed(color, width: width)
        case .dotted: return TrailStyles.dotted(color, width: width)
        case .gradient: return TrailStyle { context in
            for i in 0..<64 { context.stroke(color: blend(color, .coral, Double(i) / 63), width: width, from: Double(i) / 64, to: Double(i + 1) / 64, roundCap: false) }
        }
        case .glow: return TrailStyle { context in
            for i in stride(from: 4, through: 1, by: -1) { context.stroke(color: color.opacity(0.035 * Double(5 - i)), width: width + Double(i) * 5) }
            context.stroke(color: color, width: width)
        }
        case .chevrons: return TrailStyle { context in
            context.stroke(color: color.opacity(0.3), width: width * 0.5); context.chevrons(color: color, size: width * 1.5, spacing: 28)
        }
        case .tapered: return TrailStyle { context in
            for i in 0..<64 { context.stroke(color: color, width: width * (0.2 + 0.8 * Double(i) / 63), from: Double(i) / 64, to: Double(i + 1) / 64) }
        }
        }
    }
}

public enum TrailMotionPreset: String, CaseIterable, Sendable {
    case reveal = "Reveal", erase = "Erase", pingPong = "Ping pong", comet = "Comet", multiComet = "Multi comet", dashFlow = "Dash flow"
    case pulse = "Pulse", breathe = "Breathe", spotlight = "Spotlight", segmentedChase = "Segment chase", revealThenFlow = "Reveal + flow", drawAndErase = "Draw + erase"
    /// Use in Animate(...). Spotlight's static base belongs in a separate layer.
    public func animation(duration: Duration = .seconds(3), repeats: Bool = true) -> TrailAnimationSpec {
        let sampler = TrailSampler { time in
            let p = time.progress
            switch self {
            case .reveal: return .reveal(to: p)
            case .erase: return TrailVisualState(windows: [TrailWindow(p, 1)])
            case .pingPong: return .reveal(to: 1 - abs(2 * p - 1), direction: p < 0.5 ? .forward : .reverse)
            case .comet, .spotlight: return cometFrame(p, length: 0.25)
            case .multiComet: return TrailVisualState(windows: (0..<3).flatMap { cometFrame((p + Double($0) / 3).truncatingRemainder(dividingBy: 1), length: 0.15).windows })
            case .dashFlow: return TrailVisualState(dashPhase: p)
            case .pulse: return TrailVisualState(opacity: 0.55 + 0.45 * (1 - cos(2 * .pi * p)) / 2)
            case .breathe: return TrailVisualState(widthScale: 1 + 0.18 * sin(2 * .pi * p))
            case .segmentedChase:
                let index = min(7, floor(p * 8)); return TrailVisualState(windows: [TrailWindow(index / 8, (index + 0.8) / 8)])
            case .revealThenFlow: return p < 0.4 ? .reveal(to: p / 0.4) : TrailVisualState(dashPhase: (p - 0.4) / 0.6, head: 1)
            case .drawAndErase: return p < 0.5 ? .reveal(to: p * 2) : TrailVisualState(windows: [TrailWindow((p - 0.5) * 2, 1)], head: 1)
            }
        }
        return TrailAnimations.custom(sampler, duration: duration, repeats: repeats, reducedMotion: self == .erase || self == .drawAndErase ? .hidden : .full)
    }
    public func effect(style: any TrailLineStyle = TrailStyles.solid(), duration: Duration = .seconds(3), repeats: Bool = true) -> TrailEffect {
        let spec = animation(duration: duration, repeats: repeats)
        return TrailEffect {
            if self == .spotlight {
                Layer { Stroke(TrailColor(0xFF344D5B), width: 6) }
                Layer { Style(style); Animate(spec) }
            } else { Style(style); Animate(spec) }
        }
    }
}

private func cometFrame(_ head: Double, length: Double) -> TrailVisualState {
    var windows: [TrailWindow] = []
    func bound(_ value: Double) -> Double { min(1, max(0, value)) }
    for i in 0..<12 {
        let from = head - length + length * Double(i) / 12, to = head - length + length * Double(i + 1) / 12
        let alpha = Double(i + 1) / 12
        if from < 0 && to > 0 { windows.append(TrailWindow(1 + from, 1, opacity: alpha)); windows.append(TrailWindow(0, to, opacity: alpha)) }
        else if to <= 0 { windows.append(TrailWindow(bound(1 + from), bound(1 + to), opacity: alpha)) }
        else { windows.append(TrailWindow(bound(from), bound(to), opacity: alpha)) }
    }
    return TrailVisualState(windows: windows, head: head)
}
private func blend(_ a: TrailColor, _ b: TrailColor, _ t: Double) -> TrailColor {
    func channel(_ shift: UInt32) -> UInt32 {
        let start = Double((a.argb >> shift) & 255), end = Double((b.argb >> shift) & 255)
        return UInt32(start + (end - start) * t) << shift
    }
    return TrailColor(channel(24) | channel(16) | channel(8) | channel(0))
}
