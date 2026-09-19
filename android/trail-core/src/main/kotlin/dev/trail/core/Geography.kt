package dev.trail.core

import kotlin.math.*

/** Longitude span follows the smallest enclosing interval, including across ±180°. */
data class TrailGeoBounds(val center: TrailCoordinate, val latitudeSpan: Double, val longitudeSpan: Double)

internal object TrailGeography {
    const val EARTH_RADIUS = 6_371_008.8
    const val MERCATOR_LIMIT = 85.0511287798066
    private fun radians(degrees: Double) = degrees * PI / 180
    private fun degrees(radians: Double) = radians * 180 / PI
    fun wrap(longitude: Double): Double {
        val result = ((longitude + 180) % 360 + 360) % 360 - 180
        return if (result == -180.0 && longitude > 0) 180.0 else result
    }
    fun delta(a: Double, b: Double) = wrap(b - a)
    fun distance(a: TrailCoordinate, b: TrailCoordinate): Double {
        val dLat = radians(b.latitude - a.latitude); val dLon = radians(delta(a.longitude, b.longitude))
        val h = sin(dLat / 2).pow(2) + cos(radians(a.latitude)) * cos(radians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS * atan2(sqrt(h.coerceIn(0.0, 1.0)), sqrt((1 - h).coerceIn(0.0, 1.0)))
    }
    private fun validateSteps(steps: Int) { require(steps in 2..2048) { "Sampling steps must be in 2..2048" } }
    private fun mercatorY(latitude: Double) = ln(tan(PI / 4 + radians(latitude.coerceIn(-MERCATOR_LIMIT, MERCATOR_LIMIT)) / 2))
    private fun inverseY(y: Double) = degrees(2 * atan(exp(y)) - PI / 2)

    /** Decorative quadratic curve in unwrapped Mercator space; not a navigable route. */
    fun arc(from: TrailCoordinate, to: TrailCoordinate, bend: Double, steps: Int): List<TrailCoordinate> {
        validateSteps(steps)
        require(bend.isFinite() && bend in -1.0..1.0) { "Arc bend must be finite and in [-1, 1]" }
        require(abs(from.latitude) <= MERCATOR_LIMIT && abs(to.latitude) <= MERCATOR_LIMIT) { "Mercator arcs require endpoints within ±85.051129° latitude" }
        val ax = radians(from.longitude); val ay = mercatorY(from.latitude)
        val bx = ax + radians(delta(from.longitude, to.longitude)); val by = mercatorY(to.latitude)
        val cx = (ax + bx) / 2 - (by - ay) * bend
        val cy = (ay + by) / 2 + (bx - ax) * bend
        return (0..steps).map { i ->
            when (i) {
                0 -> from; steps -> to
                else -> {
                    val t = i.toDouble() / steps; val s = 1 - t
                    TrailCoordinate(inverseY(s * s * ay + 2 * s * t * cy + t * t * by), wrap(degrees(s * s * ax + 2 * s * t * cx + t * t * bx)))
                }
            }
        }
    }
    /** Spherical shortest connection. Exact antipodes need a caller-selected waypoint. */
    fun greatCircle(from: TrailCoordinate, to: TrailCoordinate, steps: Int): List<TrailCoordinate> {
        validateSteps(steps)
        fun vector(c: TrailCoordinate): DoubleArray {
            val lat = radians(c.latitude); val lon = radians(c.longitude)
            return doubleArrayOf(cos(lat) * cos(lon), cos(lat) * sin(lon), sin(lat))
        }
        val a = vector(from); val b = vector(to)
        val dot = a.indices.sumOf { a[it] * b[it] }.coerceIn(-1.0, 1.0)
        val cross = doubleArrayOf(a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
        val magnitude = sqrt(cross.sumOf { it * it })
        require(!(magnitude < 1e-12 && dot < 0)) { "Antipodal endpoints have no unique great circle; provide an intermediate waypoint" }
        val angle = atan2(magnitude, dot)
        return (0..steps).map { i ->
            when (i) {
                0 -> from; steps -> to
                else -> {
                    val t = i.toDouble() / steps
                    if (magnitude < 1e-12) TrailCoordinate(from.latitude + (to.latitude-from.latitude)*t, wrap(from.longitude + delta(from.longitude,to.longitude)*t))
                    else {
                        val p = a.indices.map { cos(angle*t)*a[it] + sin(angle*t)*(b[it]-dot*a[it])/magnitude }
                        TrailCoordinate(degrees(atan2(p[2], hypot(p[0],p[1]))), wrap(degrees(atan2(p[1],p[0]))))
                    }
                }
            }
        }
    }
    fun bounds(points: List<TrailCoordinate>): TrailGeoBounds? {
        if (points.isEmpty()) return null
        val longitudes = points.map { (it.longitude + 360) % 360 }.sorted()
        var largestGap = -1.0; var beginning = longitudes.first()
        for (i in longitudes.indices) {
            val next = if (i == longitudes.lastIndex) longitudes.first() + 360 else longitudes[i+1]
            if (next-longitudes[i] > largestGap) { largestGap = next-longitudes[i]; beginning = next % 360 }
        }
        val south = points.minOf { it.latitude }; val north = points.maxOf { it.latitude }
        val span = (360-largestGap).coerceIn(0.0,360.0)
        return TrailGeoBounds(TrailCoordinate((south+north)/2,wrap(beginning+span/2)), north-south, span)
    }
    fun splitAtDateLine(points: List<TrailCoordinate>): List<List<TrailCoordinate>> {
        if (points.isEmpty()) return emptyList()
        val result = ArrayList<List<TrailCoordinate>>(); var current = arrayListOf(points.first())
        for ((a,b) in points.zipWithNext()) {
            if (abs(b.longitude-a.longitude) > 180) {
                val d = delta(a.longitude,b.longitude)
                if (abs(d) < 1e-12) {
                    result.add(current); current = arrayListOf(b); continue
                }
                val boundary = if (d > 0) 180.0 else -180.0
                val t = ((boundary-a.longitude)/d).coerceIn(0.0,1.0)
                val lat = inverseY(mercatorY(a.latitude)+(mercatorY(b.latitude)-mercatorY(a.latitude))*t)
                current.add(TrailCoordinate(lat,boundary)); result.add(current)
                current = arrayListOf(TrailCoordinate(lat,-boundary))
            }
            current.add(b)
        }
        result.add(current)
        return java.util.Collections.unmodifiableList(result.map { java.util.Collections.unmodifiableList(it.toList()) })
    }
    fun decodePolyline(encoded: String, precision: Int): List<TrailCoordinate> {
        require(precision == 5 || precision == 6) { "Polyline precision must be 5 or 6" }
        require(encoded.length <= 2_000_000) { "Encoded polyline exceeds the input budget" }
        var index = 0
        fun component(): Long {
            var result = 0L; var shift = 0
            while (true) {
                require(index < encoded.length && shift <= 30) { "Truncated or overflowing encoded polyline at character $index" }
                val byte = encoded[index++].code - 63
                require(byte in 0..63) { "Invalid encoded polyline character at ${index-1}" }
                result = result or ((byte and 31).toLong() shl shift)
                if (byte < 32) break
                shift += 5
            }
            return if ((result and 1) != 0L) (result shr 1).inv() else result shr 1
        }
        val scale = 10.0.pow(precision); var lat = 0L; var lon = 0L
        val output = ArrayList<TrailCoordinate>()
        while (index < encoded.length) {
            require(output.size < 100_000) { "A route supports at most 100,000 coordinates" }
            lat += component(); lon += component()
            output.add(TrailCoordinate(lat/scale,lon/scale))
        }
        return output
    }
}
