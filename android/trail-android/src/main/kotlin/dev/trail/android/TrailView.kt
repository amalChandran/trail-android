package dev.trail.android

import android.content.Context
import android.graphics.Canvas
import android.provider.Settings
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import dev.trail.core.*

/** Small View adapter for applications that do not use Compose. All calls on the UI thread. */
class TrailView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs), Choreographer.FrameCallback {
    var path: TrailPath = TrailPath(emptyList()); set(value) { field = value; updateRenderer(); invalidate() }
    var effect: TrailEffect = TrailEffect(); set(value) { field = value; player.configure(value, reset = true); schedule(); invalidate() }
    var reducedMotion: Boolean = false; set(value) { field = value; stopClock(); schedule(); invalidate() }
    private val player = TrailPlayer(effect)
    private var renderer = TrailRenderer(path)
    private var lastFrame: Long? = null
    private var scheduled = false
    fun play() { player.play(); schedule(); invalidate() }
    fun pause() { player.pause(); stopClock(); invalidate() }
    fun seek(progress: Double) { player.seek(progress); stopClock(); invalidate() }
    fun replay() { player.replay(); stopClock(); schedule(); invalidate() }
    private fun motionReduced() = reducedMotion || Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    private fun updateRenderer() {
        val density = resources.displayMetrics.density.toDouble()
        renderer = TrailRenderer(path.fitted(width / density, height / density))
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateRenderer()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val saved = canvas.save(); val density = resources.displayMetrics.density
        canvas.scale(density, density)
        renderer.draw(canvas, effect) { player.frame(it, motionReduced()) }
        canvas.restoreToCount(saved)
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); schedule() }
    override fun onDetachedFromWindow() { stopClock(); super.onDetachedFromWindow() }
    override fun onVisibilityAggregated(isVisible: Boolean) { super.onVisibilityAggregated(isVisible); if (isVisible) schedule() else stopClock() }
    private fun schedule() {
        if (!scheduled && isAttachedToWindow && isShown && windowVisibility == VISIBLE && !motionReduced() && player.status == TrailPlaybackStatus.Playing) {
            scheduled = true; Choreographer.getInstance().postFrameCallback(this)
        }
    }
    private fun stopClock() { Choreographer.getInstance().removeFrameCallback(this); scheduled = false; lastFrame = null }
    override fun doFrame(frameTimeNanos: Long) {
        scheduled = false
        lastFrame?.let { player.advance((frameTimeNanos - it) / 1_000_000_000.0) }
        lastFrame = frameTimeNanos; invalidate(); schedule()
        if (!scheduled) lastFrame = null
    }
}
