import Foundation

public struct TrailColor: Sendable, Equatable, Hashable {
    public let argb: UInt32
    public init(_ argb: UInt32) { self.argb = argb }
    public func opacity(_ value: Double) -> Self {
        fractionCheck(value)
        return Self((argb & 0x00ffffff) | (UInt32(Double((argb >> 24) & 255) * value) << 24))
    }
    public static let blue = Self(0xFF2563EB), white = Self(0xFFFFFFFF), mint = Self(0xFF32D6AD), coral = Self(0xFFFF785A)
}

public struct TrailStroke: Sendable, Equatable {
    public let color: TrailColor
    public let width: Double
    public let start: Double
    public let end: Double
    public let dash: [Double]
    public let roundCap: Bool
}
public struct TrailChevrons: Sendable, Equatable {
    public let color: TrailColor
    public let size: Double
    public let spacing: Double
}
public enum TrailCommand: Sendable, Equatable { case stroke(TrailStroke), chevrons(TrailChevrons) }

public protocol TrailLineStyle: Sendable { func draw(in context: inout TrailDrawContext) }
public struct TrailStyle: TrailLineStyle {
    private let body: @Sendable (inout TrailDrawContext) -> Void
    public init(_ body: @escaping @Sendable (inout TrailDrawContext) -> Void) { self.body = body }
    public func draw(in context: inout TrailDrawContext) { body(&context) }
}
/// Value-scoped command writer; copying a context cannot mutate a prepared effect.
public struct TrailDrawContext {
    private(set) var commands: [TrailCommand] = []
    public mutating func stroke(color: TrailColor = .blue, width: Double = 6, from start: Double = 0, to end: Double = 1, dash: [Double] = [], roundCap: Bool = true) {
        precondition(width.isFinite && width > 0 && width <= 1024)
        fractionCheck(start); fractionCheck(end); precondition(start <= end)
        precondition(dash.isEmpty || (dash.count % 2 == 0 && dash.count <= 32 && dash.allSatisfy { $0.isFinite && $0 > 0 }))
        precondition(commands.count < 512)
        commands.append(.stroke(TrailStroke(color: color, width: width, start: start, end: end, dash: dash, roundCap: roundCap)))
    }
    public mutating func chevrons(color: TrailColor = .blue, size: Double = 8, spacing: Double = 28) {
        precondition(size.isFinite && size > 0 && size <= 1024 && spacing.isFinite && spacing >= 2 && commands.count < 512)
        commands.append(.chevrons(TrailChevrons(color: color, size: size, spacing: spacing)))
    }
}

public enum TrailStyles {
    public static func solid(_ color: TrailColor = .blue, width: Double = 6) -> some TrailLineStyle { TrailStyle { $0.stroke(color: color, width: width) } }
    public static func cased(_ color: TrailColor = .blue, width: Double = 6, casing: TrailColor = .white, casingWidth: Double = 3) -> some TrailLineStyle {
        precondition(casingWidth.isFinite && casingWidth >= 0)
        return TrailStyle { $0.stroke(color: casing, width: width + 2 * casingWidth); $0.stroke(color: color, width: width) }
    }
    public static func dashed(_ color: TrailColor = .blue, width: Double = 6, dash: Double = 12, gap: Double = 8) -> some TrailLineStyle { TrailStyle { $0.stroke(color: color, width: width, dash: [dash, gap]) } }
    public static func dotted(_ color: TrailColor = .blue, width: Double = 6, gap: Double = 12) -> some TrailLineStyle { TrailStyle { $0.stroke(color: color, width: width, dash: [0.01, gap]) } }
}

public struct TrailLayer: Sendable {
    public let commands: [TrailCommand]
    public let animation: TrailAnimationSpec?
    public init(style: any TrailLineStyle, animation: TrailAnimationSpec? = nil) {
        var context = TrailDrawContext(); style.draw(in: &context)
        self.commands = context.commands; self.animation = animation
    }
}

public struct TrailEffect: Sendable {
    public let id: UUID
    public let layers: [TrailLayer]
    public let durationSeconds: Double
    public let repeats: Bool
    public init(style: any TrailLineStyle = TrailStyles.solid(), animation: TrailAnimationSpec? = nil) { self.init(layers: [TrailLayer(style: style, animation: animation)]) }
    public init(layers: [TrailLayer]) {
        precondition((1...16).contains(layers.count))
        self.id = UUID()
        self.layers = layers
        self.durationSeconds = layers.map { $0.animation?.durationSeconds ?? 0 }.max() ?? 0
        self.repeats = layers.contains { $0.animation?.repeats == true }
    }
    public func sample(layer: Int, elapsed: Double, reducedMotion: Bool = false) -> TrailVisualState {
        precondition(elapsed.isFinite && elapsed >= 0)
        guard let spec = layers[layer].animation else { return .full }
        if reducedMotion { return spec.reducedMotion }
        let cycles = elapsed / spec.durationSeconds
        let progress = spec.repeats ? cycles - floor(cycles) : min(1, cycles)
        return spec.sampler.sample(at: TrailTime(progress: progress, cycle: spec.repeats ? Int(min(floor(cycles), Double(Int.max - 1024))) : 0,
                                                elapsedSeconds: spec.repeats ? progress * spec.durationSeconds : min(elapsed, spec.durationSeconds)))
    }
    public init(@TrailEffectBuilder _ content: () -> [TrailEffectPart]) {
        do { self = try Self(parts: content()) }
        catch { preconditionFailure(String(describing: error)) }
    }
    /// Use when structural DSL errors need recovery, such as a configurable preset editor.
    public static func validating(@TrailEffectBuilder _ content: () -> [TrailEffectPart]) throws -> Self {
        try Self(parts: content())
    }
    private init(parts: [TrailEffectPart]) throws {
        var style: any TrailLineStyle = TrailStyles.solid()
        var animation: TrailAnimationSpec?
        var layers: [TrailLayer] = []
        var hasStyle = false, hasAnimation = false
        for part in parts {
            switch part {
            case .style(let value):
                guard !hasStyle else { throw TrailConfigurationError.duplicateStyle }
                hasStyle = true; style = value
            case .animation(let value):
                guard !hasAnimation else { throw TrailConfigurationError.duplicateAnimation }
                hasAnimation = true; animation = value
            case .layer(let value): layers.append(value)
            case .group(let parts): layers.append(contentsOf: try Self(parts: parts).layers)
            case .sequence(let parts, let repeats):
                guard !hasAnimation else { throw TrailConfigurationError.duplicateAnimation }
                hasAnimation = true; animation = try Self.sequence(parts, repeats: repeats)
            }
        }
        guard layers.isEmpty || (!hasStyle && !hasAnimation) else { throw TrailConfigurationError.mixedLayers }
        if layers.isEmpty { layers.append(TrailLayer(style: style, animation: animation)) }
        guard layers.count <= 16 else { throw TrailConfigurationError.layerCount }
        self.init(layers: layers)
    }
    private static func sequence(_ parts: [TrailEffectPart], repeats: Bool) throws -> TrailAnimationSpec {
        let clips = try parts.map { part -> TrailAnimationSpec in
            switch part {
            case .animation(let spec): return spec
            case .sequence(let nested, let loops): return try sequence(nested, repeats: loops)
            default: throw TrailConfigurationError.nonAnimationSequenceStep
            }
        }
        return try TrailAnimations.sequence(clips, repeats: repeats)
    }
}

public enum TrailEffectPart: Sendable {
    case style(any TrailLineStyle), animation(TrailAnimationSpec), layer(TrailLayer)
    case group([TrailEffectPart]), sequence([TrailEffectPart], repeats: Bool)
}
@resultBuilder public enum TrailEffectBuilder {
    public static func buildExpression(_ part: TrailEffectPart) -> [TrailEffectPart] { [part] }
    public static func buildBlock(_ parts: [TrailEffectPart]...) -> [TrailEffectPart] { parts.flatMap { $0 } }
    public static func buildOptional(_ part: [TrailEffectPart]?) -> [TrailEffectPart] { part ?? [] }
    public static func buildEither(first: [TrailEffectPart]) -> [TrailEffectPart] { first }
    public static func buildEither(second: [TrailEffectPart]) -> [TrailEffectPart] { second }
    public static func buildArray(_ parts: [[TrailEffectPart]]) -> [TrailEffectPart] { parts.flatMap { $0 } }
}
public func Stroke(_ color: TrailColor = .blue, width: Double = 6) -> TrailEffectPart { .style(TrailStyles.solid(color, width: width)) }
public func Reveal(duration: Duration = .seconds(2), repeats: Bool = false) -> TrailEffectPart { .animation(TrailAnimations.reveal(duration: duration, repeats: repeats)) }
public func Erase(duration: Duration = .seconds(2), repeats: Bool = false) -> TrailEffectPart { .animation(TrailAnimations.erase(duration: duration, repeats: repeats)) }
public func Style(_ style: any TrailLineStyle) -> TrailEffectPart { .style(style) }
public func Animate(_ spec: TrailAnimationSpec) -> TrailEffectPart { .animation(spec) }
public func Layer(style: any TrailLineStyle, animation: TrailAnimationSpec? = nil) -> TrailEffectPart { .layer(TrailLayer(style: style, animation: animation)) }
public func Layer(@TrailEffectBuilder _ content: () -> [TrailEffectPart]) -> TrailEffectPart { .group(content()) }
public func Sequence(repeats: Bool = false, @TrailEffectBuilder _ content: () -> [TrailEffectPart]) -> TrailEffectPart { .sequence(content(), repeats: repeats) }
