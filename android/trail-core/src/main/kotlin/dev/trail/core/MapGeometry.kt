package dev.trail.core

import kotlin.math.*

/** A geographic pose; bearing is clockwise from north, independent of the camera. */
data class TrailMapPose(val coordinate: TrailCoordinate, val bearing: Double)

/** A native-map drawing primitive. Dimensions and dash phase are dp / Apple points. */
data class TrailMapStroke(
    val coordinates: List<TrailCoordinate>, val color: TrailColor, val width: Double,
    val dash: List<Double>, val dashPhase: Double, val roundCap: Boolean,
)

/**
 * Prepared Mercator geometry for native map layers. No screen projection, camera listener,
 * network request or clock. Keep this value for the lifetime of a route.
 * Animation distance is Mercator arc length; intermediate road vertices are retained.
 */
class TrailMapGeometry(val route: TrailRoute) {
    val path: TrailPath = route.project(::point)

    fun poseAt(fraction: Double, headingWindow: Double = path.length * .02,
               direction: TrailDirection = TrailDirection.Forward): TrailMapPose? =
        path.poseAt(fraction, headingWindow, direction)?.let {
            TrailMapPose(coordinate(it.point), ((it.headingRadians * 180 / PI + 90) % 360 + 360) % 360)
        }

    /** Cache static layer results. Only patterned styles need unitsPerPoint after zoom changes. */
    fun strokes(layer: TrailLayer, state: TrailVisualState, unitsPerPoint: Double): List<TrailMapStroke> {
        require(unitsPerPoint.isFinite() && unitsPerPoint > 0) { "Map units per point must be finite and positive" }
        if (path.length == 0.0 || state.opacity == 0.0 || state.widthScale == 0.0) return emptyList()
        val result = ArrayList<TrailMapStroke>()
        fun add(points: List<TrailPoint>, color: TrailColor, width: Double, dash: List<Double> = emptyList(), phase: Double = 0.0, cap: Boolean = true) {
            for (segment in TrailGeography.splitAtDateLine(points.map(::coordinate))) {
                if (segment.size > 1) result.add(TrailMapStroke(segment, color, width, dash, phase, cap))
            }
            require(result.size <= 8192) { "Native map layer exceeds 8192 primitives; simplify the style or its visible windows" }
        }
        for (command in layer.commands) when (command) {
            is TrailStroke -> for (window in state.windows) {
                val start = max(command.start, window.start); val end = min(command.end, window.end)
                if (start >= end || window.opacity == 0.0) continue
                val cycle = command.dash.sum()
                if (start == 0.0 && end == 1.0 && cycle == 0.0) {
                    // Static route/casing layers keep the original buffers even on a 100k-point route.
                    for (segment in route.segments) if (segment.size > 1) result.add(TrailMapStroke(segment,
                        command.color.withOpacity(state.opacity * window.opacity), command.width * state.widthScale,
                        emptyList(), 0.0, command.roundCap))
                    continue
                }
                for (slice in path.slices(start, end)) add(slice.points,
                    command.color.withOpacity(state.opacity * window.opacity), command.width * state.widthScale,
                    command.dash, if (cycle == 0.0) 0.0 else (slice.distanceFromStart / unitsPerPoint - state.dashPhase * cycle) % cycle,
                    command.roundCap)
            }
            is TrailChevrons -> {
                val spacing = command.spacing * unitsPerPoint
                val count = min(2048.0, floor(path.length / spacing)).toInt()
                for (i in 0 until count) {
                    val fraction = ((i + .5 + state.dashPhase) * spacing / path.length) % 1.0
                    val window = state.windows.firstOrNull { fraction >= it.start && fraction <= it.end } ?: continue
                    val pose = path.poseAt(fraction, 0.0) ?: continue
                    val half = command.size * state.widthScale * unitsPerPoint / 2
                    fun transformed(x: Double, y: Double) = TrailPoint(
                        pose.point.x + x * cos(pose.headingRadians) - y * sin(pose.headingRadians),
                        pose.point.y + x * sin(pose.headingRadians) + y * cos(pose.headingRadians))
                    add(listOf(transformed(-half, -half), transformed(half, 0.0), transformed(-half, half)),
                        command.color.withOpacity(state.opacity * window.opacity), command.size * .25 * state.widthScale)
                }
            }
        }
        return result
    }

    companion object {
        const val WORLD_METERS = 40_075_016.68557849
        private const val RADIUS = WORLD_METERS / (2 * PI)
        internal fun point(c: TrailCoordinate): TrailPoint {
            val lat = c.latitude.coerceIn(-TrailGeography.MERCATOR_LIMIT, TrailGeography.MERCATOR_LIMIT) * PI / 180
            return TrailPoint(c.longitude * PI / 180 * RADIUS, -ln(tan(PI / 4 + lat / 2)) * RADIUS)
        }
        internal fun coordinate(p: TrailPoint) = TrailCoordinate(
            (atan(sinh(-p.y / RADIUS)) * 180 / PI).coerceIn(-90.0, 90.0), TrailGeography.wrap(p.x / RADIUS * 180 / PI))
    }
}
