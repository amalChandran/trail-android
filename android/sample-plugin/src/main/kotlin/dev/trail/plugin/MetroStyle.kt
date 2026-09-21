package dev.trail.plugin

import dev.trail.core.*

/** Compiled as a separate consumer module: no internal Trail APIs or registration. */
class MetroStyle(private val color: TrailColor = TrailColor.Coral) : TrailLineStyle {
    override fun draw(context: TrailDrawContext) {
        context.stroke(TrailColor.White, width = 12.0)
        context.stroke(color, width = 7.0)
        context.chevrons(TrailColor.White, size = 4.0, spacing = 32.0)
    }
}

class QuadraticReveal : TrailAnimation {
    override fun sample(time: TrailTime) = TrailVisualState.reveal(time.progress * time.progress)
}

fun TrailEffectScope.metro(color: TrailColor = TrailColor.Coral) {
    style(MetroStyle(color))
    animation(TrailAnimations.custom(QuadraticReveal()))
}
