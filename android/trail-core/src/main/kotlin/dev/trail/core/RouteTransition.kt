package dev.trail.core

import kotlin.math.*

/** Prepared correspondence; target vertices are retained, so the final route never cuts corners. */
class TrailRouteMorph(val from: TrailRoute, val to: TrailRoute, samples: Int = 128) {
    private val start: List<TrailPoint>
    private val end: List<TrailPoint>
    init {
        require(from.coordinates.size >= 2 && to.coordinates.size >= 2) { "A morph needs two nonempty routes with at least two points" }
        require(samples in 2..2048) { "Morph samples must be in 2..2048" }
        val a = unwrapped(from.coordinates)
        val rawB = unwrapped(to.coordinates)
        val shift = round((a.points.first().x - rawB.points.first().x) / TrailMapGeometry.WORLD_METERS) * TrailMapGeometry.WORLD_METERS
        val b = TrailPath(rawB.points.map { TrailPoint(it.x + shift, it.y) })
        require(a.length > 0 && b.length > 0) { "A morph needs nonzero route lengths" }
        val fractions = sortedSetOf(0.0, 1.0)
        // Keep every target corner, plus regular samples to describe the starting arc.
        var distance = 0.0
        for (i in 1 until b.points.size) {
            distance += hypot(b.points[i].x - b.points[i-1].x, b.points[i].y - b.points[i-1].y)
            fractions.add((distance / b.length).coerceIn(0.0, 1.0))
        }
        if (fractions.size + samples <= 100_000) for (i in 1 until samples) fractions.add(i.toDouble() / samples)
        start = fractions.map { a.pointAt(it)!! }; end = fractions.map { b.pointAt(it)!! }
    }
    /** Progress is already eased by the caller. Exact endpoints return the original route objects. */
    fun routeAt(progress: Double): TrailRoute {
        requireFraction(progress, "morph progress")
        if (progress == 0.0) return from
        if (progress == 1.0) return to
        return TrailRoute(to.id, start.indices.map { i ->
            TrailMapGeometry.coordinate(TrailPoint(start[i].x + (end[i].x - start[i].x) * progress,
                start[i].y + (end[i].y - start[i].y) * progress))
        }, to.revision)
    }
    private fun unwrapped(coordinates: List<TrailCoordinate>): TrailPath {
        var previous = coordinates.first().longitude
        var longitude = previous
        return TrailPath(coordinates.mapIndexed { index, c ->
            if (index > 0) { longitude += TrailGeography.delta(previous, c.longitude); previous = c.longitude }
            TrailPoint(longitude / 360 * TrailMapGeometry.WORLD_METERS, TrailMapGeometry.point(c).y)
        })
    }
}

enum class TrailRoutePhase { Loading, Morphing, Ready, Failed, Cancelled }
class TrailRouteRequest internal constructor()

/**
 * Deterministic, main-thread request state. A token belongs to one request on one instance.
 * begin invalidates earlier responses; fail/cancel cannot replace an already resolved route.
 * No networking, retries or background work are hidden in the SDK.
 */
class TrailRouteTransition(from: TrailCoordinate, to: TrailCoordinate, val durationSeconds: Double = .65) {
    var request = TrailRouteRequest(); private set
    var phase = TrailRoutePhase.Loading; private set
    var route = TrailRoute.arc("trail/loading", from, to); private set
    private var origin = from
    private var destination = to
    private var morph: TrailRouteMorph? = null
    private var elapsed = 0.0
    init { require(durationSeconds.isFinite() && durationSeconds > 0 && durationSeconds <= 10) { "Morph duration must be in (0, 10] seconds" } }
    fun begin(from: TrailCoordinate, to: TrailCoordinate): TrailRouteRequest {
        val arc = TrailRoute.arc("trail/loading", from, to)
        request = TrailRouteRequest(); origin = from; destination = to; route = arc
        phase = TrailRoutePhase.Loading; morph = null; elapsed = 0.0
        return request
    }
    fun resolve(request: TrailRouteRequest, route: TrailRoute, reducedMotion: Boolean = false): Boolean {
        if (request !== this.request || phase != TrailRoutePhase.Loading) return false
        require(route.coordinates.size >= 2 && route.distanceMeters > 0) { "Resolved directions need at least two distinct coordinates" }
        require(TrailGeography.distance(origin, route.coordinates.first()) <= 150 &&
            TrailGeography.distance(destination, route.coordinates.last()) <= 150) { "Resolved route endpoints must match the request within 150 meters (including road snapping)" }
        if (reducedMotion || this.route.distanceMeters == 0.0) { this.route = route; phase = TrailRoutePhase.Ready }
        else { morph = TrailRouteMorph(this.route, route); elapsed = 0.0; phase = TrailRoutePhase.Morphing }
        return true
    }
    fun fail(request: TrailRouteRequest): Boolean = finishRequest(request, TrailRoutePhase.Failed)
    fun cancel(request: TrailRouteRequest): Boolean = finishRequest(request, TrailRoutePhase.Cancelled)
    private fun finishRequest(request: TrailRouteRequest, phase: TrailRoutePhase): Boolean {
        if (request !== this.request || this.phase != TrailRoutePhase.Loading) return false
        this.phase = phase; return true
    }
    fun advance(seconds: Double, reducedMotion: Boolean = false) {
        require(seconds.isFinite() && seconds >= 0) { "Elapsed time must be finite and nonnegative" }
        if (phase != TrailRoutePhase.Morphing) return
        elapsed = min(durationSeconds, elapsed + seconds)
        val p = if (reducedMotion) 1.0 else elapsed / durationSeconds
        route = morph!!.routeAt(if (p == 1.0) 1.0 else p*p*p*(10 + p*(-15 + 6*p)))
        if (p == 1.0) { morph = null; phase = TrailRoutePhase.Ready }
    }
}
