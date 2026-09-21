package dev.trail.playground

import com.google.android.gms.maps.model.LatLng
import dev.trail.core.TrailCoordinate
import dev.trail.playground.examples.migrateLegacyArc
import dev.trail.playground.examples.migrateLegacyPath
import org.junit.Assert.*
import org.junit.Test

class MigrationTest {
    @Test fun pathKeepsTurnsRepeatedWaypointsAndOrder() {
        val points = listOf(LatLng(40.75, -73.98), LatLng(40.75, -73.97),
            LatLng(40.75, -73.97), LatLng(40.76, -73.97))
        val route = migrateLegacyPath("trip", points, revision = 7)
        assertEquals(points.map { TrailCoordinate(it.latitude, it.longitude) }, route.coordinates)
        assertEquals("trip", route.key.id)
        assertEquals(7L, route.key.revision)
    }

    @Test fun laterLegacyListMutationCannotMoveAnExistingRoute() {
        val points = mutableListOf(LatLng(0.0, 0.0), LatLng(1.0, 1.0))
        val original = migrateLegacyPath("trip", points)
        points[1] = LatLng(2.0, 2.0)
        val replacement = migrateLegacyPath("trip", points, revision = 1)
        points.clear()
        assertEquals(TrailCoordinate(1.0, 1.0), original.coordinates.last())
        assertEquals(TrailCoordinate(2.0, 2.0), replacement.coordinates.last())
        assertNotEquals(original.key, replacement.key)
    }

    @Test fun anArcRejectsAFullRouteInsteadOfDroppingItsTurns() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            migrateLegacyArc("trip", listOf(LatLng(0.0, 0.0), LatLng(1.0, 0.0), LatLng(1.0, 1.0)))
        }
        assertTrue(error.message!!.contains("migrateLegacyPath"))
    }

    @Test fun twoPointArcKeepsRequestedEndpointsAndRevision() {
        val route = migrateLegacyArc("flight", listOf(LatLng(10.0, 179.0), LatLng(11.0, -179.0)), 2)
        assertEquals(TrailCoordinate(10.0, 179.0), route.coordinates.first())
        assertEquals(11.0, route.coordinates.last().latitude, 1e-8)
        assertEquals(-179.0, route.coordinates.last().longitude, 1e-8)
        assertEquals(129, route.coordinates.size)
        assertEquals(2L, route.revision)
        assertEquals(2, route.segments.size)
    }

    @Test fun clearingTheLegacyListProducesSafeEmptyGeometry() {
        val route = migrateLegacyPath("cleared", emptyList())
        assertTrue(route.coordinates.isEmpty())
        assertNull(route.bounds)
    }
}
