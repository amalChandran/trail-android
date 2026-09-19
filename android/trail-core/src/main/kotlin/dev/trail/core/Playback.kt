package dev.trail.core

enum class TrailPlaybackStatus { Playing, Paused, Finished }

/** Host-owned deterministic clock. UI adapters drive it only while visible and active. */
class TrailPlayer(effect: TrailEffect = TrailEffect(), autoPlay: Boolean = true) {
    var effect: TrailEffect = effect; private set
    var elapsedSeconds: Double = 0.0; private set
    private var seekEndpoint = false
    var status: TrailPlaybackStatus = if (autoPlay && effect.durationSeconds > 0) TrailPlaybackStatus.Playing else TrailPlaybackStatus.Paused; private set
    val progress: Double get() = if (effect.durationSeconds == 0.0) 1.0 else {
        val cycles = elapsedSeconds / effect.durationSeconds
        if (effect.repeats && !seekEndpoint) cycles - kotlin.math.floor(cycles) else cycles.coerceIn(0.0, 1.0)
    }
    fun play() { if (effect.durationSeconds == 0.0) return; seekEndpoint = false; if (status == TrailPlaybackStatus.Finished) elapsedSeconds = 0.0; status = TrailPlaybackStatus.Playing }
    fun pause() { if (status == TrailPlaybackStatus.Playing) status = TrailPlaybackStatus.Paused }
    fun replay() { elapsedSeconds = 0.0; seekEndpoint = false; status = if (effect.durationSeconds > 0) TrailPlaybackStatus.Playing else TrailPlaybackStatus.Paused }
    fun seek(fraction: Double) { requireFraction(fraction, "seek"); seekEndpoint = fraction == 1.0; elapsedSeconds = fraction * effect.durationSeconds; status = TrailPlaybackStatus.Paused }
    fun advance(seconds: Double) {
        require(seconds.isFinite() && seconds >= 0) { "Clock delta must be finite and nonnegative" }
        if (status != TrailPlaybackStatus.Playing) return
        val next = elapsedSeconds + seconds
        require(next.isFinite()) { "Clock overflow" }
        elapsedSeconds = if (effect.repeats) next else next.coerceAtMost(effect.durationSeconds)
        if (!effect.repeats && elapsedSeconds >= effect.durationSeconds) status = TrailPlaybackStatus.Finished
    }
    fun configure(effect: TrailEffect, reset: Boolean = false) {
        val oldDuration = this.effect.durationSeconds
        val normalized = if (oldDuration == 0.0) 0.0 else elapsedSeconds / oldDuration
        this.effect = effect
        if (reset) seekEndpoint = false
        elapsedSeconds = if (reset) 0.0 else normalized * effect.durationSeconds
        if (effect.durationSeconds == 0.0) status = TrailPlaybackStatus.Paused
        else if (reset) status = TrailPlaybackStatus.Playing
    }
    fun frame(layer: Int, reducedMotion: Boolean = false): TrailVisualState {
        // Paused seek-to-end displays the final frame, even for a looping clip.
        val spec = effect.layers[layer].animation
        if (!reducedMotion && seekEndpoint && spec != null && elapsedSeconds == spec.durationSeconds) {
            return spec.sampler.sample(TrailTime(1.0, 0, spec.durationSeconds))
        }
        return effect.sample(layer, elapsedSeconds, reducedMotion)
    }
}
