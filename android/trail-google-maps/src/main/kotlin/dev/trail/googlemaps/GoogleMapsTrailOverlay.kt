package dev.trail.googlemaps

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import dev.trail.compose.*
import dev.trail.core.*

/**
 * Place as a sibling above GoogleMap, with identical bounds. Reads the SDK's live projection,
 * including bearing/tilt, and never replaces camera listeners. It does not consume touch input.
 * Map controls should be placed above this overlay; paths cannot interleave with native labels.
 * Paths crossing the antimeridian are rejected explicitly in this alpha (no incorrect world-spanning stroke).
 */
@Composable fun GoogleMapsTrailOverlay(
    route: TrailRoute,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    effect: TrailEffect = remember { TrailEffect() },
    playback: TrailPlayback = rememberTrailPlayback(effect, route.id to route.revision),
    reducedMotion: Boolean = rememberSystemReducedMotion(),
) {
    require(route.coordinates.zipWithNext().none { (a, b) -> kotlin.math.abs(a.longitude - b.longitude) > 180 }) {
        "Antimeridian routes require splitting into separate bindings in this alpha"
    }
    val density = LocalDensity.current.density
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val projection = cameraPositionState.projection
    val position = cameraPositionState.position
    val path = remember(route, projection, position, density, viewport) {
        if (projection == null) TrailPath(emptyList()) else route.project {
            val point = projection.toScreenLocation(LatLng(it.latitude, it.longitude))
            TrailPoint(point.x / density.toDouble(), point.y / density.toDouble())
        }
    }
    TrailCanvas(path, modifier.onSizeChanged { viewport = it }, effect, playback, fit = false,
        reducedMotion = reducedMotion, active = projection != null)
}
