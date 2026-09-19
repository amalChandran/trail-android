import Foundation

/// Recoverable structural errors from TrailEffect.validating { }. The ordinary initializer
/// treats these as programmer errors and fails with the same actionable description.
public enum TrailConfigurationError: Error, Equatable, CustomStringConvertible, LocalizedError {
    case duplicateStyle
    case duplicateAnimation
    case mixedLayers
    case layerCount
    case sequenceCount
    case repeatingSequenceStep
    case nonAnimationSequenceStep
    case sequenceDuration

    public var description: String {
        switch self {
        case .duplicateStyle: "One style is allowed per layer. Put each style in its own Layer { Style(...) } block."
        case .duplicateAnimation: "One animation is allowed per layer. Use Sequence { Reveal(...); Erase(...) } for ordered steps, or separate Layer { } blocks for simultaneous animations."
        case .mixedLayers: "Do not mix top-level style/animation declarations with Layer { }. Put every style and animation inside an explicit Layer { } block."
        case .layerCount: "An effect needs 1–16 layers."
        case .sequenceCount: "A sequence needs 1–64 animation steps."
        case .repeatingSequenceStep: "A sequence step cannot repeat. Set repeats: true on Sequence { } instead."
        case .nonAnimationSequenceStep: "Sequence { } accepts animation steps only. Declare Stroke or Style outside the sequence."
        case .sequenceDuration: "The total sequence duration must be finite and representable."
        }
    }
    public var errorDescription: String? { description }
}
