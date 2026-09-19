package dev.trail.compose

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.trail.core.*
import kotlinx.coroutines.flow.collectLatest

/** UI state for loading arc → resolved route. Capture request before starting asynchronous work. */
@Stable class TrailRouteTransitionState internal constructor(from: TrailCoordinate, to: TrailCoordinate, durationSeconds: Double) {
    private val model = TrailRouteTransition(from, to, durationSeconds)
    private var revision by mutableLongStateOf(0)
    val request: TrailRouteRequest get() { revision; return model.request }
    val phase: TrailRoutePhase get() { revision; return model.phase }
    val route: TrailRoute get() { revision; return model.route }
    fun begin(from: TrailCoordinate, to: TrailCoordinate): TrailRouteRequest = model.begin(from, to).also { revision++ }
    fun resolve(request: TrailRouteRequest, route: TrailRoute): Boolean = model.resolve(request, route).also { if (it) revision++ }
    fun fail(request: TrailRouteRequest): Boolean = model.fail(request).also { if (it) revision++ }
    fun cancel(request: TrailRouteRequest): Boolean = model.cancel(request).also { if (it) revision++ }
    internal fun advance(seconds: Double, reducedMotion: Boolean) { model.advance(seconds, reducedMotion); revision++ }
}

/** Owns only the short morph clock. The map binding owns ordinary effect playback. */
@Composable fun rememberTrailRouteTransition(from: TrailCoordinate, to: TrailCoordinate,
    durationSeconds: Double = .65, reducedMotion: Boolean = rememberSystemReducedMotion(), active: Boolean = true): TrailRouteTransitionState {
    val state = remember(from, to, durationSeconds) { TrailRouteTransitionState(from, to, durationSeconds) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(state, owner, reducedMotion, active) {
        if (!active) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { state.phase }.collectLatest { phase ->
                if (phase == TrailRoutePhase.Morphing) {
                    if (reducedMotion) state.advance(0.0, true)
                    else {
                        var previous: Long? = null
                        while (state.phase == TrailRoutePhase.Morphing) withFrameNanos { now ->
                            previous?.let { state.advance((now-it)/1e9, false) }; previous = now
                        }
                    }
                }
            }
        }
    }
    return state
}
