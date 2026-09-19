package dev.trail.effects

import dev.trail.core.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class CatalogContractTest(private val style: TrailStylePreset, private val motion: TrailMotionPreset) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}/{1}") fun cases() = TrailStylePreset.entries.flatMap { s -> TrailMotionPreset.entries.map { m -> arrayOf(s,m) } }
    }
    @Test fun nativeMapPrimitivesPreserveFiniteGeometryForEveryStyleAndMotion() {
        val route=TrailRoute("road",listOf(TrailCoordinate(40.0,-73.0),TrailCoordinate(40.002,-73.0),TrailCoordinate(40.002,-72.998),TrailCoordinate(40.004,-72.998)))
        val geometry=TrailMapGeometry(route)
        val effect=motion.effect(style.style(),2.seconds,repeat=true)
        for(p in listOf(0.0,.2,.8,1.0)) {
            val strokes=geometry.strokes(effect.layers[0],effect.sample(0,p*effect.durationSeconds),2.0)
            assertTrue(strokes.size<=8192)
            assertTrue(strokes.all { it.width.isFinite() && it.width>0 && it.coordinates.size>=2 && it.dashPhase.isFinite() })
        }
    }
    @Test fun seekRepeatReducedMotionAndRecordedCommandsAreStable() {
        val effect=motion.effect(style.style(),2.seconds,repeat=true)
        for (index in effect.layers.indices) {
            for (time in listOf(0.0,.001,.25,1.0,1.999,2.0,2.001,200.25)) {
                val a=effect.sample(index,time); effect.sample(index,.77); val b=effect.sample(index,time)
                assertEquals(a.windows,b.windows); assertEquals(a.opacity,b.opacity); assertEquals(a.head,b.head)
                assertTrue(a.widthScale.isFinite() && a.widthScale in 0.0..16.0)
                assertTrue(a.dashPhase in 0.0..1.0 && a.opacity in 0.0..1.0)
                assertTrue(a.windows.all { it.start in 0.0..1.0 && it.end in it.start..1.0 && it.opacity in 0.0..1.0 })
                assertEquals(effect.sample(index,0.0,true).windows,effect.sample(index,time,true).windows)
            }
            assertTrue(effect.layers[index].commands.size <= 512)
        }
    }
}
