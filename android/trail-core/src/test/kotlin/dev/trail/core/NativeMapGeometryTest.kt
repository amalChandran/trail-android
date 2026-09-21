package dev.trail.core

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.*
import kotlin.test.*

@RunWith(Parameterized::class)
class NativeMapGeometryTest(private val lat: Double, private val lon: Double, private val north: Boolean,
                            private val progress: Double, private val reverse: Boolean) {
    @Test fun geographicHeadHasAnalyticPositionAndCameraIndependentBearing() {
        val a = TrailCoordinate(lat,lon)
        val b = TrailCoordinate(if(north) lat+2 else lat, if(north) lon else if(lon+2>180) lon-358 else lon+2)
        val geometry = TrailMapGeometry(TrailRoute.direct("test",a,b))
        val pose = geometry.poseAt(progress,direction=if(reverse) TrailDirection.Reverse else TrailDirection.Forward)!!
        fun y(degrees: Double) = ln(tan(PI/4+degrees*PI/360))
        val expectedLat = if(north) atan(sinh(y(lat)+(y(lat+2)-y(lat))*progress))*180/PI else lat
        val expectedLon = if(north) lon else ((lon+2*progress+180)%360)-180
        assertEquals(expectedLat,pose.coordinate.latitude,1e-8)
        assertTrue(abs(TrailGeography.delta(expectedLon,pose.coordinate.longitude))<1e-8)
        assertEquals((if(north) 0.0 else 90.0)+(if(reverse) 180.0 else 0.0),pose.bearing,1e-6)
        val strokes = geometry.strokes(TrailLayer(TrailStyles.solid()),TrailVisualState.reveal(progress),1.0)
        if(progress>0) {
            val end=strokes.last().coordinates.last()
            assertEquals(pose.coordinate.latitude,end.latitude,1e-8)
            assertTrue(abs(TrailGeography.delta(pose.coordinate.longitude,end.longitude))<1e-8)
        } else assertTrue(strokes.isEmpty())
        assertTrue(strokes.all { stroke -> stroke.coordinates.zipWithNext().all { abs(it.second.longitude-it.first.longitude)<=180 } })
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="lat={0}/lon={1}/north={2}/p={3}/reverse={4}")
        fun cases(): List<Array<Any>> = listOf(-60.0,0.0,60.0).flatMap { lat -> listOf(-73.0,179.0).flatMap { lon ->
            listOf(false,true).flatMap { north -> listOf(0.0,.01,.25,.5,.99,1.0).flatMap { p ->
                listOf(false,true).map { reverse -> arrayOf(lat,lon,north,p,reverse) }
            } }
        } }
    }
}

class NativeMapLimitsTest {
    @Test fun staticNativeStrokesReuseTheGeographicBufferAndKeepEveryCorner() {
        val route=TrailRoute("road",(0 until 100_000).map { TrailCoordinate(40.0+it*1e-7,-73.0+(it%2)*1e-5) })
        val geometry=TrailMapGeometry(route)
        val strokes=geometry.strokes(TrailLayer(TrailStyles.cased()),TrailVisualState.Full,1.0)
        assertEquals(2,strokes.size)
        assertSame(route.segments.single(),strokes[0].coordinates)
        assertSame(strokes[0].coordinates,strokes[1].coordinates)
        assertEquals(100_000,strokes[0].coordinates.size)
    }
    @Test fun invalidScaleAndDegenerateGeometryAreHandledExplicitly() {
        val empty=TrailMapGeometry(TrailRoute("empty",emptyList()))
        assertNull(empty.poseAt(.5))
        assertTrue(empty.strokes(TrailLayer(TrailStyles.solid()),TrailVisualState.Full,1.0).isEmpty())
        for(value in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertFailsWith<IllegalArgumentException> {
            empty.strokes(TrailLayer(TrailStyles.solid()),TrailVisualState.Full,value)
        }
    }
}
