package dev.trail.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.trail.core.*

/**
 * Provider-neutral map binding. Projection returns local dp, with exactly the map's bounds.
 * Change projectionRevision after camera, inset or projection changes; layout is observed here.
 * A missing projection stops the clock without losing playback state. Never consumes gestures.
 * Change route.revision when replacing geometry under the same ID.
 */
@Composable fun TrailProjectedOverlay(
    route: TrailRoute,
    projection: TrailProjection?,
    projectionRevision: Any?,
    modifier: Modifier = Modifier,
    effect: TrailEffect? = null,
    playback: TrailPlayback = rememberTrailPlayback(effect ?: remember { TrailEffect() }, route.key),
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    onProjected: (TrailPath?) -> Unit = {},
) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val path = remember(route.key, projection, projectionRevision, viewport) {
        if (projection == null || viewport.width == 0 || viewport.height == 0) null
        else route.projectIfReady(projection)
    }
    SideEffect { onProjected(path) }
    val empty = remember { TrailPath(emptyList()) }
    TrailCanvas(path ?: empty, modifier.clipToBounds().onSizeChanged { viewport = it }, effect, playback,
        fit = false, reducedMotion = reducedMotion, active = path != null)
}
