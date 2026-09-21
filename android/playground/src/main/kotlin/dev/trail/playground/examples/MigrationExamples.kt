package dev.trail.playground.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import dev.trail.core.TrailColor
import dev.trail.core.TrailCoordinate
import dev.trail.core.TrailRoute
import dev.trail.core.trailEffect
import dev.trail.googlemaps.GoogleMapsTrail
import kotlin.time.Duration.Companion.seconds

/** App-boundary migration helper: copy every old waypoint, in order, into an immutable route. */
fun migrateLegacyPath(id: String, points: List<LatLng>, revision: Long = 0): TrailRoute =
    TrailRoute(id, points.map { TrailCoordinate(it.latitude, it.longitude) }, revision)

/** Deliberately requires endpoints: never silently discard the middle of a directions route. */
fun migrateLegacyArc(id: String, endpoints: List<LatLng>, revision: Long = 0): TrailRoute {
    require(endpoints.size == 2) {
        "An endpoint arc needs exactly two points. Use migrateLegacyPath to keep every waypoint."
    }
    return TrailRoute.arc(
        id,
        TrailCoordinate(endpoints[0].latitude, endpoints[0].longitude),
        TrailCoordinate(endpoints[1].latitude, endpoints[1].longitude),
        revision = revision,
    )
}

// Named replacement for the old bottom/top paints. Widths are dp; durations are explicit.
val migratedRouteEffect = trailEffect {
    layer { stroke(TrailColor.White, width = 10.0) }
    layer {
        stroke(TrailColor.Blue, width = 6.0)
        reveal(duration = 2.seconds)
    }
}

/** Camera state belongs to the caller. Trail adds native map content without camera listeners. */
@Composable fun MigratedGoogleRoute(
    route: TrailRoute,
    camera: CameraPositionState,
    modifier: Modifier = Modifier,
) {
    GoogleMap(modifier, cameraPositionState = camera) {
        GoogleMapsTrail(route, camera, effect = migratedRouteEffect)
    }
}
