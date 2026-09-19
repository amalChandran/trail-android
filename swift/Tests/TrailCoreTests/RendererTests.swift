import CoreGraphics
import Foundation
import Testing
import TrailCore
import TrailUI

private struct RasterCase: Sendable, CustomStringConvertible {
    let width: Double, color: TrailColor, opacity: Double, erase: Bool, progress: Double
    var description: String { "\(erase ? "erase" : "reveal")/\(progress)/width=\(width)/color=\(color.argb)/alpha=\(opacity)" }
    static let all = [2.0,6.0,12.0].flatMap { w in [TrailColor.blue,.mint,.coral].flatMap { c in
        [0.25,1.0].flatMap { a in [false,true].flatMap { e in [0.0,0.25,0.75,1.0].map { p in RasterCase(width: w,color: c,opacity: a,erase: e,progress: p) } } }
    } }
}
private func bitmap() throws -> CGContext {
    try #require(CGContext(data: nil,width: 220,height: 160,bitsPerComponent: 8,bytesPerRow: 220*4,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue))
}
private func pixel(_ context: CGContext,_ x: Int,_ y: Int) -> [Int] {
    let data=context.data!.assumingMemoryBound(to: UInt8.self), offset=y*context.bytesPerRow+x*4
    return (0..<4).map { Int(data[offset+$0]) }
}
@Test(arguments: RasterCase.all)
private func coreGraphicsPixelsRespectWindowWidthColorAndOpacity(_ c: RasterCase) throws {
    let context=try bitmap()
    let effect=TrailEffect { Stroke(c.color,width: c.width); if c.erase { Erase() } else { Reveal() } }
    let frame=effect.sample(layer: 0,elapsed: c.progress*2)
    TrailRenderer.draw(path: TrailPath([TrailPoint(20,80),TrailPoint(180,80)]),effect: effect,in: context) { _ in
        TrailVisualState(windows: frame.windows,opacity: c.opacity)
    }
    let start=c.erase ? 20+160*c.progress : 20, end=c.erase ? 180 : 20+160*c.progress
    for x in 0..<220 {
        let center=Double(x)+0.5, value=pixel(context,x,80)
        if start < end && center > start+c.width && center < end-c.width {
            #expect(abs(value[3]-Int(255*c.opacity))<=2)
            for (channel,shift) in [16,8,0].enumerated() {
                let expected=Double((c.color.argb >> shift)&255)*c.opacity
                #expect(abs(Double(value[channel])-expected)<=2)
            }
        }
        if start==end || center < start-c.width || center > end+c.width { #expect(value[3]==0) }
        #expect(pixel(context,x,Int(80+c.width/2+2))[3]==0)
    }
}
@Test private func coreGraphicsNeverBridgesContoursAndRestoresGraphicsState() throws {
    let context=try bitmap()
    context.setLineWidth(17); context.translateBy(x: 0,y: 1)
    let transform=context.ctm
    let path=TrailPath([TrailPoint(10,79),TrailPoint(50,79),TrailPoint(170,79),TrailPoint(210,79)],breakBefore: [2])
    TrailRenderer.draw(path: path,effect: TrailEffect(),in: context) { _ in .reveal(to: 0.75) }
    #expect(context.ctm==transform)
    #expect(pixel(context,30,80)[3]>0); #expect(pixel(context,180,80)[3]>0)
    #expect(pixel(context,100,80)[3]==0); #expect(pixel(context,205,80)[3]==0)
}
