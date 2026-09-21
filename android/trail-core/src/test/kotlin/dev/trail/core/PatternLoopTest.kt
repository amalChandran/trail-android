package dev.trail.core

import kotlin.test.*

class PatternLoopTest {
    @Test fun chevronsAtCycleEndMatchCycleStartOnRoutesWithPartialSpacing() {
        for (longitude in listOf(.0001, .001, .001234, 1.0)) {
            val geometry = TrailMapGeometry(TrailRoute.direct("route", TrailCoordinate(0.0, 0.0), TrailCoordinate(0.0, longitude)))
            val layer = TrailLayer(TrailLineStyle { it.chevrons(spacing = 28.0) })
            val start = geometry.strokes(layer, TrailVisualState(dashPhase = 0.0), 1.0)
            val end = geometry.strokes(layer, TrailVisualState(dashPhase = 1.0), 1.0)
            assertEquals(start, end, "Route length ${geometry.path.length}")
        }
    }
}
