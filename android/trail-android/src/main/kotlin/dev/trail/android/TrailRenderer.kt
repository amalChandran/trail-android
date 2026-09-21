package dev.trail.android

import android.graphics.*
import dev.trail.core.*
import kotlin.math.*

/** UI-thread renderer. Geometry and Paint/Path buffers are reused between frames. */
class TrailRenderer(val path: TrailPath) {
    private data class Contour(val offset: Double, val length: Double, val measure: PathMeasure)
    private val contours = path.slices(0.0, 1.0).map { section ->
        val fullPath = Path().apply {
            moveTo(section.points.first().x.toFloat(), section.points.first().y.toFloat())
            section.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        }
        Contour(section.distanceFromStart, TrailPath(section.points).length, PathMeasure(fullPath, false))
    }
    private val segment = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val dashCache = HashMap<TrailStroke, Pair<Double, DashPathEffect>>()
    private val dashIntervals = HashMap<TrailStroke, FloatArray>()

    /** Canvas coordinates and all dimensions are logical units; density is applied by the host. */
    fun draw(canvas: Canvas, effect: TrailEffect, frame: (Int) -> TrailVisualState) {
        if (path.length == 0.0) return
        effect.layers.forEachIndexed { index, layer ->
            val state = frame(index)
            if (state.opacity <= 0 || state.widthScale <= 0) return@forEachIndexed
            for (command in layer.commands) when (command) {
                is TrailStroke -> drawStroke(canvas, command, state)
                is TrailChevrons -> drawChevrons(canvas, command, state)
            }
        }
    }
    private fun drawStroke(canvas: Canvas, stroke: TrailStroke, state: TrailVisualState) {
        paint.strokeWidth = (stroke.width * state.widthScale).toFloat()
        paint.strokeCap = if (stroke.roundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
        val cycle = stroke.dash.sum()
        for (window in state.windows) {
            val start = max(stroke.start, window.start); val end = min(stroke.end, window.end)
            if (start >= end) continue
            paint.color = stroke.color.withOpacity(state.opacity * window.opacity).argb
            for (contour in contours) {
                val a = max(start * path.length, contour.offset)
                val b = min(end * path.length, contour.offset + contour.length)
                if (a >= b) continue
                // Clip each continuous contour separately; never bridge a world-wrap seam.
                paint.pathEffect = if (cycle == 0.0) null else {
                    val phase = (a - state.dashPhase * cycle) % cycle
                    val cached = dashCache[stroke]
                    if (cached?.first == phase) cached.second else DashPathEffect(dashIntervals.getOrPut(stroke) { stroke.dash.map { it.toFloat() }.toFloatArray() }, phase.toFloat()).also {
                        if (dashCache.size > 512) { dashCache.clear(); dashIntervals.clear() }
                        dashCache[stroke] = phase to it
                    }
                }
                segment.rewind()
                contour.measure.getSegment(((a-contour.offset)/contour.length*contour.measure.length).toFloat(),
                    ((b-contour.offset)/contour.length*contour.measure.length).toFloat(), segment, true)
                canvas.drawPath(segment, paint)
            }
        }
    }
    private fun drawChevrons(canvas: Canvas, command: TrailChevrons, state: TrailVisualState) {
        val count = min(2048, floor(path.length / command.spacing).toInt())
        paint.pathEffect = null; paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = (command.size * 0.25 * state.widthScale).toFloat()
        for (i in 0 until count) {
            val fraction = ((i + 0.5) * command.spacing / path.length + state.dashPhase * command.spacing / path.length) % 1.0
            val window = state.windows.firstOrNull { fraction >= it.start && fraction <= it.end } ?: continue
            val point = path.pointAt(fraction) ?: continue
            paint.color = command.color.withOpacity(state.opacity * window.opacity).argb
            val save = canvas.save()
            canvas.translate(point.x.toFloat(), point.y.toFloat())
            canvas.rotate(Math.toDegrees(path.tangentAt(fraction)).toFloat())
            val size = (command.size * state.widthScale).toFloat()
            segment.rewind(); segment.moveTo(-size / 2, -size / 2); segment.lineTo(size / 2, 0f); segment.lineTo(-size / 2, size / 2)
            canvas.drawPath(segment, paint); canvas.restoreToCount(save)
        }
    }
}
