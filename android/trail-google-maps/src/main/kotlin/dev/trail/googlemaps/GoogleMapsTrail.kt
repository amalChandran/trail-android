package dev.trail.googlemaps

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import kotlin.math.pow

/** Up-facing bitmap, centered on the route and flat on the map. Created once, never per frame. */
data class GoogleMapsTrailVehicle(val icon: BitmapDescriptor, val title: String = "Vehicle")

/**
 * Place INSIDE GoogleMap { }. Native polylines and flat markers stay on geographic coordinates
 * while the map renders camera gestures. No sibling Canvas or camera-position feedback loop.
 * Own one playback per binding. Pass active=false when a retained map is offscreen.
 */
@Composable @GoogleMapComposable fun GoogleMapsTrail(
    route: TrailRoute,
    cameraPositionState: CameraPositionState,
    effect: TrailEffect? = null,
    playback: TrailPlayback = rememberTrailPlayback(effect ?: remember { TrailEffect() }, route.key),
    vehicle: GoogleMapsTrailVehicle? = null,
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    active: Boolean = true,
) {
    if (effect != null) SideEffect { playback.configure(effect) }
    val configured = effect ?: playback.effect
    val geometry = remember(route) { TrailMapGeometry(route) }
    val density = LocalDensity.current.density
    val units = TrailMapGeometry.WORLD_METERS / (256 * 2.0.pow(cameraPositionState.position.zoom.toDouble()))
    TrailPlaybackClock(playback, active && geometry.path.length > 0, reducedMotion)
    for ((index, layer) in configured.layers.withIndex()) {
        val frame = if (playback.effect.layers.size != configured.layers.size) configured.sample(index, playback.progress * configured.durationSeconds, reducedMotion)
            else playback.frame(index, reducedMotion)
        val patterned = remember(layer) { layer.commands.any { it is TrailChevrons || it is TrailStroke && it.dash.isNotEmpty() } }
        val strokes = remember(geometry, layer, frame, if (patterned) units else 1.0) { geometry.strokes(layer, frame, units) }
        for (stroke in strokes) {
            val points = remember(stroke.coordinates) { stroke.coordinates.map { LatLng(it.latitude, it.longitude.coerceIn(-180.0 + 1e-7, 180.0 - 1e-7)) } }
            val cap = if (stroke.roundCap) RoundCap() else ButtCap()
            Polyline(points = points, color = Color(stroke.color.argb), width = (stroke.width * density).toFloat(),
                startCap = cap, endCap = cap, jointType = JointType.ROUND, geodesic = false,
                pattern = remember(stroke.dash, stroke.dashPhase, density) { nativeDashPattern(stroke.dash, stroke.dashPhase, density) },
                zIndex = index.toFloat())
        }
    }
    if (vehicle != null) {
        val frame = playback.frame(playback.effect.layers.lastIndex, reducedMotion)
        val pose = geometry.poseAt(if (reducedMotion) 1.0 else frame.head ?: playback.progress,
            direction = if (reducedMotion) TrailDirection.Forward else frame.headDirection)
        if (pose != null) Marker(state = rememberUpdatedMarkerState(LatLng(pose.coordinate.latitude, pose.coordinate.longitude)),
            icon = vehicle.icon, title = vehicle.title, anchor = Offset(.5f, .5f), flat = true,
            rotation = pose.bearing.toFloat(), zIndex = 100f)
    }
}

/** Rotate the cyclic pattern because Google has no dash-offset property. Length is unchanged. */
internal fun nativeDashPattern(dash: List<Double>, phase: Double, density: Float): List<PatternItem>? {
    if (dash.isEmpty()) return null
    val cycle = dash.sum()
    var offset = ((phase % cycle) + cycle) % cycle
    var start = 0
    while (offset >= dash[start] && start < dash.lastIndex) { offset -= dash[start]; start++ }
    val result = ArrayList<PatternItem>(dash.size + 1)
    fun add(index: Int, length: Double) {
        if (length > 1e-9) result.add(if (index % 2 == 0) Dash((length * density).toFloat()) else Gap((length * density).toFloat()))
    }
    add(start, dash[start] - offset)
    for (step in 1 until dash.size) { val index = (start + step) % dash.size; add(index, dash[index]) }
    add(start, offset)
    return result
}
