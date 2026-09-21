package dev.trail.effects

import dev.trail.core.*
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Ready-to-render route animations. Each pairs its motion with a compatible line style. */
enum class TrailRoutePreset(val label: String, val description: String, val duration: Duration) {
    MovingDots("Moving dots", "Round dots flowing toward the destination", .75.seconds),
    MovingDashes("Moving dashes", "A steady stream of short route segments", 1.seconds),
    Loading("Loading", "A travelling segment over the complete route", 2.seconds),
    Comet("Comet", "A bright head with a fading tail", 3.seconds),
    RouteSweep("Route sweep", "A two-color draw and fade, inspired by the Java overlay", 3.seconds),
    DrawAndErase("Draw + erase", "Draw to the destination, then clear from the origin", 3.seconds);

    /**
     * Width and spacing are logical display units. Duration is one pattern step for dots/dashes,
     * or one route traversal for the other presets. The base remains visible throughout.
     * Reduced motion shows a static, complete route; playback, seeking and lifecycle are host-owned.
     */
    fun effect(
        color: TrailColor = TrailColor.Mint,
        width: Double = 6.0,
        baseColor: TrailColor = TrailColor(0xFF344D5B.toInt()),
        duration: Duration = this.duration,
        repeat: Boolean = true,
    ): TrailEffect {
        val line = when (this) {
            MovingDots -> TrailStyles.dotted(color, width, gap = width * 3)
            MovingDashes -> TrailStyles.dashed(color, width, dash = width * 2.5, gap = width * 2)
            // Translucent tail steps need flat boundaries; round caps brighten the
            // neighboring pixels into beads instead of a fading line.
            Comet -> TrailLineStyle { it.stroke(color, width, roundCap = false) }
            else -> TrailStyles.solid(color, width)
        }
        val sampler = when (this) {
            MovingDots, MovingDashes -> TrailAnimation { TrailVisualState(dashPhase = it.progress) }
            Loading -> TrailAnimations.loading(duration).sampler
            Comet -> TrailAnimation { routeComet(it.progress, (color.argb ushr 24) / 255.0) }
            RouteSweep -> TrailAnimation { time ->
                // The old Java overlay draws a top color over a persistent bottom color.
                // Let that foreground settle and fade before restarting, avoiding a hard reset.
                val p = time.progress
                when {
                    p < .55 -> TrailVisualState.reveal(1 - (1 - p / .55).pow(3))
                    p < .72 -> TrailVisualState.reveal(1.0)
                    else -> TrailVisualState(opacity = 1 - smooth((p - .72) / .28), head = 1.0)
                }
            }
            DrawAndErase -> TrailAnimation { time ->
                if (time.progress < .5) TrailVisualState.reveal(smooth(time.progress * 2))
                else TrailVisualState(listOf(TrailWindow(smooth((time.progress - .5) * 2), 1.0)), head = 1.0)
            }
        }
        return trailEffect {
            layer { stroke(baseColor, width) }
            layer { style(line); animation(TrailAnimations.custom(sampler, duration, repeat)) }
        }
    }
}

private fun smooth(value: Double): Double = value.coerceIn(0.0, 1.0).let { it * it * (3 - 2 * it) }

private fun routeComet(head: Double, colorOpacity: Double): TrailVisualState {
    // Nested windows avoid antialiased cracks between translucent adjoining segments.
    // Account for the supplied color's alpha so overlapping steps never make a
    // translucent route more opaque than requested. Opaque colors use 1/(12-i).
    val windows = ArrayList<TrailWindow>(24)
    for (i in 0 until 12) {
        val start = head - .25 + .25 * i / 12
        val opacity = 1.0 / (12 - colorOpacity * i)
        if (start < 0.0) {
            windows.add(TrailWindow(1 + start, 1.0, opacity))
            if (head > 0.0) windows.add(TrailWindow(0.0, head, opacity))
        } else windows.add(TrailWindow(start, head, opacity))
    }
    return TrailVisualState(windows, head = head)
}
