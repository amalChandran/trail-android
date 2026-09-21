package dev.trail.core

import kotlin.math.*
import kotlin.test.*

class PoseTest {
    private val path=TrailPath(listOf(TrailPoint(0.0,0.0),TrailPoint(100.0,0.0),TrailPoint(100.0,100.0)))
    @Test fun invalidWindowsAndFractionsAreRejected() {
        for(w in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY)) assertFailsWith<IllegalArgumentException> { path.poseAt(.5,w) }
        for(p in listOf(-.1,1.1,Double.NaN)) assertFailsWith<IllegalArgumentException> { path.poseAt(p) }
    }
    @Test fun steeringIsContinuousThroughACornerWithoutMovingOffTheRoute() {
        var previous=0.0
        for(i in 0..1000) {
            val pose=path.poseAt(i/1000.0,20.0)!!
            assertTrue(pose.headingRadians>=previous-1e-9)
            assertTrue(pose.headingRadians-previous<.021)
            assertEquals(path.pointAt(i/1000.0),pose.point)
            previous=pose.headingRadians
        }
        assertEquals(PI/2,previous,1e-9)
    }
}
