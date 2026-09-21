package dev.trail.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** sRGB packed ARGB. No UI framework dependency. */
@JvmInline value class TrailColor(val argb: Int) {
    fun withOpacity(opacity: Double): TrailColor {
        requireFraction(opacity, "opacity")
        val alpha = (((argb ushr 24) and 255) * opacity).toInt()
        return TrailColor((argb and 0x00ffffff) or (alpha shl 24))
    }
    companion object {
        val Blue = TrailColor(0xFF2563EB.toInt()); val White = TrailColor(0xFFFFFFFF.toInt())
        val Mint = TrailColor(0xFF32D6AD.toInt()); val Coral = TrailColor(0xFFFF785A.toInt())
    }
}

/** Immutable command recorded once per effect, executed with each frame's visual state. */
sealed interface TrailCommand
data class TrailStroke internal constructor(
    val color: TrailColor, val width: Double, val start: Double, val end: Double,
    val dash: List<Double>, val roundCap: Boolean,
) : TrailCommand
data class TrailChevrons internal constructor(val color: TrailColor, val size: Double, val spacing: Double) : TrailCommand

fun interface TrailLineStyle { fun draw(context: TrailDrawContext) }

/** Public plugin surface. Width and dash lengths are logical display units. */
class TrailDrawContext internal constructor() {
    private var open = true
    private val commands = ArrayList<TrailCommand>()
    fun stroke(color: TrailColor = TrailColor.Blue, width: Double = 6.0,
               start: Double = 0.0, end: Double = 1.0, dash: List<Double> = emptyList(), roundCap: Boolean = true) {
        check(open) { "A drawing context cannot be retained after draw returns" }
        require(width.isFinite() && width > 0 && width <= 1024) { "Width must be in (0, 1024]" }
        requireFraction(start, "start"); requireFraction(end, "end"); require(start <= end)
        require(dash.isEmpty() || (dash.size % 2 == 0 && dash.size <= 32 && dash.all { it.isFinite() && it > 0 })) { "Dash must have even, positive finite entries (max 32)" }
        require(commands.size < 512) { "At most 512 drawing commands per style" }
        commands.add(TrailStroke(color, width, start, end, java.util.Collections.unmodifiableList(dash.toList()), roundCap))
    }
    fun chevrons(color: TrailColor = TrailColor.Blue, size: Double = 8.0, spacing: Double = 28.0) {
        check(open) { "Drawing context is closed" }
        require(size.isFinite() && size > 0 && size <= 1024)
        require(spacing.isFinite() && spacing >= 2.0)
        require(commands.size < 512)
        commands.add(TrailChevrons(color, size, spacing))
    }
    internal fun finish(): List<TrailCommand> { open = false; return java.util.Collections.unmodifiableList(commands.toList()) }
}

object TrailStyles {
    fun solid(color: TrailColor = TrailColor.Blue, width: Double = 6.0): TrailLineStyle = TrailLineStyle { it.stroke(color, width) }
    fun cased(color: TrailColor = TrailColor.Blue, width: Double = 6.0, casing: TrailColor = TrailColor.White,
              casingWidth: Double = 3.0): TrailLineStyle {
        require(casingWidth.isFinite() && casingWidth >= 0)
        return TrailLineStyle { it.stroke(casing, width + 2 * casingWidth); it.stroke(color, width) }
    }
    fun dashed(color: TrailColor = TrailColor.Blue, width: Double = 6.0, dash: Double = 12.0, gap: Double = 8.0): TrailLineStyle =
        TrailLineStyle { it.stroke(color, width, dash = listOf(dash, gap)) }
    fun dotted(color: TrailColor = TrailColor.Blue, width: Double = 6.0, gap: Double = 12.0): TrailLineStyle =
        TrailLineStyle { it.stroke(color, width, dash = listOf(0.01, gap)) }
}

class TrailLayer(val style: TrailLineStyle, val animation: TrailAnimationSpec? = null) {
    /** Preparation invokes public plugins once; errors surface at configuration, not mid-frame. */
    val commands: List<TrailCommand> = TrailDrawContext().let { style.draw(it); it.finish() }
}

class TrailEffect(layers: List<TrailLayer>) {
    val layers: List<TrailLayer> = java.util.Collections.unmodifiableList(layers.toList())
    init { require(layers.isNotEmpty() && layers.size <= 16) { "An effect needs 1–16 layers" } }
    constructor(style: TrailLineStyle = TrailStyles.solid(), animation: TrailAnimationSpec? = null) : this(listOf(TrailLayer(style, animation)))
    val durationSeconds: Double = this.layers.maxOf { it.animation?.durationSeconds ?: 0.0 }
    val repeats: Boolean = this.layers.any { it.animation?.repeat == true }
    fun sample(layer: Int, elapsed: Double, reducedMotion: Boolean = false): TrailVisualState {
        require(elapsed.isFinite() && elapsed >= 0)
        val spec = layers[layer].animation ?: return TrailVisualState.Full
        if (reducedMotion) return spec.reducedMotion
        val cycles = elapsed / spec.durationSeconds
        val progress = if (spec.repeat) cycles - kotlin.math.floor(cycles) else cycles.coerceAtMost(1.0)
        return spec.sampler.sample(TrailTime(progress, if (spec.repeat) kotlin.math.floor(cycles).toLong() else 0,
            if (spec.repeat) progress * spec.durationSeconds else elapsed.coerceAtMost(spec.durationSeconds)))
    }
}

@DslMarker annotation class TrailDsl

@TrailDsl class TrailEffectScope internal constructor() {
    private var currentStyle: TrailLineStyle = TrailStyles.solid()
    private var currentAnimation: TrailAnimationSpec? = null
    private var hasStyle = false; private var hasAnimation = false
    private val layers = ArrayList<TrailLayer>()
    fun style(style: TrailLineStyle) {
        require(layers.isEmpty()) { MIXED_LAYERS }
        require(!hasStyle) { "One style is allowed per layer. Put each style in its own layer { style(...) } block." }
        hasStyle = true; currentStyle = style
    }
    fun stroke(color: TrailColor = TrailColor.Blue, width: Double = 6.0) = style(TrailStyles.solid(color, width))
    fun animation(spec: TrailAnimationSpec) {
        require(layers.isEmpty()) { MIXED_LAYERS }
        require(!hasAnimation) {
            "One animation is allowed per layer. Use sequence { reveal(...); erase(...) } for ordered steps, or separate layer { } blocks for simultaneous animations."
        }
        hasAnimation = true; currentAnimation = spec
    }
    fun reveal(duration: Duration = 2.seconds, repeat: Boolean = false) = animation(TrailAnimations.reveal(duration, repeat))
    fun erase(duration: Duration = 2.seconds, repeat: Boolean = false) = animation(TrailAnimations.erase(duration, repeat))
    fun sequence(repeat: Boolean = false, block: TrailSequenceScope.() -> Unit) = animation(TrailSequenceScope().apply(block).build(repeat))
    fun layer(block: TrailEffectScope.() -> Unit) {
        require(!hasStyle && !hasAnimation) { MIXED_LAYERS }
        layers.addAll(TrailEffectScope().apply(block).build().layers)
    }
    internal fun build(): TrailEffect {
        if (layers.isEmpty() || hasStyle || hasAnimation) layers.add(0, TrailLayer(currentStyle, currentAnimation))
        return TrailEffect(layers)
    }
    private companion object {
        const val MIXED_LAYERS = "Do not mix top-level style/animation declarations with layer { }. Put every style and animation inside an explicit layer { } block."
    }
}
fun trailEffect(block: TrailEffectScope.() -> Unit): TrailEffect = TrailEffectScope().apply(block).build()
