package dev.trail.playground

import android.graphics.*
import androidx.test.platform.app.InstrumentationRegistry
import dev.trail.android.TrailRenderer
import dev.trail.core.*
import dev.trail.effects.TrailRoutePreset
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Pixel checks and reproducible README frames produced by the actual Android renderer. */
class RouteAnimationRenderingTest {
    private val path = TrailPath(listOf(TrailPoint(20.0, 80.0), TrailPoint(380.0, 80.0)))

    private fun render(effect: TrailEffect, seconds: Double, reduced: Boolean = false): Bitmap =
        Bitmap.createBitmap(400, 160, Bitmap.Config.ARGB_8888).also { bitmap ->
            TrailRenderer(path).draw(Canvas(bitmap), effect) { effect.sample(it, seconds, reduced) }
        }

    @Test fun everyRouteOptionMovesAndReducedMotionStopsItsPixels() {
        for (preset in TrailRoutePreset.entries) {
            val effect = preset.effect()
            val a = render(effect, effect.durationSeconds * .15)
            val b = render(effect, effect.durationSeconds * .65)
            val reducedA = render(effect, .1, true)
            val reducedB = render(effect, 20.0, true)
            val start = render(effect, 0.0)
            val end = render(effect, effect.durationSeconds)
            try {
                assertFalse("${preset.name} must visibly animate", a.sameAs(b))
                assertTrue("${preset.name} reduced motion", reducedA.sameAs(reducedB))
                assertTrue("${preset.name} repeat boundary", start.sameAs(end))
                // A persistent base means the route remains legible even between moving segments.
                for (x in 24..376) assertTrue("${preset.name} base at $x", Color.alpha(a.getPixel(x, 80)) > 0)
            } finally { listOf(a, b, reducedA, reducedB, start, end).forEach { it.recycle() } }
        }
    }

    @Test fun dotsTravelForwardWithoutChangingTheirDiameterAndClippingKeepsPatternPhase() {
        val style = TrailStyles.dotted(TrailColor.Mint, width = 8.0, gap = 32.0)
        val effect = TrailEffect(style)
        fun draw(phase: Double, start: Double = 0.0): Bitmap = Bitmap.createBitmap(400, 160, Bitmap.Config.ARGB_8888).also {
            TrailRenderer(path).draw(Canvas(it), effect) { TrailVisualState(listOf(TrailWindow(start, 1.0)), dashPhase = phase) }
        }
        val a = draw(.25); val b = draw(.75); val clipped = draw(.25, .3)
        try {
            assertTrue(Color.alpha(a.getPixel(60, 80)) > 200)
            assertEquals(0, Color.alpha(b.getPixel(60, 80)))
            assertTrue(Color.alpha(b.getPixel(76, 80)) > 200)
            assertEquals(0, Color.alpha(a.getPixel(60, 86)))
            for (x in 140..376) for (y in 72..88) assertEquals("phase retained at $x,$y", a.getPixel(x, y), clipped.getPixel(x, y))
        } finally { listOf(a, b, clipped).forEach { it.recycle() } }
    }

    @Test fun chevronLoopHasTheSamePixelsAtBothEndsOfItsCycle() {
        val effect = TrailEffect(TrailLineStyle { it.chevrons(spacing = 28.0) })
        fun draw(phase: Double) = Bitmap.createBitmap(400, 160, Bitmap.Config.ARGB_8888).also {
            TrailRenderer(path).draw(Canvas(it), effect) { TrailVisualState(dashPhase = phase) }
        }
        val a = draw(0.0); val b = draw(1.0)
        try { assertTrue(a.sameAs(b)) } finally { a.recycle(); b.recycle() }
    }

    @Test fun cometTailGetsBrighterTowardItsHeadWithoutOverlappingCapBeads() {
        val effect = TrailRoutePreset.Comet.effect()
        val frame = render(effect, effect.durationSeconds * .5)
        try {
            // Head is x=200, tail starts x=110 on this 360-unit path.
            for (x in 114 until 196) {
                val previous = Color.green(frame.getPixel(x, 80))
                val next = Color.green(frame.getPixel(x + 1, 80))
                assertTrue("tail brightness must not dip at $x: $previous -> $next", next >= previous - 2)
            }
        } finally { frame.recycle() }
    }

    @Test fun exportReadmeFramesWhenRequested() {
        assumeTrue("Opt-in README capture", InstrumentationRegistry.getArguments().getString("captureRoutePreviews") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.getExternalFilesDir(null), "route-previews").apply { mkdirs() }
        val previewPath = TrailPath(listOf(
            TrailPoint(32.0, 166.0), TrailPoint(80.0, 166.0), TrailPoint(103.0, 133.0),
            TrailPoint(154.0, 133.0), TrailPoint(177.0, 93.0), TrailPoint(228.0, 93.0),
            TrailPoint(253.0, 136.0), TrailPoint(306.0, 136.0), TrailPoint(337.0, 83.0), TrailPoint(388.0, 83.0),
        ))
        val renderer = TrailRenderer(previewPath)
        for ((index, preset) in TrailRoutePreset.entries.withIndex()) {
            val folder = File(root, preset.name).apply { mkdirs() }
            val effect = preset.effect()
            val bitmap = Bitmap.createBitmap(420, 236, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            try {
                for (frame in 0 until 144) {
                    canvas.drawColor(0xFF0C1921.toInt())
                    paint.color = 0xFF152A36.toInt(); paint.strokeWidth = 1f
                    for (x in 12..420 step 24) canvas.drawLine(x.toFloat(), 65f, x.toFloat(), 190f, paint)
                    for (y in 70..190 step 24) canvas.drawLine(16f, y.toFloat(), 404f, y.toFloat(), paint)
                    paint.color = 0xFFE2EBEF.toInt(); paint.textSize = 22f; paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    canvas.drawText(preset.label, 22f, 35f, paint)
                    paint.color = 0xFF32D6AD.toInt(); paint.textSize = 11f; paint.typeface = Typeface.MONOSPACE
                    canvas.drawText("0${index + 1} / TRAIL", 323f, 33f, paint)
                    renderer.draw(canvas, effect) { effect.sample(it, frame / 24.0) }
                    paint.color = 0xFFE2EBEF.toInt(); paint.style = Paint.Style.FILL
                    for (point in listOf(previewPath.points.first(), previewPath.points.last())) {
                        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), 5f, paint)
                    }
                    paint.color = 0xFF93A9B4.toInt(); paint.textSize = 11f; paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    val caption = when (preset) {
                        TrailRoutePreset.MovingDots -> "Direction cues / round dots / continuous flow"
                        TrailRoutePreset.MovingDashes -> "Navigation / moving line segments"
                        TrailRoutePreset.Loading -> "Waiting for directions / travelling window"
                        TrailRoutePreset.Comet -> "Route emphasis / fading tail"
                        TrailRoutePreset.RouteSweep -> "Java-inspired / draw, settle, fade"
                        TrailRoutePreset.DrawAndErase -> "Route discovery / draw, then clear"
                    }
                    canvas.drawText(caption, 22f, 215f, paint)
                    File(folder, "%03d.png".format(frame)).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            } finally { bitmap.recycle() }
        }
    }
}
