import CoreGraphics
import Foundation
import TrailCore

/// Core Graphics executor. Each draw saves/restores all graphics state.
public enum TrailRenderer {
    public static func draw(path: TrailPath, effect: TrailEffect, in context: CGContext, frame: (Int) -> TrailVisualState) {
        guard path.length > 0 else { return }
        context.saveGState(); defer { context.restoreGState() }
        context.setLineJoin(.round)
        for (index, layer) in effect.layers.enumerated() {
            let state = frame(index)
            guard state.opacity > 0 && state.widthScale > 0 else { continue }
            for command in layer.commands {
                switch command {
                case .stroke(let stroke):
                    context.setLineWidth(stroke.width * state.widthScale)
                    context.setLineCap(stroke.roundCap ? .round : .butt)
                    let cycle = stroke.dash.reduce(0, +)
                    for window in state.windows {
                        let start = max(stroke.start, window.start), end = min(stroke.end, window.end)
                        guard start < end else { continue }
                        let points = path.slice(from: start, to: end)
                        guard let first = points.first else { continue }
                        context.setStrokeColor(stroke.color.cgColor)
                        context.setAlpha(state.opacity * window.opacity)
                        context.setLineDash(phase: cycle == 0 ? 0 : (start * path.length - state.dashPhase * cycle).truncatingRemainder(dividingBy: cycle), lengths: stroke.dash.map { CGFloat($0) })
                        context.beginPath(); context.move(to: CGPoint(x: first.x, y: first.y))
                        for point in points.dropFirst() { context.addLine(to: CGPoint(x: point.x, y: point.y)) }
                        context.strokePath()
                    }
                case .chevrons(let stamp):
                    let count = min(2048, Int(min(2048, floor(path.length / stamp.spacing))))
                    context.setLineDash(phase: 0, lengths: []); context.setLineCap(.round)
                    context.setLineWidth(stamp.size * 0.25 * state.widthScale); context.setStrokeColor(stamp.color.cgColor)
                    for i in 0..<count {
                        let fraction = ((Double(i) + 0.5) * stamp.spacing / path.length + state.dashPhase * stamp.spacing / path.length).truncatingRemainder(dividingBy: 1)
                        guard let window = state.windows.first(where: { fraction >= $0.start && fraction <= $0.end }), let point = path.point(at: fraction) else { continue }
                        context.saveGState(); context.setAlpha(state.opacity * window.opacity)
                        context.translateBy(x: point.x, y: point.y); context.rotate(by: path.tangent(at: fraction))
                        let size = stamp.size * state.widthScale
                        context.beginPath(); context.move(to: CGPoint(x: -size / 2, y: -size / 2))
                        context.addLine(to: CGPoint(x: size / 2, y: 0)); context.addLine(to: CGPoint(x: -size / 2, y: size / 2))
                        context.strokePath(); context.restoreGState()
                    }
                }
            }
        }
    }
}

private extension TrailColor {
    var cgColor: CGColor { CGColor(red: Double((argb >> 16) & 255) / 255, green: Double((argb >> 8) & 255) / 255, blue: Double(argb & 255) / 255, alpha: Double((argb >> 24) & 255) / 255) }
}
