package dev.trail.core

import kotlin.math.*

/** A point in a local, logical-unit coordinate space (dp on Android, points on iOS). */
data class TrailPoint(val x: Double, val y: Double) {
    init { require(x.isFinite() && y.isFinite()) { "Point coordinates must be finite" } }
}

data class TrailCoordinate(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitude must be in [-90, 90]" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitude must be in [-180, 180]" }
    }
}

/** Geographic input. Change revision when replacing a route with the same ID. */
class TrailRoute(val id: String, coordinates: List<TrailCoordinate>, val revision: Long = 0) {
    val coordinates: List<TrailCoordinate> = java.util.Collections.unmodifiableList(coordinates.toList())
    init { require(id.isNotBlank()) { "Route ID must not be blank" } }
    fun project(projection: (TrailCoordinate) -> TrailPoint): TrailPath = TrailPath(coordinates.map(projection))
}

/** Immutable measured polyline. Empty, singleton and repeated-point paths are safe. */
class TrailPath(points: List<TrailPoint>) {
    val points: List<TrailPoint> = java.util.Collections.unmodifiableList(points.toList())
    private val distances = DoubleArray(points.size)
    val length: Double
    init {
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            distances[i] = distances[i - 1] + hypot(b.x - a.x, b.y - a.y)
            require(distances[i].isFinite()) { "Path length overflow" }
        }
        length = distances.lastOrNull() ?: 0.0
    }
    fun pointAt(fraction: Double): TrailPoint? {
        requireFraction(fraction, "fraction")
        if (points.isEmpty()) return null
        if (length == 0.0) return points.first()
        if (fraction == 1.0) return points.last()
        val distance = length * fraction
        val end = upperBound(distance).coerceIn(1, points.lastIndex)
        val start = end - 1
        val span = distances[end] - distances[start]
        val t = if (span == 0.0) 0.0 else (distance - distances[start]) / span
        return TrailPoint(points[start].x + (points[end].x - points[start].x) * t,
            points[start].y + (points[end].y - points[start].y) * t)
    }
    fun tangentAt(fraction: Double): Double {
        requireFraction(fraction, "fraction")
        if (length == 0.0) return 0.0
        val end = upperBound(length * fraction).coerceIn(1, points.lastIndex)
        var start = end - 1
        while (start > 0 && points[start] == points[end]) start--
        return atan2(points[end].y - points[start].y, points[end].x - points[start].x)
    }
    /** Emits just the requested arc-length range, retaining original intermediate vertices. */
    fun slice(start: Double, end: Double): List<TrailPoint> {
        requireFraction(start, "start"); requireFraction(end, "end")
        require(start <= end) { "start must not exceed end" }
        if (length == 0.0 || start == end) return emptyList()
        val result = ArrayList<TrailPoint>()
        result.add(pointAt(start)!!)
        var i = upperBound(start * length)
        while (i < points.size && distances[i] < end * length) result.add(points[i++])
        result.add(pointAt(end)!!)
        return result
    }
    /** Fits a local path into a viewport without distorting its aspect ratio. */
    fun fitted(width: Double, height: Double, padding: Double = 16.0): TrailPath {
        require(width.isFinite() && height.isFinite() && width >= 0 && height >= 0)
        require(padding.isFinite() && padding >= 0)
        if (points.isEmpty()) return this
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
        val w = (width - 2 * padding).coerceAtLeast(0.0)
        val h = (height - 2 * padding).coerceAtLeast(0.0)
        val scale = min(if (maxX == minX) Double.POSITIVE_INFINITY else w / (maxX - minX),
            if (maxY == minY) Double.POSITIVE_INFINITY else h / (maxY - minY)).let { if (it.isFinite()) it else 1.0 }
        val centerX = (minX + maxX) / 2; val centerY = (minY + maxY) / 2
        return TrailPath(points.map { TrailPoint((it.x - centerX) * scale + width / 2, (it.y - centerY) * scale + height / 2) })
    }
    private fun upperBound(value: Double): Int {
        var low = 0; var high = distances.size
        while (low < high) { val mid = (low + high) ushr 1; if (distances[mid] <= value) low = mid + 1 else high = mid }
        return low
    }
}

internal fun requireFraction(value: Double, name: String) {
    require(value.isFinite() && value in 0.0..1.0) { "$name must be finite and in [0, 1]" }
}
