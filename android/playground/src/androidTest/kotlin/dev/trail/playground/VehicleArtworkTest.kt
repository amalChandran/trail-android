package dev.trail.playground

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import dev.trail.core.*
import dev.trail.effects.TrailMotionPreset
import dev.trail.playground.examples.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.*

/** Independent front-of-vehicle color probes catch incorrect pivots and 90/180-degree offsets. */
@RunWith(Parameterized::class)
class VehicleArtworkTest(private val id: String,private val degrees: Int,private val density: Double) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}/heading={1}/density={2}") fun cases() =
            listOf("cab","flight","ferry").flatMap { id -> listOf(0,90,180,270).flatMap { angle ->
                listOf(2.0,3.0).map { arrayOf<Any>(id,angle,it) }
            } }
    }
    @Test fun topDownArtworkFacesItsRouteHeadingAndKeepsTheCorrectAnchor() {
        val art=loadVehicleArtwork(InstrumentationRegistry.getInstrumentation().targetContext).getValue(id)
        val bitmap=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
        val angle=degrees*PI/180
        art.draw(Canvas(bitmap),TrailPose(TrailPoint(128/density,128/density),angle,0),density)
        val probeY=when(id) { "cab" -> 19.0; "flight" -> 18.0; else -> 44.0 }
        val expected=Color.parseColor(when(id) { "cab" -> "#FFCC66"; "flight" -> "#203F50"; else -> "#92B8CC" })
        val distance=(art.anchor.y-probeY)*art.size/art.height*density
        val x=(128+cos(angle)*distance).roundToInt(); val y=(128+sin(angle)*distance).roundToInt()
        val error=(-2..2).flatMap { dx -> (-2..2).map { dy -> bitmap.getPixel(x+dx,y+dy) } }.minOf { pixel ->
            abs(Color.red(pixel)-Color.red(expected))+abs(Color.green(pixel)-Color.green(expected))+abs(Color.blue(pixel)-Color.blue(expected))
        }
        assertTrue("Front probe must rotate with heading, RGB error=$error",error<=6)
        assertTrue("Vehicle stays centered on the route anchor",Color.alpha(bitmap.getPixel(128,128))>240)
        assertEquals("No background badge or full-canvas fill",0,Color.alpha(bitmap.getPixel(0,0)))
        val spec=art.animation(TrailMotionPreset.Reveal)
        val player=TrailPlayer(trailEffect { animation(spec) })
        player.seek(.3); val paused=player.frame(0).head
        player.advance(5.0); assertEquals(paused,player.frame(0).head)
        assertEquals(journeyProgress(.3),paused!!,1e-9)
        player.replay(); assertEquals(0.0,player.frame(0).head!!,1e-9)
        bitmap.recycle()
    }
}
