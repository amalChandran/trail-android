package dev.trail.effects

import dev.trail.core.*
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class TrailStylePreset(val label: String) {
    Solid("Solid"), Cased("Cased"), Dashed("Dashed"), Dotted("Dotted"), Gradient("Gradient"), Glow("Glow"), Chevrons("Chevrons"), Tapered("Tapered");
    fun style(color: TrailColor = TrailColor.Mint, width: Double = 6.0): TrailLineStyle = when (this) {
        Solid -> TrailStyles.solid(color, width)
        Cased -> TrailStyles.cased(color, width, TrailColor(0xFF132B36.toInt()), 3.0)
        Dashed -> TrailStyles.dashed(color, width)
        Dotted -> TrailStyles.dotted(color, width)
        Gradient -> TrailLineStyle { context ->
            for (i in 0 until 64) context.stroke(blend(color, TrailColor.Coral, i / 63.0), width,
                i / 64.0, (i + 1) / 64.0, roundCap = false)
        }
        Glow -> TrailLineStyle { context ->
            for (i in 4 downTo 1) context.stroke(color.withOpacity(0.035 * (5 - i)), width + i * 5)
            context.stroke(color, width)
        }
        Chevrons -> TrailLineStyle { it.stroke(color.withOpacity(0.3), width * 0.5); it.chevrons(color, width * 1.5, 28.0) }
        Tapered -> TrailLineStyle { context ->
            for (i in 0 until 64) context.stroke(color, width * (0.2 + 0.8 * i / 63.0), i / 64.0, (i + 1) / 64.0)
        }
    }
}

enum class TrailMotionPreset(val label: String) {
    Reveal("Reveal"), Erase("Erase"), PingPong("Ping pong"), Comet("Comet"), MultiComet("Multi comet"),
    DashFlow("Dash flow"), Pulse("Pulse"), Breathe("Breathe"), Spotlight("Spotlight"),
    SegmentedChase("Segment chase"), RevealThenFlow("Reveal + flow"), DrawAndErase("Draw + erase");

    /** Sampler for use inside animation(...). Spotlight's static base belongs in a separate layer. */
    fun animation(duration: Duration = 3.seconds, repeat: Boolean = true): TrailAnimationSpec {
        val sampler = TrailAnimation { time ->
            val p = time.progress
            when (this) {
                Reveal -> TrailVisualState.reveal(p)
                Erase -> TrailVisualState(listOf(TrailWindow(p, 1.0)))
                PingPong -> TrailVisualState.reveal(1 - abs(2 * p - 1))
                Comet, Spotlight -> comet(p, 0.25)
                MultiComet -> TrailVisualState((0 until 3).flatMap { comet((p + it / 3.0) % 1, 0.15).windows })
                DashFlow -> TrailVisualState(dashPhase = p)
                Pulse -> TrailVisualState(opacity = 0.55 + 0.45 * (1 - cos(2 * PI * p)) / 2)
                Breathe -> TrailVisualState(widthScale = 1 + 0.18 * sin(2 * PI * p))
                SegmentedChase -> {
                    val index = floor(p * 8).toInt().coerceAtMost(7)
                    TrailVisualState(listOf(TrailWindow(index / 8.0, (index + 0.8) / 8.0)))
                }
                RevealThenFlow -> if (p < 0.4) TrailVisualState.reveal(p / 0.4) else TrailVisualState(dashPhase = (p - 0.4) / 0.6)
                DrawAndErase -> if (p < 0.5) TrailVisualState.reveal(p * 2) else TrailVisualState(listOf(TrailWindow((p - 0.5) * 2, 1.0)))
            }
        }
        return TrailAnimations.custom(sampler, duration, repeat, if (this == Erase || this == DrawAndErase) TrailVisualState.Hidden else TrailVisualState.Full)
    }
    fun effect(style: TrailLineStyle = TrailStyles.solid(), duration: Duration = 3.seconds, repeat: Boolean = true): TrailEffect {
        val spec = animation(duration, repeat)
        return trailEffect {
            if (this@TrailMotionPreset == Spotlight) {
                layer { stroke(TrailColor(0xFF344D5B.toInt()), 6.0) }
                layer { style(style); animation(spec) }
            } else { style(style); animation(spec) }
        }
    }
}

private fun comet(head: Double, length: Double): TrailVisualState {
    // Bounded layered tail, deterministic in arbitrary sampling order; wraps across the route start.
    val windows = ArrayList<TrailWindow>()
    for (i in 0 until 12) {
        val from = head - length + length * i / 12
        val to = head - length + length * (i + 1) / 12
        val alpha = (i + 1) / 12.0
        if (from < 0 && to > 0) {
            windows.add(TrailWindow(1 + from, 1.0, alpha)); windows.add(TrailWindow(0.0, to, alpha))
        } else if (to <= 0) windows.add(TrailWindow((1 + from).coerceIn(0.0, 1.0), (1 + to).coerceIn(0.0, 1.0), alpha))
        else windows.add(TrailWindow(from.coerceIn(0.0, 1.0), to.coerceIn(0.0, 1.0), alpha))
    }
    return TrailVisualState(windows, head = head)
}

private fun blend(a: TrailColor, b: TrailColor, t: Double): TrailColor {
    fun channel(shift: Int): Int { val start = (a.argb ushr shift) and 255; return (start + (((b.argb ushr shift) and 255) - start) * t).toInt() shl shift }
    return TrailColor(channel(24) or channel(16) or channel(8) or channel(0))
}
