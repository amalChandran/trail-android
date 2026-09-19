package dev.trail.core

import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TrailWindow(val start: Double, val end: Double, val opacity: Double = 1.0) {
    init { requireFraction(start, "window.start"); requireFraction(end, "window.end"); require(start <= end); requireFraction(opacity, "window.opacity") }
}

class TrailVisualState(
    windows: List<TrailWindow> = listOf(TrailWindow(0.0, 1.0)),
    val opacity: Double = 1.0,
    val widthScale: Double = 1.0,
    val dashPhase: Double = 0.0,
    val head: Double? = null,
) {
    val windows: List<TrailWindow> = java.util.Collections.unmodifiableList(windows.toList())
    init {
        require(windows.size <= 256) { "At most 256 windows per frame" }
        requireFraction(opacity, "opacity"); requireFraction(dashPhase, "dashPhase")
        require(widthScale.isFinite() && widthScale in 0.0..16.0) { "widthScale must be in [0, 16]" }
        head?.let { requireFraction(it, "head") }
    }
    companion object {
        val Full = TrailVisualState()
        val Hidden = TrailVisualState(emptyList())
        fun reveal(to: Double) = TrailVisualState(listOf(TrailWindow(0.0, to)), head = to)
    }
}

data class TrailTime(val progress: Double, val cycle: Long = 0, val elapsedSeconds: Double = 0.0) {
    init { requireFraction(progress, "progress"); require(cycle >= 0); require(elapsedSeconds.isFinite() && elapsedSeconds >= 0) }
}

/** Pure sampler: seeking and playback must produce the same output for the same time. */
fun interface TrailAnimation { fun sample(time: TrailTime): TrailVisualState }

class TrailAnimationSpec(
    val sampler: TrailAnimation,
    val duration: Duration = 2.seconds,
    val repeat: Boolean = false,
    val reducedMotion: TrailVisualState = TrailVisualState.Full,
) {
    val durationSeconds = duration.toDouble(DurationUnitSeconds)
    init { require(duration.isFinite() && duration > Duration.ZERO) { "Duration must be positive and finite" } }
}
private val DurationUnitSeconds = kotlin.time.DurationUnit.SECONDS

object TrailAnimations {
    fun custom(sampler: TrailAnimation, duration: Duration = 2.seconds, repeat: Boolean = false,
               reducedMotion: TrailVisualState = TrailVisualState.Full) = TrailAnimationSpec(sampler, duration, repeat, reducedMotion)
    fun reveal(duration: Duration = 2.seconds, repeat: Boolean = false) = custom(TrailAnimation { TrailVisualState.reveal(it.progress) }, duration, repeat)
    fun erase(duration: Duration = 2.seconds, repeat: Boolean = false) = custom(TrailAnimation {
        TrailVisualState(listOf(TrailWindow(it.progress, 1.0)))
    }, duration, repeat, TrailVisualState.Hidden)
}
