import Foundation
import Testing
import TrailCore
import TrailEffects
import TrailSamplePlugin

@Test func arcLengthInterpolation() {
    let path = TrailPath([TrailPoint(0, 0), TrailPoint(3, 0), TrailPoint(3, 4)])
    #expect(path.length == 7)
    #expect(path.point(at: 0.5) == TrailPoint(3, 0.5))
    #expect(path.slice(from: 0.2, to: 0.8).count == 3)
    #expect(abs(path.tangent(at: 0.8) - .pi / 2) < 1e-12)
}
@Test func emptyAndDegenerateGeometry() {
    #expect(TrailPath([]).point(at: 0.5) == nil)
    let path = TrailPath([TrailPoint(2, 3), TrailPoint(2, 3)])
    #expect(path.length == 0)
    #expect(path.point(at: 0.2) == TrailPoint(2, 3))
    #expect(path.slice(from: 0, to: 1).isEmpty)
    #expect(path.fitted(width: 0, height: 0).points.allSatisfy { $0.x.isFinite && $0.y.isFinite })
}
@Test func invalidGeographicInput() {
    #expect(throws: TrailError.self) { try TrailCoordinate(latitude: 91, longitude: 0) }
    #expect(throws: TrailError.self) { try TrailCoordinate(latitude: 0, longitude: .nan) }
    #expect(throws: TrailError.self) { try TrailRoute(id: " ", coordinates: []) }
}
@Test func pauseCompletionAndReplay() {
    var player = TrailPlayer(effect: TrailEffect(animation: TrailAnimations.reveal(duration: .seconds(2))))
    player.advance(by: 0.5); player.pause(); player.advance(by: 10)
    #expect(player.progress == 0.25)
    player.play(); player.advance(by: 5)
    #expect(player.status == .finished); #expect(player.frame(layer: 0).head == 1)
    player.play(); #expect(player.progress == 0)
}
@Test func loopBoundaryAndExplicitEndpoint() {
    var player = TrailPlayer(effect: TrailEffect(animation: TrailAnimations.reveal(duration: .seconds(2), repeats: true)))
    player.advance(by: 2); #expect(player.frame(layer: 0).head == 0)
    player.advance(by: 0.5); player.pause(); #expect(player.progress == 0.25)
    player.seek(to: 1); #expect(player.frame(layer: 0).head == 1)
    player.play(); player.advance(by: 0.2); #expect(abs(player.progress - 0.1) < 1e-12)
}
@Test func reconfigurationPreservesProgress() {
    var player = TrailPlayer(effect: TrailEffect(animation: TrailAnimations.reveal(duration: .seconds(2))))
    player.advance(by: 0.5)
    player.configure(TrailEffect(animation: TrailAnimations.reveal(duration: .seconds(8))))
    #expect(player.progress == 0.25); #expect(player.elapsedSeconds == 2)
    player.configure(player.effect, reset: true); #expect(player.progress == 0)
}
@Test func builderAndExternalPlugin() {
    let effect = TrailEffect { Stroke(.blue, width: 6); Reveal(duration: .seconds(2)) }
    #expect(effect.layers.count == 1)
    #expect(TrailEffect.metro().layers[0].commands.count == 3)
    let values = [0.0, 0.25, 0.5, 0.75, 1.0].map { QuadraticReveal().sample(at: TrailTime(progress: $0)).head! }
    #expect(values == [0, 0.0625, 0.25, 0.5625, 1])
}
@Test func reducedMotionHasStableIntent() {
    let reveal = TrailEffect(animation: TrailAnimations.reveal())
    let erase = TrailEffect(animation: TrailAnimations.erase())
    #expect(reveal.sample(layer: 0, elapsed: 0.1, reducedMotion: true) == .full)
    #expect(erase.sample(layer: 0, elapsed: 0.1, reducedMotion: true) == .hidden)
}
@Test(arguments: TrailStylePreset.allCases, TrailMotionPreset.allCases)
func everyPresetIsDeterministic(style: TrailStylePreset, motion: TrailMotionPreset) {
    let effect = motion.effect(style: style.style(), duration: .seconds(1), repeats: false)
    for p in [0.0, 0.9, 0.25, 1, 0.5, 0.001] {
        for layer in effect.layers.indices {
            let a = effect.sample(layer: layer, elapsed: p)
            _ = effect.sample(layer: layer, elapsed: 0.8)
            #expect(a == effect.sample(layer: layer, elapsed: p))
            #expect(a.windows.count <= 256)
        }
    }
}
@Test func preparedGradientSpansWholeRoute() {
    let commands = TrailEffect(style: TrailStylePreset.gradient.style()).layers[0].commands
    #expect(commands.count == 64)
    if case .stroke(let first) = commands[0], case .stroke(let last) = commands[63] {
        #expect(first.start == 0); #expect(last.end == 1)
        #expect(first.color == .mint); #expect(last.color == .coral)
    } else { Issue.record("Expected stroke commands") }
}
