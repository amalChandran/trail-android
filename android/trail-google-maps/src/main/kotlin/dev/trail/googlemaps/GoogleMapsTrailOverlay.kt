package dev.trail.googlemaps

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import dev.trail.compose.*
import dev.trail.core.*

/**
 * Place as a sibling above GoogleMap, with identical bounds. Reads the SDK's live projection,
 * including bearing/tilt, and never replaces camera listeners. It does not consume touch input.
 * Map controls should be placed above this overlay; paths cannot interleave with native labels.
 * Geographic preparation and antimeridian splitting belong to TrailCore, not the SDK adapter.
 */
@Composable fun GoogleMapsTrailOverlay(
    route: TrailRoute,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    effect: TrailEffect? = null,
    playback: TrailPlayback = rememberTrailPlayback(effect ?: remember { TrailEffect() }, route.key),
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    onProjected: (TrailPath?) -> Unit = {},
) {
    val density = LocalDensity.current.density
    val projection = cameraPositionState.projection
    val position = cameraPositionState.position
    val adapter = remember(projection, density) {
        projection?.asTrailProjection(density.toDouble())
    }
    TrailProjectedOverlay(route, adapter, position, modifier, effect, playback, reducedMotion, onProjected)
}

/** Google returns physical screen pixels; Trail's shared renderer consumes logical dp. */
fun Projection.asTrailProjection(density: Double): TrailProjection {
    require(density.isFinite() && density > 0) { "Density must be finite and positive" }
    return TrailProjection {
        // LatLng normalizes +180 to -180. Keep each split seam on its own side of the world.
        val longitude = it.longitude.coerceIn(-180.0 + 1e-7, 180.0 - 1e-7)
        val point = toScreenLocation(LatLng(it.latitude, longitude))
        TrailPoint(point.x / density, point.y / density)
    }
}
