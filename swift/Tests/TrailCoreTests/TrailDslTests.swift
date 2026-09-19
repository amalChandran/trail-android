import Testing
import TrailCore

@Test func sequenceUsesEachStepsDurationAndCanSeekOutOfOrder() {
    let effect = TrailEffect {
        Stroke(.blue)
        Sequence { Reveal(duration: .seconds(2)); Erase(duration: .seconds(1)) }
    }
    #expect(effect.durationSeconds == 3)
    for (elapsed, expected) in [
        (2.5, TrailWindow(0.5, 1)), (1.0, TrailWindow(0, 0.5)),
        (2.0, TrailWindow(0, 1)), (3.0, TrailWindow(1, 1)), (0.0, TrailWindow(0, 0)),
    ] {
        #expect(effect.sample(layer: 0, elapsed: elapsed).windows == [expected])
    }
    #expect(effect.sample(layer: 0, elapsed: 1, reducedMotion: true) == .hidden)
}

@Test func sequenceLoopsAsAWholeAndSeekOneShowsItsFinalFrame() {
    var player = TrailPlayer(effect: TrailEffect {
        Sequence(repeats: true) { Reveal(duration: .seconds(2)); Erase(duration: .seconds(1)) }
    })
    player.advance(by: 3)
    #expect(player.frame(layer: 0).windows == [TrailWindow(0, 0)])
    player.seek(to: 1)
    #expect(player.frame(layer: 0).windows == [TrailWindow(1, 1)])
    player.play(); player.advance(by: 1)
    #expect(player.frame(layer: 0).windows[0].start == 0)
    #expect(abs(player.frame(layer: 0).windows[0].end - 0.5) < 1e-12)
}

@Test func invalidCompositionIsRecoverableAndActionable() {
    #expect(throws: TrailConfigurationError.duplicateAnimation) {
        try TrailEffect.validating { Reveal(); Erase() }
    }
    #expect(TrailConfigurationError.duplicateAnimation.description.contains("Sequence"))
    #expect(throws: TrailConfigurationError.duplicateStyle) {
        try TrailEffect.validating { Stroke(); Stroke(.mint) }
    }
    #expect(throws: TrailConfigurationError.mixedLayers) {
        try TrailEffect.validating { Stroke(); Layer { Reveal() } }
    }
    #expect(throws: TrailConfigurationError.mixedLayers) {
        try TrailEffect.validating { Layer { Stroke() }; Reveal() }
    }
    #expect(throws: TrailConfigurationError.sequenceCount) {
        try TrailEffect.validating { Sequence { } }
    }
    #expect(throws: TrailConfigurationError.repeatingSequenceStep) {
        try TrailEffect.validating { Sequence { Reveal(repeats: true) } }
    }
    #expect(throws: TrailConfigurationError.nonAnimationSequenceStep) {
        try TrailEffect.validating { Sequence { Stroke() } }
    }
    #expect(throws: TrailConfigurationError.sequenceCount) {
        try TrailEffect.validating { Sequence { for _ in 0..<65 { Reveal() } } }
    }
    #expect(throws: TrailConfigurationError.layerCount) {
        try TrailEffect.validating { for _ in 0..<17 { Layer { Stroke() } } }
    }
}

@Test func layersUseTheirOwnTimingsAndPreserveDrawingOrder() {
    let effect = TrailEffect {
        Layer { Stroke(.white, width: 10) }
        Layer { Stroke(.blue, width: 6); Reveal(duration: .seconds(2)) }
        Layer { Stroke(.mint, width: 2); Reveal(duration: .seconds(4)) }
    }
    let colors = effect.layers.compactMap { layer -> TrailColor? in
        if case .stroke(let command) = layer.commands.first { return command.color }
        return nil
    }
    #expect(colors == [.white, .blue, .mint])
    #expect(effect.sample(layer: 0, elapsed: 1) == .full)
    #expect(effect.sample(layer: 1, elapsed: 1).head == 0.5)
    #expect(effect.sample(layer: 2, elapsed: 1).head == 0.25)
    #expect(effect.durationSeconds == 4)
}

@Test func namedPresetDoesNotSharePlaybackBetweenBindings() {
    let preset = TrailEffect { Stroke(); Reveal(duration: .seconds(2)) }
    var first = TrailPlayer(effect: preset), second = TrailPlayer(effect: preset)
    first.advance(by: 1); second.seek(to: 0.75)
    #expect(first.progress == 0.5); #expect(first.status == .playing)
    #expect(second.progress == 0.75); #expect(second.status == .paused)
    first.replay()
    #expect(second.progress == 0.75)
}

@Test func replacingLoopingEffectPreservesCycleProgressAndPauseIntent() {
    var player = TrailPlayer(effect: TrailEffect { Reveal(duration: .seconds(2), repeats: true) })
    player.advance(by: 8.5)
    player.configure(TrailEffect { Reveal(duration: .seconds(8)) })
    #expect(player.progress == 0.25); #expect(player.elapsedSeconds == 2)
    #expect(player.status == .playing)
    player.pause()
    player.configure(TrailEffect { Stroke(.mint); Reveal(duration: .seconds(4)) })
    #expect(player.progress == 0.25); #expect(player.status == .paused)
}

@Test func staticEffectRetainsAutoPlayIntentButExplicitPauseWins() {
    let animated = TrailEffect { Reveal() }
    var automatic = TrailPlayer(effect: TrailEffect { Stroke() })
    automatic.configure(animated)
    #expect(automatic.status == .playing)
    var paused = TrailPlayer(effect: TrailEffect { Stroke() })
    paused.pause(); paused.configure(animated)
    #expect(paused.status == .paused)
    paused.configure(animated, reset: true)
    #expect(paused.status == .playing); #expect(paused.progress == 0)
}
