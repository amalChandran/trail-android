package dev.trail.effects

import dev.trail.core.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class EffectsTest {
    @Test fun movingHeadsReportReverseTravelAndStayAtArrivalDuringTrailingEffects() {
        val ping=TrailMotionPreset.PingPong.animation(1.seconds,repeat=false)
        val forward=ping.sampler.sample(TrailTime(.25)); val reverse=ping.sampler.sample(TrailTime(.75))
        assertEquals(forward.head,reverse.head)
        assertEquals(TrailDirection.Forward,forward.headDirection); assertEquals(TrailDirection.Reverse,reverse.headDirection)
        for(motion in listOf(TrailMotionPreset.RevealThenFlow,TrailMotionPreset.DrawAndErase)) {
            for(p in listOf(.5,.75,1.0)) assertEquals(1.0,motion.animation().sampler.sample(TrailTime(p)).head)
        }
    }
    @Test fun everyPresetIsBoundedAndSupportsSeekingInAnyOrder() {
        for (style in TrailStylePreset.entries) for (motion in TrailMotionPreset.entries) {
            val effect = motion.effect(style.style(), 1.seconds, repeat = false)
            for (p in listOf(0.0, 0.9, 0.25, 1.0, 0.5, 0.001)) {
                for (layer in effect.layers.indices) {
                    val a = effect.sample(layer, p); effect.sample(layer, 0.8); val b = effect.sample(layer, p)
                    assertEquals(a.windows, b.windows, "$style / $motion")
                    assertTrue(a.opacity in 0.0..1.0); assertTrue(a.windows.size <= 256)
                }
            }
        }
    }
    @Test fun gradientAndTaperAreAlongRouteRatherThanScreenAxes() {
        val gradient = TrailEffect(TrailStylePreset.Gradient.style()).layers.single().commands.map { it as TrailStroke }
        assertEquals(64, gradient.size); assertEquals(0.0, gradient.first().start); assertEquals(1.0, gradient.last().end)
        assertEquals(TrailColor.Mint, gradient.first().color); assertEquals(TrailColor.Coral, gradient.last().color)
        val taper = TrailEffect(TrailStylePreset.Tapered.style()).layers.single().commands.map { it as TrailStroke }
        assertTrue(taper.zipWithNext().all { (a, b) -> a.width <= b.width })
    }
    @Test fun spotlightKeepsAnIndependentStaticBase() {
        val effect = TrailMotionPreset.Spotlight.effect()
        assertEquals(2, effect.layers.size)
        assertEquals(listOf(TrailWindow(0.0, 1.0)), effect.sample(0, 0.4).windows)
        assertTrue(effect.sample(1, 0.4).windows.size > 1)
    }
}
