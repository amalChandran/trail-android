import SwiftUI
import CoreGraphics
import TrailCore
import TrailUI
import TrailEffects

@MainActor struct VehicleArtwork {
    struct Layer {
        let path: CGPath, fill: CGColor, stroke: CGColor?, width: Double
    }
    let id: String, height: Double, anchor: TrailPoint, size: Double, headingWindow: Double, duration: Double
    let layers: [Layer]

    /// Paths/colors are prepared once. Every frame changes only the drawing transform.
    func draw(in context: CGContext, pose: TrailPose) {
        context.saveGState()
        context.translateBy(x: pose.point.x,y: pose.point.y)
        context.rotate(by: pose.headingRadians + .pi/2)
        context.scaleBy(x: size/height,y: size/height)
        context.translateBy(x: -anchor.x,y: -anchor.y)
        context.setLineJoin(.round)
        for layer in layers {
            context.setFillColor(layer.fill); context.addPath(layer.path); context.fillPath()
            if let stroke=layer.stroke {
                context.setStrokeColor(stroke); context.setLineWidth(layer.width)
                context.addPath(layer.path); context.strokePath()
            }
        }
        context.restoreGState()
    }
    func animation(_ motion: TrailMotionPreset) -> TrailAnimationSpec {
        let base=motion.animation(duration: .seconds(duration),repeats: true)
        return TrailAnimations.custom(TrailSampler { time in
            base.sampler.sample(at: TrailTime(progress: journeyProgress(time.progress),cycle: time.cycle,elapsedSeconds: time.elapsedSeconds))
        },duration: .seconds(duration),repeats: true,reducedMotion: base.reducedMotion)
    }

    static let fleet: [String:VehicleArtwork] = {
        struct File: Decodable { let vehicles: [Record] }
        struct Record: Decodable {
            struct Shape: Decodable { let path: [[Double]], fill: String, stroke: String?, width: Double? }
            let id: String, height: Double, anchor: [Double], size: Double, headingWindow: Double, duration: Double, layers: [Shape]
        }
        let url=Bundle.main.url(forResource: "vehicles",withExtension: "json")!
        let file=try! JSONDecoder().decode(File.self,from: Data(contentsOf: url))
        func color(_ hex: String) -> CGColor {
            let argb=UInt32(hex.dropFirst(),radix: 16)!
            let components=[CGFloat((argb>>16)&255)/255,CGFloat((argb>>8)&255)/255,CGFloat(argb&255)/255,CGFloat(argb>>24)/255]
            return CGColor(colorSpace: CGColorSpace(name: CGColorSpace.sRGB)!,components: components)!
        }
        return Dictionary(uniqueKeysWithValues: file.vehicles.map { record in
            let layers=record.layers.map { shape in
                let path=CGMutablePath()
                for command in shape.path {
                    func point(_ i: Int) -> CGPoint { CGPoint(x: command[i],y: command[i+1]) }
                    switch Int(command[0]) {
                    case 0: path.move(to: point(1))
                    case 1: path.addLine(to: point(1))
                    case 2: path.addCurve(to: point(5),control1: point(1),control2: point(3))
                    case 3: path.addQuadCurve(to: point(3),control: point(1))
                    case 4: path.closeSubpath()
                    default: preconditionFailure("Unknown vehicle path command")
                    }
                }
                return Layer(path: path,fill: color(shape.fill),stroke: shape.stroke.map(color),width: shape.width ?? 1)
            }
            return (record.id,VehicleArtwork(id: record.id,height: record.height,anchor: TrailPoint(record.anchor[0],record.anchor[1]),
                size: record.size,headingWindow: record.headingWindow,duration: record.duration,layers: layers))
        })
    }()
}

/// Same eased time drives the route head and the vehicle, with no independent animation clock.
func journeyProgress(_ p: Double) -> Double { min(1,max(0,p*p*p*(10+p*(-15+6*p)))) }

@MainActor struct JourneyVehicle: View {
    let path: TrailPath, artwork: VehicleArtwork, playback: TrailPlayback, reduced: Bool
    var body: some View {
        let frame=playback.frame(layer: playback.player.effect.layers.count-1)
        let fraction=reduced ? 1 : frame.head ?? journeyProgress(playback.progress)
        let pose=path.pose(at: fraction,headingWindow: artwork.headingWindow,direction: reduced ? .forward : frame.headDirection)
        Canvas { context,_ in
            if let pose { context.withCGContext { artwork.draw(in: $0,pose: pose) } }
        }
        .allowsHitTesting(false).accessibilityHidden(true)
    }
}
