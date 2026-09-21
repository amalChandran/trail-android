package dev.trail.core

import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class TrailDslTest {
    @Test fun sequenceUsesEachStepsDurationAndCanSeekOutOfOrder() {
        val effect = trailEffect {
            stroke(TrailColor.Blue)
            sequence { reveal(2.seconds); erase(1.seconds) }
        }
        assertEquals(3.0, effect.durationSeconds)
        for ((elapsed, expected) in listOf(
            2.5 to TrailWindow(0.5, 1.0),
            1.0 to TrailWindow(0.0, 0.5),
            2.0 to TrailWindow(0.0, 1.0),
            3.0 to TrailWindow(1.0, 1.0),
            0.0 to TrailWindow(0.0, 0.0),
        )) assertEquals(expected, effect.sample(0, elapsed).windows.single())
        assertTrue(effect.sample(0, 1.0, reducedMotion = true).windows.isEmpty())
    }

    @Test fun sequenceLoopsAsAWholeAndSeekOneShowsItsFinalFrame() {
        val player = TrailPlayer(trailEffect {
            sequence(repeat = true) { reveal(2.seconds); erase(1.seconds) }
        })
        player.advance(3.0)
        assertEquals(TrailWindow(0.0, 0.0), player.frame(0).windows.single())
        player.seek(1.0)
        assertEquals(TrailWindow(1.0, 1.0), player.frame(0).windows.single())
        player.play(); player.advance(1.0)
        assertEquals(0.0, player.frame(0).windows.single().start)
        assertEquals(0.5, player.frame(0).windows.single().end, 1e-12)
    }

    @Test fun invalidCompositionExplainsHowToFixIt() {
        val duplicate = assertFailsWith<IllegalArgumentException> { trailEffect { reveal(); erase() } }
        assertTrue(duplicate.message.orEmpty().contains("sequence"))
        assertTrue(duplicate.message.orEmpty().contains("layer"))
        assertFailsWith<IllegalArgumentException> { trailEffect { stroke(); layer { reveal() } } }
        assertFailsWith<IllegalArgumentException> { trailEffect { layer { stroke() }; reveal() } }
        assertFailsWith<IllegalArgumentException> { trailEffect { sequence { } } }
        val repeats = assertFailsWith<IllegalArgumentException> {
            trailEffect { sequence { animation(TrailAnimations.reveal(repeat = true)) } }
        }
        assertTrue(repeats.message.orEmpty().contains("repeat = true on sequence"))
        assertFailsWith<IllegalArgumentException> { trailEffect { sequence { repeat(65) { reveal() } } } }
        assertFailsWith<IllegalArgumentException> { trailEffect { repeat(17) { layer { stroke() } } } }
    }

    @Test fun layersUseTheirOwnTimingsAndPreserveDrawingOrder() {
        val effect = trailEffect {
            layer { stroke(TrailColor.White, 10.0) }
            layer { stroke(TrailColor.Blue, 6.0); reveal(2.seconds) }
            layer { stroke(TrailColor.Mint, 2.0); reveal(4.seconds) }
        }
        assertEquals(listOf(TrailColor.White, TrailColor.Blue, TrailColor.Mint), effect.layers.map {
            (it.commands.single() as TrailStroke).color
        })
        assertEquals(1.0, effect.sample(0, 1.0).windows.single().end)
        assertEquals(0.5, effect.sample(1, 1.0).head)
        assertEquals(0.25, effect.sample(2, 1.0).head)
        assertEquals(4.0, effect.durationSeconds)
    }

    @Test fun namedPresetDoesNotSharePlaybackBetweenBindings() {
        val preset = trailEffect { stroke(); reveal(2.seconds) }
        val first = TrailPlayer(preset)
        val second = TrailPlayer(preset)
        first.advance(1.0); second.seek(0.75)
        assertEquals(0.5, first.progress)
        assertEquals(TrailPlaybackStatus.Playing, first.status)
        assertEquals(0.75, second.progress)
        assertEquals(TrailPlaybackStatus.Paused, second.status)
        first.replay()
        assertEquals(0.75, second.progress)
    }

    @Test fun replacingLoopingEffectPreservesCycleProgressAndPauseIntent() {
        val player = TrailPlayer(trailEffect { reveal(2.seconds, repeat = true) })
        player.advance(8.5)
        player.configure(trailEffect { reveal(8.seconds) })
        assertEquals(0.25, player.progress)
        assertEquals(2.0, player.elapsedSeconds)
        assertEquals(TrailPlaybackStatus.Playing, player.status)
        player.pause()
        player.configure(trailEffect { stroke(TrailColor.Mint); reveal(4.seconds) })
        assertEquals(0.25, player.progress)
        assertEquals(TrailPlaybackStatus.Paused, player.status)
    }

    @Test fun staticEffectRetainsAutoPlayIntentButExplicitPauseWins() {
        val animated = trailEffect { reveal() }
        val automatic = TrailPlayer(trailEffect { stroke() })
        automatic.configure(animated)
        assertEquals(TrailPlaybackStatus.Playing, automatic.status)
        val paused = TrailPlayer(trailEffect { stroke() })
        paused.pause(); paused.configure(animated)
        assertEquals(TrailPlaybackStatus.Paused, paused.status)
        paused.configure(animated, reset = true)
        assertEquals(TrailPlaybackStatus.Playing, paused.status)
        assertEquals(0.0, paused.progress)
    }
}
