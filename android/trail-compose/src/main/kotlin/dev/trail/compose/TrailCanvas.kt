package dev.trail.compose

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.trail.android.TrailRenderer
import dev.trail.core.*
import kotlinx.coroutines.flow.collectLatest

@Stable class TrailPlayback internal constructor(effect: TrailEffect, autoPlay: Boolean) {
    internal val player = TrailPlayer(effect, autoPlay)
    internal var tick by mutableLongStateOf(0); private set
    val progress: Double get() { tick; return player.progress }
    val isPlaying: Boolean get() { tick; return player.status == TrailPlaybackStatus.Playing }
    val status: TrailPlaybackStatus get() { tick; return player.status }
    fun play() { player.play(); changed() }
    fun pause() { player.pause(); changed() }
    fun replay() { player.replay(); changed() }
    fun seek(progress: Double) { player.seek(progress); changed() }
    internal fun changed() { tick++ }
    internal fun configure(effect: TrailEffect) {
        if (player.effect === effect) return
        val previousStatus = player.status
        player.configure(effect)
        // Inline DSL construction may produce a new value on recomposition. Only notify a
        // changed playback state, avoiding an effect-construction/recomposition feedback loop.
        if (player.status != previousStatus) changed()
    }
}

/** Reuse one controller for one visible binding. Configuration updates preserve normalized time. */
@Composable fun rememberTrailPlayback(effect: TrailEffect, routeKey: Any? = Unit, autoPlay: Boolean = true): TrailPlayback {
    val playback = remember(routeKey) { TrailPlayback(effect, autoPlay) }
    SideEffect { playback.configure(effect) }
    return playback
}

@Composable fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) { reduced = read() } }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

/** Draws a measured local path. No map or Material dependency. Offscreen/background clocks stop. */
@Composable fun TrailCanvas(
    path: TrailPath,
    modifier: Modifier = Modifier,
    effect: TrailEffect = remember { TrailEffect() },
    playback: TrailPlayback = rememberTrailPlayback(effect, path),
    fit: Boolean = true,
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    active: Boolean = true,
) {
    SideEffect { playback.configure(effect) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var visible by remember { mutableStateOf(false) }
    val hostView = LocalView.current
    val density = LocalDensity.current.density
    val renderer = remember(path, size, fit, density) {
        TrailRenderer(if (fit) path.fitted(size.width / density.toDouble(), size.height / density.toDouble(), 24.0) else path)
    }
    val owner = LocalLifecycleOwner.current
    val drawable = renderer.path.length > 0
    LaunchedEffect(playback, owner, reducedMotion, active, size, visible, drawable) {
        if (!drawable || !active || !visible || reducedMotion || size.width == 0 || size.height == 0) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snapshotFlow { playback.isPlaying }.collectLatest { playing ->
                if (playing) {
                    var previous: Long? = null
                    while (playback.isPlaying) withFrameNanos { frame ->
                        previous?.let { playback.player.advance((frame - it) / 1_000_000_000.0); playback.changed() }
                        previous = frame
                    }
                }
            }
        }
    }
    Canvas(modifier.onSizeChanged { size = it }.onGloballyPositioned {
        val bounds = it.boundsInWindow()
        visible = bounds.width > 0 && bounds.height > 0 && bounds.bottom > 0 && bounds.right > 0 &&
            bounds.top < hostView.height && bounds.left < hostView.width
    }) {
        playback.tick
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas; val saved = native.save()
            native.scale(density, density)
            renderer.draw(native, effect) { playback.player.frame(it, reducedMotion) }
            native.restoreToCount(saved)
        }
    }
}

@Composable fun TrailCanvas(path: TrailPath, modifier: Modifier = Modifier, content: TrailEffectScope.() -> Unit) {
    val effect = trailEffect(content)
    TrailCanvas(path, modifier, effect)
}
