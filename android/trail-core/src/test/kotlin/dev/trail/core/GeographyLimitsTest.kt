package dev.trail.core

import org.junit.Assert.*
import org.junit.Test

class GeographyLimitsTest {
    private val origin=TrailCoordinate(0.0,0.0)
    @Test fun distanceHasIndependentKnownSphericalAnchors() {
        // Mean radius × independently known central angles, not another SDK distance function.
        for((point,meters) in listOf(
            TrailCoordinate(0.0,0.0) to 0.0,
            TrailCoordinate(0.0,1.0) to 111195.0802335329,
            TrailCoordinate(1.0,0.0) to 111195.0802335329,
            TrailCoordinate(0.0,90.0) to 10007557.221017962,
            TrailCoordinate(90.0,0.0) to 10007557.221017962,
            TrailCoordinate(0.0,180.0) to 20015114.442035925,
        )) assertEquals(meters,TrailRoute.direct("anchor",origin,point).distanceMeters,0.001)
    }
    @Test fun samplingAndBendRejectInvalidExternalValues() {
        for(steps in listOf(Int.MIN_VALUE,-1,0,1,2049,Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { TrailRoute.arc("arc",origin,origin,steps=steps) }
            assertThrows(IllegalArgumentException::class.java) { TrailRoute.greatCircle("gc",origin,origin,steps=steps) }
        }
        for(bend in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1.01,1.01)) {
            assertThrows(IllegalArgumentException::class.java) { TrailRoute.arc("arc",origin,origin,bend=bend) }
        }
        assertEquals(2049,TrailRoute.arc("max",origin,origin,bend=-1.0,steps=2048).coordinates.size)
    }
    @Test fun routeAndEncodedInputBudgetsAreEnforced() {
        assertEquals(100_000,TrailRoute("max",List(100_000) { origin }).coordinates.size)
        assertThrows(IllegalArgumentException::class.java) { TrailRoute("large",List(100_001) { origin }) }
        val error=assertThrows(IllegalArgumentException::class.java) { TrailRoute.encodedPolyline("large","?".repeat(2_000_001)) }
        assertTrue(error.message!!.contains("budget"))
        assertThrows(IllegalArgumentException::class.java) { TrailRoute.encodedPolyline("points","??".repeat(100_001)) }
    }
    @Test fun routeIdentityAndInputsAreValidatedAndDefensivelyCopied() {
        for(id in listOf(""," ","\n\t")) assertThrows(IllegalArgumentException::class.java) { TrailRoute(id,listOf(origin)) }
        assertThrows(IllegalArgumentException::class.java) { TrailRoute("route",listOf(origin),-1) }
        val input=mutableListOf(origin,TrailCoordinate(1.0,1.0)); val route=TrailRoute("immutable",input)
        input.clear()
        assertEquals(2,route.coordinates.size); assertEquals(2,route.segments.single().size)
        assertThrows(UnsupportedOperationException::class.java) { (route.segments.single() as MutableList).clear() }
        assertTrue(route.distanceMeters>0)
    }
}
