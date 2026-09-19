package dev.trail.core

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs
import kotlin.test.*

@RunWith(Parameterized::class)
class RouteTransitionTest(private val latitude: Double, private val longitude: Double, private val progress: Double) {
    @Test fun morphKeepsEndpointsAndResolvesExactRoadGeometryIncludingWorldWrap() {
        val a=TrailCoordinate(latitude,longitude)
        val endLon=TrailGeography.wrap(longitude+2)
        val b=TrailCoordinate(latitude+1,endLon)
        val road=TrailRoute("road",listOf(a,TrailCoordinate(latitude+.3,longitude),TrailCoordinate(latitude+.3,endLon),b),revision=7)
        val arc=TrailRoute.arc("loading",a,b)
        val morph=TrailRouteMorph(arc,road)
        val frame=morph.routeAt(progress)
        assertEquals(a.latitude,frame.coordinates.first().latitude,1e-8)
        assertEquals(b.latitude,frame.coordinates.last().latitude,1e-8)
        assertTrue(abs(TrailGeography.delta(a.longitude,frame.coordinates.first().longitude))<1e-8)
        assertTrue(abs(TrailGeography.delta(b.longitude,frame.coordinates.last().longitude))<1e-8)
        for(segment in frame.segments) assertTrue(segment.zipWithNext().all { abs(it.second.longitude-it.first.longitude)<=180 })
        if(progress==0.0) assertSame(arc,frame)
        if(progress==1.0) { assertSame(road,frame); assertEquals(4,frame.coordinates.size); assertEquals(7L,frame.revision) }
        val state=TrailRouteTransition(a,b)
        assertTrue(state.resolve(state.request,road))
        state.advance(state.durationSeconds*progress)
        assertEquals(if(progress==1.0) TrailRoutePhase.Ready else TrailRoutePhase.Morphing,state.phase)
        if(progress==1.0) assertSame(road,state.route)
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="lat={0}/lon={1}/p={2}")
        fun cases(): List<Array<Any>> = listOf(-60.0,0.0,60.0).flatMap { lat -> listOf(-73.0,179.0).flatMap { lon ->
            listOf(0.0,.01,.25,.5,.75,.99,1.0).map { arrayOf(lat,lon,it) }
        } }
    }
}
class RouteRequestLifecycleTest {
    private val a=TrailCoordinate(40.0,-73.0)
    private val b=TrailCoordinate(40.001,-73.002)
    private val route=TrailRoute.direct("road",a,b)
    @Test fun staleDuplicateAndForeignResponsesCannotMutateCurrentRequest() {
        val state=TrailRouteTransition(a,b); val old=state.request
        val current=state.begin(a,b)
        assertFalse(state.resolve(old,route)); assertFalse(state.fail(old)); assertFalse(state.cancel(old))
        assertFalse(state.resolve(TrailRouteTransition(a,b).request,route))
        assertEquals(TrailRoutePhase.Loading,state.phase)
        assertTrue(state.resolve(current,route)); assertFalse(state.resolve(current,route)); assertFalse(state.fail(current))
        assertFalse(state.cancel(current)); state.advance(100.0)
        assertSame(route,state.route); assertEquals(TrailRoutePhase.Ready,state.phase)
    }
    @Test fun failureCancellationRetryAndReducedMotionHaveDeterministicTerminalStates() {
        val state=TrailRouteTransition(a,b)
        assertTrue(state.fail(state.request)); assertFalse(state.resolve(state.request,route))
        val retry=state.begin(a,b); assertTrue(state.cancel(retry)); assertFalse(state.fail(retry))
        val next=state.begin(a,b); assertTrue(state.resolve(next,route,reducedMotion=true))
        assertSame(route,state.route); assertEquals(TrailRoutePhase.Ready,state.phase)
        state.begin(a,b); state.resolve(state.request,route); state.advance(.1); state.advance(0.0,reducedMotion=true)
        assertSame(route,state.route)
    }
    @Test fun newRequestInterruptsMorphWithoutOldTimeOrGeometryLeaking() {
        val state=TrailRouteTransition(a,b); state.resolve(state.request,route); state.advance(.2)
        val next=state.begin(b,a); val arc=state.route; state.advance(30.0)
        assertSame(arc,state.route); assertEquals(TrailRoutePhase.Loading,state.phase)
        assertTrue(state.resolve(next,TrailRoute.direct("reverse",b,a)))
        state.advance(state.durationSeconds); assertEquals(b,state.route.coordinates.first())
    }
    @Test fun malformedAndMismatchedResponsesLeaveLoadingStateRecoverable() {
        val state=TrailRouteTransition(a,b)
        for(bad in listOf(TrailRoute("empty",emptyList()),TrailRoute.direct("zero",a,a),TrailRoute.direct("wrong",TrailCoordinate(0.0,0.0),TrailCoordinate(1.0,1.0)))) {
            assertFailsWith<IllegalArgumentException> { state.resolve(state.request,bad) }
            assertEquals(TrailRoutePhase.Loading,state.phase)
        }
        assertTrue(state.resolve(state.request,route))
        for(delta in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertFailsWith<IllegalArgumentException> { state.advance(delta) }
    }
}
