package dev.trail.playground

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import dev.trail.android.TrailRenderer
import dev.trail.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

/** Real Android Canvas pixels, independently checked against a horizontal-stroke oracle. */
@RunWith(Parameterized::class)
class RendererContractTest(private val case: Case) {
    data class Case(val width: Double, val color: TrailColor, val opacity: Double, val erase: Boolean, val progress: Double) {
        override fun toString() = "${if(erase) "erase" else "reveal"}/$progress/width=$width/color=${color.argb}/alpha=$opacity"
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}") fun cases(): List<Array<Case>> =
            listOf(2.0,6.0,12.0).flatMap { w -> listOf(TrailColor.Blue,TrailColor.Mint,TrailColor.Coral).flatMap { c ->
                listOf(.25,1.0).flatMap { a -> listOf(false,true).flatMap { e -> listOf(0.0,.25,.75,1.0).map { p -> arrayOf(Case(w,c,a,e,p)) } } }
            } }
    }
    @Test fun pixelsRespectWindowWidthColorAndOpacity() {
        val effect=trailEffect { stroke(case.color,case.width); if(case.erase) erase() else reveal() }
        val bitmap=Bitmap.createBitmap(220,160,Bitmap.Config.ARGB_8888)
        try {
            val frame=effect.sample(0,case.progress*2)
            TrailRenderer(TrailPath(listOf(TrailPoint(20.0,80.0),TrailPoint(180.0,80.0)))).draw(Canvas(bitmap),effect) {
                TrailVisualState(frame.windows,opacity=case.opacity)
            }
            val start=if(case.erase) 20+160*case.progress else 20.0
            val end=if(case.erase) 180.0 else 20+160*case.progress
            for(x in 0 until 220) {
                val center=x+.5
                if(start < end && center>start+case.width && center<end-case.width) {
                    val pixel=bitmap.getPixel(x,80)
                    assertEquals("alpha at $x",(255*case.opacity).toInt(),Color.alpha(pixel),2)
                    assertTrue(abs(Color.red(pixel)-Color.red(case.color.argb))<=5)
                    assertTrue(abs(Color.green(pixel)-Color.green(case.color.argb))<=5)
                    assertTrue(abs(Color.blue(pixel)-Color.blue(case.color.argb))<=5)
                }
                if(start==end || center<start-case.width || center>end+case.width) assertEquals("outside window at $x",0,Color.alpha(bitmap.getPixel(x,80)))
                assertEquals("outside stroke width",0,Color.alpha(bitmap.getPixel(x,(80+case.width/2+2).toInt())))
            }
        } finally { bitmap.recycle() }
    }
    private fun assertEquals(message: String, expected: Int, actual: Int, tolerance: Int) {
        assertTrue("$message: expected $expected, actual $actual",abs(expected-actual)<=tolerance)
    }
}

class RendererSeamTest {
    @Test fun revealNeverDrawsAcrossDisconnectedContours() {
        val path=TrailPath(listOf(TrailPoint(10.0,80.0),TrailPoint(50.0,80.0),TrailPoint(170.0,80.0),TrailPoint(210.0,80.0)),setOf(2))
        val bitmap=Bitmap.createBitmap(220,160,Bitmap.Config.ARGB_8888)
        try {
            TrailRenderer(path).draw(Canvas(bitmap),trailEffect { stroke(TrailColor.Blue) }) { TrailVisualState.reveal(.75) }
            assertTrue(Color.alpha(bitmap.getPixel(30,80))>0)
            assertTrue(Color.alpha(bitmap.getPixel(180,80))>0)
            assertEquals(0,Color.alpha(bitmap.getPixel(100,80)))
            assertEquals(0,Color.alpha(bitmap.getPixel(205,80)))
        } finally { bitmap.recycle() }
    }
}
