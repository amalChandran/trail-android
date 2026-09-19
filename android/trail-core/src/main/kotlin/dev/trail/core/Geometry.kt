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
    init {
        require(id.isNotBlank()) { "Route ID must not be blank" }
        require(revision >= 0) { "Revision must not be negative" }
        require(coordinates.size <= 100_000) { "A route supports at most 100,000 coordinates" }
    }
    val key = TrailRouteKey(id, revision)
    val segments: List<List<TrailCoordinate>> = TrailGeography.splitAtDateLine(this.coordinates)
    val distanceMeters: Double = this.coordinates.zipWithNext().sumOf { (a, b) -> TrailGeography.distance(a, b) }
    val bounds: TrailGeoBounds? = TrailGeography.bounds(this.coordinates)
    fun project(projection: (TrailCoordinate) -> TrailPoint): TrailPath = projectIfReady(TrailProjection(projection))!!
    /** All-or-nothing projection prevents drawing a false bridge across an unavailable point. */
    fun projectIfReady(projection: TrailProjection): TrailPath? {
        val points = ArrayList<TrailPoint>(); val breaks = HashSet<Int>()
        for (segment in segments) {
            if (points.isNotEmpty()) breaks.add(points.size)
            for (coordinate in segment) points.add(projection.project(coordinate) ?: return null)
        }
        return TrailPath(points, breaks)
    }
    companion object {
        fun direct(id: String, from: TrailCoordinate, to: TrailCoordinate, revision: Long = 0) = TrailRoute(id, listOf(from, to), revision)
        fun arc(id: String, from: TrailCoordinate, to: TrailCoordinate, bend: Double = 0.25, steps: Int = 128, revision: Long = 0) =
            TrailRoute(id, TrailGeography.arc(from, to, bend, steps), revision)
        fun greatCircle(id: String, from: TrailCoordinate, to: TrailCoordinate, steps: Int = 128, revision: Long = 0) =
            TrailRoute(id, TrailGeography.greatCircle(from, to, steps), revision)
        fun encodedPolyline(id: String, encoded: String, precision: Int = 5, revision: Long = 0) =
            TrailRoute(id, TrailGeography.decodePolyline(encoded, precision), revision)
    }
}

data class TrailRouteKey(val id: String, val revision: Long)
/** The only SDK-facing geometry contract. Return local logical units, or null until ready. */
fun interface TrailProjection { fun project(coordinate: TrailCoordinate): TrailPoint? }
data class TrailPathSlice internal constructor(val points: List<TrailPoint>, val distanceFromStart: Double)

/** Immutable measured polyline. Empty, singleton and repeated-point paths are safe. */
class TrailPath(points: List<TrailPoint>, breakBefore: Set<Int> = emptySet()) {
    val points: List<TrailPoint> = java.util.Collections.unmodifiableList(points.toList())
    val breakBefore: Set<Int> = java.util.Collections.unmodifiableSet(breakBefore.toSet())
    private val distances = DoubleArray(points.size)
    private val contours: List<IntRange>
    val length: Double
    init {
        require(breakBefore.all { it in 1 until points.size }) { "Break indices must refer to a point after the first" }
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            distances[i] = distances[i - 1] + if (i in breakBefore) 0.0 else hypot(b.x - a.x, b.y - a.y)
            require(distances[i].isFinite()) { "Path length overflow" }
        }
        length = distances.lastOrNull() ?: 0.0
        val starts = listOf(0) + breakBefore.sorted()
        contours = if (points.isEmpty()) emptyList() else starts.mapIndexed { i, start -> start until (starts.getOrNull(i + 1) ?: points.size) }
    }
    fun pointAt(fraction: Double): TrailPoint? {
        requireFraction(fraction, "fraction")
        if (points.isEmpty()) return null
        if (length == 0.0 || fraction == 0.0) return points.first()
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
        var end = upperBound(length * fraction).coerceIn(1, points.lastIndex)
        while (end > 1 && distances[end] == distances[end - 1]) end--
        val start = end - 1
        return atan2(points[end].y - points[start].y, points[end].x - points[start].x)
    }
    /** Emits just the requested arc-length range, retaining original intermediate vertices. */
    fun slice(start: Double, end: Double): List<TrailPoint> {
        require(breakBefore.isEmpty()) { "Use slices() for a path with disconnected contours" }
        return slices(start, end).flatMap { it.points }
    }
    /** Each result is a continuous contour. Gaps never count towards distance or get bridged. */
    fun slices(start: Double, end: Double): List<TrailPathSlice> {
        requireFraction(start, "start"); requireFraction(end, "end")
        require(start <= end) { "start must not exceed end" }
        if (length == 0.0 || start == end) return emptyList()
        val result = ArrayList<TrailPathSlice>()
        for (range in contours) {
            val a = max(start * length, distances[range.first]); val b = min(end * length, distances[range.last])
            if (a >= b) continue
            fun point(distance: Double): TrailPoint {
                if (distance <= distances[range.first]) return points[range.first]
                if (distance >= distances[range.last]) return points[range.last]
                val right = upperBound(distance).coerceIn(range.first + 1, range.last)
                val t = ((distance - distances[right - 1]) / (distances[right] - distances[right - 1])).coerceIn(0.0, 1.0)
                return TrailPoint(points[right - 1].x + (points[right].x - points[right - 1].x) * t,
                    points[right - 1].y + (points[right].y - points[right - 1].y) * t)
            }
            val section = ArrayList<TrailPoint>(); section.add(point(a))
            var i = upperBound(a)
            while (i <= range.last && distances[i] < b) section.add(points[i++])
            section.add(point(b))
            result.add(TrailPathSlice(java.util.Collections.unmodifiableList(section), a))
        }
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
        return TrailPath(points.map { TrailPoint((it.x - centerX) * scale + width / 2, (it.y - centerY) * scale + height / 2) }, breakBefore)
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
