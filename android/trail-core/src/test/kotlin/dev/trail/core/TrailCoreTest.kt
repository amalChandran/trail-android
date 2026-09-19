package dev.trail.core

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TrailCoreTest {
    private val path = TrailPath(listOf(TrailPoint(0.0, 0.0), TrailPoint(3.0, 0.0), TrailPoint(3.0, 4.0)))
    @Test fun arcLengthInterpolationUsesDistanceNotVertexCount() {
        assertEquals(7.0, path.length)
        assertEquals(TrailPoint(3.0, 0.5), path.pointAt(0.5))
        val slice = path.slice(0.2, 0.8)
        assertEquals(3, slice.size); assertEquals(1.4, slice.first().x, 1e-12)
        assertEquals(TrailPoint(3.0, 0.0), slice[1]); assertEquals(2.6, slice.last().y, 1e-12)
        assertEquals(Math.PI / 2, path.tangentAt(0.8), 1e-12)
    }
    @Test fun emptyAndDegenerateGeometryIsSafe() {
        assertNull(TrailPath(emptyList()).pointAt(0.5))
        val repeated = TrailPath(List(4) { TrailPoint(2.0, 3.0) })
        assertEquals(0.0, repeated.length); assertEquals(TrailPoint(2.0, 3.0), repeated.pointAt(0.2)); assertTrue(repeated.slice(0.0, 1.0).isEmpty())
        assertTrue(path.fitted(0.0, 0.0).points.all { it.x.isFinite() && it.y.isFinite() })
    }
    @Test fun geometryAndCoordinatesValidateInputs() {
        assertFailsWith<IllegalArgumentException> { TrailCoordinate(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { TrailCoordinate(0.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { TrailPoint(Double.POSITIVE_INFINITY, 0.0) }
        assertFailsWith<IllegalArgumentException> { path.pointAt(-0.1) }
        assertFailsWith<IllegalArgumentException> { path.slice(0.8, 0.3) }
        assertFailsWith<IllegalArgumentException> { TrailRoute("", emptyList()) }
    }
    @Test fun geometryCopiesMutableInput() {
        val source = mutableListOf(TrailPoint(0.0, 0.0), TrailPoint(1.0, 0.0))
        val geometry = TrailPath(source); source.clear()
        assertEquals(1.0, geometry.length); assertEquals(2, geometry.points.size)
        assertFailsWith<UnsupportedOperationException> { (geometry.points as MutableList).clear() }
    }
    @Test fun nonLoopingPlaybackCompletesAtExactEndpointAndDoesNotAdvancePaused() {
        val effect = TrailEffect(animation = TrailAnimations.reveal(2.seconds))
        val player = TrailPlayer(effect)
        player.advance(0.5); player.pause(); player.advance(10.0)
        assertEquals(0.25, player.progress)
        player.play(); player.advance(5.0)
        assertEquals(TrailPlaybackStatus.Finished, player.status); assertEquals(1.0, player.frame(0).head)
        player.play(); assertEquals(0.0, player.progress)
    }
    @Test fun loopingBoundaryIsZeroButExplicitSeekEndIsOne() {
        val player = TrailPlayer(TrailEffect(animation = TrailAnimations.reveal(2.seconds, repeat = true)))
        player.advance(2.0); assertEquals(0.0, player.frame(0).head)
        player.advance(0.5); player.pause(); assertEquals(0.25, player.progress)
        player.seek(1.0); assertEquals(1.0, player.frame(0).head)
        player.play(); player.advance(0.2); assertEquals(0.1, player.progress, 1e-12)
    }
    @Test fun reconfigurationPreservesProgressUnlessResetRequested() {
        val player = TrailPlayer(TrailEffect(animation = TrailAnimations.reveal(2.seconds)))
        player.advance(0.5)
        player.configure(TrailEffect(animation = TrailAnimations.reveal(8.seconds)))
        assertEquals(0.25, player.progress); assertEquals(2.0, player.elapsedSeconds)
        player.configure(player.effect, reset = true); assertEquals(0.0, player.progress)
    }
    @Test fun invalidPluginOutputAndClockValuesFailEarly() {
        assertFailsWith<IllegalArgumentException> { TrailVisualState(opacity = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { TrailVisualState(widthScale = -1.0) }
        assertFailsWith<IllegalArgumentException> { TrailWindow(0.8, 0.2) }
        assertFailsWith<IllegalArgumentException> { TrailAnimations.reveal(Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { TrailPlayer().advance(-1.0) }
        assertFailsWith<IllegalArgumentException> { TrailPlayer().seek(Double.NaN) }
    }
    @Test fun stylesPrepareOnceAndExpiredContextsCannotBeRetained() {
        var calls = 0; var retained: TrailDrawContext? = null
        val effect = TrailEffect(TrailLineStyle { calls++; retained = it; it.stroke() }, TrailAnimations.reveal())
        repeat(100) { effect.sample(0, it / 100.0) }
        assertEquals(1, calls)
        assertFailsWith<IllegalStateException> { retained!!.stroke() }
        assertFailsWith<IllegalArgumentException> { TrailEffect(TrailStyles.solid(width = -1.0)) }
        assertFailsWith<IllegalArgumentException> { TrailEffect(TrailLineStyle { it.stroke(dash = listOf(1.0)) }) }
    }
    @Test fun dslHasSafeDefaultsAndExplicitLayerOrdering() {
        val defaults = trailEffect { }
        assertEquals(0.0, defaults.durationSeconds)
        val effect = trailEffect {
            layer { stroke(TrailColor.White, 10.0) }
            layer { stroke(TrailColor.Blue, 5.0); reveal(2.seconds) }
        }
        assertEquals(2, effect.layers.size)
        assertEquals(TrailColor.White, (effect.layers.first().commands.first() as TrailStroke).color)
        assertFailsWith<IllegalArgumentException> { trailEffect { reveal(); reveal() } }
        assertFailsWith<IllegalArgumentException> { trailEffect { stroke(); stroke() } }
    }
    @Test fun reducedMotionStopsAtIntentionalStaticPresentation() {
        val reveal = TrailEffect(animation = TrailAnimations.reveal())
        val erase = TrailEffect(animation = TrailAnimations.erase())
        assertEquals(1.0, reveal.sample(0, 0.1, true).windows.single().end)
        assertTrue(erase.sample(0, 0.1, true).windows.isEmpty())
    }
    @Test fun propertyLikeGeometrySweepNeverReturnsOutsideBounds() {
        val random = kotlin.random.Random(42)
        repeat(100) {
            val points = List(20) { TrailPoint(random.nextDouble(500.0), random.nextDouble(500.0)) }
            val route = TrailPath(points)
            for (step in 0..100) {
                val point = route.pointAt(step / 100.0)!!
                assertTrue(point.x in 0.0..500.0 && point.y in 0.0..500.0)
            }
        }
    }
}
