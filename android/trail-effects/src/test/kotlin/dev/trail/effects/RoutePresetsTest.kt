package dev.trail.effects

import dev.trail.core.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class RoutePresetsTest {
    @Test fun cometOverlappingStepsHonorTheSuppliedColorOpacity() {
        for (alpha in listOf(0, 64, 128, 255)) {
            val effect = TrailRoutePreset.Comet.effect(color = TrailColor((alpha shl 24) or 0x0032D6AD))
            val state = effect.sample(1, effect.durationSeconds * .5)
            val remaining = state.windows.filter { .49 in it.start..it.end }.fold(1.0) { transparency, window ->
                transparency * (1 - alpha / 255.0 * window.opacity)
            }
            assertEquals(alpha / 255.0, 1 - remaining, 1e-9)
        }
    }

    @Test fun routeOptionsKeepTheBaseAndHaveVisibleSeekableForegroundMotion() {
        for (preset in TrailRoutePreset.entries) {
            val effect = preset.effect(duration = 2.seconds)
            assertEquals(2, effect.layers.size)
            assertNull(effect.layers.first().animation)
            for (time in listOf(0.0, .01, .5, 1.1, 1.9, 2.0, 203.1)) {
                assertSame(TrailVisualState.Full, effect.sample(0, time))
                val a = effect.sample(1, time)
                effect.sample(1, 7.0)
                val b = effect.sample(1, time)
                assertEquals(a.windows, b.windows, preset.name)
                assertEquals(a.dashPhase, b.dashPhase, preset.name)
                assertEquals(a.opacity, b.opacity, preset.name)
                assertEquals(listOf(TrailWindow(0.0, 1.0)), effect.sample(1, time, true).windows)
            }
            val a = effect.sample(1, .2); val b = effect.sample(1, 1.1)
            assertTrue(a.windows != b.windows || a.dashPhase != b.dashPhase || a.opacity != b.opacity, preset.name)
            val player = TrailPlayer(effect)
            player.advance(.5); player.pause(); player.advance(50.0)
            assertEquals(.25, player.progress)
            player.replay(); assertEquals(0.0, player.progress)
            val finite = TrailPlayer(preset.effect(duration = 1.seconds, repeat = false))
            finite.advance(5.0); assertEquals(TrailPlaybackStatus.Finished, finite.status)
        }
    }

    @Test fun patternFlowDefaultsToAnActuallyMovingStyle() {
        for (motion in listOf(TrailMotionPreset.DashFlow, TrailMotionPreset.RevealThenFlow)) {
            assertTrue((motion.effect().layers.single().commands.single() as TrailStroke).dash.isNotEmpty())
        }
        for (preset in listOf(TrailRoutePreset.MovingDots, TrailRoutePreset.MovingDashes)) {
            val foreground = preset.effect(width = 8.0).layers.last().commands.single() as TrailStroke
            assertTrue(foreground.dash.isNotEmpty()); assertEquals(8.0, foreground.width)
        }
    }

    @Test fun sweepAndDrawEraseHaveAQuietLoopBoundaryAndLoadingKeepsItsLength() {
        for (preset in listOf(TrailRoutePreset.RouteSweep, TrailRoutePreset.DrawAndErase)) {
            val animation = preset.effect().layers.last().animation!!
            fun visible(p: Double) = animation.sampler.sample(TrailTime(p)).let { state ->
                state.windows.sumOf { (it.end - it.start) * it.opacity } * state.opacity
            }
            assertEquals(0.0, visible(0.0), 1e-9)
            assertEquals(0.0, visible(1.0), 1e-9)
            assertTrue(visible(.5) > .9)
        }
        val loader = TrailRoutePreset.Loading.effect().layers.last().animation!!
        for (p in listOf(0.0, .001, .219, .22, .8, 1.0)) {
            assertEquals(.22, loader.sampler.sample(TrailTime(p)).windows.sumOf { it.end - it.start }, 1e-9)
        }
    }
}
