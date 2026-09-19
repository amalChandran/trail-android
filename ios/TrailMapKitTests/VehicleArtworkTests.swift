import Testing
import Foundation
import CoreGraphics
import TrailCore
import TrailEffects
@testable import TrailPlayground

private struct VehicleCase: Sendable, CustomStringConvertible {
    let id: String, degrees: Int, density: Double
    var description: String { "\(id)/heading=\(degrees)/density=\(density)" }
    static let all=["cab","flight","ferry"].flatMap { id in [0,90,180,270].flatMap { angle in
        [2.0,3.0].map { VehicleCase(id: id,degrees: angle,density: $0) }
    } }
}

@MainActor @Test(arguments: VehicleCase.all)
private func topDownArtworkFacesItsRouteHeadingAndKeepsItsAnchor(_ c: VehicleCase) throws {
    let art=try #require(VehicleArtwork.fleet[c.id])
    let context=try #require(CGContext(data: nil,width: 256,height: 256,bitsPerComponent: 8,bytesPerRow: 256*4,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue))
    let angle=Double(c.degrees)*Double.pi/180, center=128/c.density
    let path=TrailPath([TrailPoint(center-cos(angle)*10,center-sin(angle)*10),TrailPoint(center+cos(angle)*10,center+sin(angle)*10)])
    // Match SwiftUI Canvas's top-left, y-down drawing space. A raw bitmap CGContext is y-up.
    context.translateBy(x: 0,y: 256)
    context.scaleBy(x: c.density,y: -c.density)
    art.draw(in: context,pose: path.pose(at: 0.5)!)
    let probeY: Double = c.id == "cab" ? 19 : c.id == "flight" ? 18 : 44
    let expected = c.id == "cab" ? [255,204,102] : c.id == "flight" ? [32,63,80] : [146,184,204]
    let distance=(art.anchor.y-probeY)*art.size/art.height*c.density
    let x=Int((128+cos(angle)*distance).rounded()), y=Int((128+sin(angle)*distance).rounded())
    let bytes=try #require(context.data).assumingMemoryBound(to: UInt8.self)
    let error=(-2...2).flatMap { dx in (-2...2).map { dy in
        let offset=((y+dy)*256+x+dx)*4
        return (0..<3).reduce(0) { $0+abs(Int(bytes[offset+$1])-expected[$1]) }
    } }.min()!
    #expect(error <= 6)
    #expect(bytes[(128*256+128)*4+3] > 240)
    #expect(bytes[3] == 0)
    let spec=art.animation(.reveal)
    var player=TrailPlayer(effect: TrailEffect { Animate(spec) })
    player.seek(to: 0.3); let paused=player.frame(layer: 0).head
    player.advance(by: 5); #expect(player.frame(layer: 0).head == paused)
    #expect(abs(paused!-journeyProgress(0.3)) < 1e-9)
    player.replay(); #expect(player.frame(layer: 0).head == 0)
}
