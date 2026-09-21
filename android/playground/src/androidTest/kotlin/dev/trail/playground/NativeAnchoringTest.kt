package dev.trail.playground

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.GoogleMap as NativeMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.googlemaps.*
import dev.trail.effects.TrailRoutePreset
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

/** SDK snapshots contain only native map content: a drifting sibling Canvas cannot pass this. */
@OptIn(MapsComposeExperimentalApi::class)
class NativeAnchoringTest {
    @get:Rule val compose=createComposeRule()
    @Test fun movingDotsAreRoundAndMoveForwardOnTheNativeMap() {
        assumeTrue("A configured key is required for native Google verification", BuildConfig.HAS_MAPS_KEY)
        compose.mainClock.autoAdvance = false
        val route = TrailRoute.direct("dots", TrailCoordinate(40.75, -73.987), TrailCoordinate(40.75, -73.981))
        val effect = TrailRoutePreset.MovingDots.effect(color = TrailColor(0xffff00ff.toInt()), width = 12.0)
        lateinit var playback: TrailPlayback
        var map: NativeMap? = null
        var loaded = false
        var density = 1f
        compose.setContent {
            density = LocalDensity.current.density
            val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(40.75, -73.984), 16f) }
            playback = rememberTrailPlayback(effect, autoPlay = false)
            GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera, onMapLoaded = { loaded = true },
                properties = MapProperties(mapType = MapType.NONE)) {
                MapEffect(Unit) { map = it }
                GoogleMapsTrail(route, camera, effect, playback)
            }
        }
        compose.waitUntil(30_000) { compose.mainClock.advanceTimeBy(32); loaded && map != null }
        var previousCenters = emptyList<Double>()
        for (progress in listOf(.25, .75)) {
            compose.runOnIdle { playback.seek(progress) }; compose.mainClock.advanceTimeBy(64)
            fun snapshot(): Bitmap {
                var shot: Bitmap? = null
                compose.runOnIdle { map!!.snapshot { shot = it } }
                compose.waitUntil(10_000) { shot != null }
                return shot!!
            }
            val y = compose.runOnIdle { map!!.projection.toScreenLocation(LatLng(40.75, -73.984)).y }
            fun magenta(color: Int) = (color ushr 16 and 255) > 180 && (color ushr 8 and 255) < 90 && (color and 255) > 180
            fun centers(bitmap: Bitmap): List<Pair<Double, Int>> {
                val runs = ArrayList<Pair<Double, Int>>()
                var start = -1
                for (x in 0 until bitmap.width) {
                    if (magenta(bitmap.getPixel(x, y))) { if (start < 0) start = x }
                    else if (start >= 0) { runs.add((start + x - 1) / 2.0 to x - start); start = -1 }
                }
                return runs.filter { it.first > 100 * density && it.first < bitmap.width - 100 * density }
            }
            // Maps has a separate renderer clock. Poll snapshots until it displays the new phase.
            var accepted: Bitmap? = null
            compose.waitUntil(10_000) {
                val shot = snapshot()
                val dots = centers(shot)
                val moved = previousCenters.isEmpty() || dots.any { dot -> previousCenters.all { abs(it - dot.first) > 4 } }
                if (dots.size >= 2 && moved) { accepted = shot; true } else { shot.recycle(); false }
            }
            val shot = accepted!!
            try {
                val dots = centers(shot)
                dots.forEach { (center, width) ->
                    assertTrue("native dot width=$width, density=$density", width in (10 * density).toInt()..(14 * density).toInt())
                    val height = (y - (10 * density).toInt()..y + (10 * density).toInt()).count { magenta(shot.getPixel(center.toInt(), it)) }
                    assertTrue("dot must be round: $width x $height", abs(width - height) <= 3)
                }
                if (previousCenters.isNotEmpty()) {
                    // Half a dot+gap period, in the origin-to-destination direction.
                    val expected = previousCenters.first() + 18 * density
                    assertTrue("dots move forward by half a spacing", dots.any { abs(it.first - expected) < 4 })
                }
                previousCenters = dots.map { it.first }
            } finally { shot.recycle() }
        }
    }

    @Test fun nativeRouteAndVehiclePixelsStayAtGeographicPointsAcrossZigzagZoomBearingAndTilt() {
        assumeTrue("A configured key is required for native Google verification",BuildConfig.HAS_MAPS_KEY)
        compose.mainClock.autoAdvance=false
        val route=TrailRoute("road",listOf(TrailCoordinate(40.750,-73.985),TrailCoordinate(40.755,-73.985),TrailCoordinate(40.755,-73.977)))
        val geometry=TrailMapGeometry(route)
        val effect=trailEffect { stroke(TrailColor(0xffff00ff.toInt()),8.0); reveal(10.seconds) }
        lateinit var camera: CameraPositionState
        lateinit var playback: TrailPlayback
        var map: NativeMap?=null
        var loaded=false
        compose.setContent {
            camera=rememberCameraPositionState { position=CameraPosition.fromLatLngZoom(LatLng(40.754,-73.983),15f) }
            playback=rememberTrailPlayback(effect,autoPlay=false)
            val density=LocalDensity.current.density
            GoogleMap(Modifier.fillMaxSize(),cameraPositionState=camera,onMapLoaded={ loaded=true },properties=MapProperties(mapType=MapType.NONE)) {
                MapEffect(Unit) { map=it }
                val icon=remember { BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap((14*density).toInt(),(14*density).toInt(),Bitmap.Config.ARGB_8888).apply { eraseColor(0xff00ffff.toInt()) }) }
                GoogleMapsTrail(route,camera,effect,playback,GoogleMapsTrailVehicle(icon))
            }
        }
        compose.waitUntil(30_000) { compose.mainClock.advanceTimeBy(32); loaded && map!=null }
        compose.runOnIdle { playback.seek(.65) }; compose.mainClock.advanceTimeBy(64)
        val trace=geometry.poseAt(.2)!!.coordinate; val head=geometry.poseAt(.65)!!.coordinate
        for(index in 0 until 12) {
            val target=CameraPosition(LatLng(40.754+(if(index%2==0) .0004 else -.0004),-73.982),
                if(index%3==0) 14f else 15f,if(index%2==0) 0f else 40f,listOf(0f,45f,180f)[index%3])
            compose.runOnIdle { camera.move(CameraUpdateFactory.newCameraPosition(target)) }
            compose.waitUntil(10_000) {
                compose.mainClock.advanceTimeBy(32)
                compose.runOnIdle { !camera.isMoving && abs(camera.position.bearing-target.bearing)<.01 && abs(camera.position.zoom-target.zoom)<.01 && abs(camera.position.target.latitude-target.target.latitude)<1e-6 }
            }
            var shot: Bitmap?=null
            compose.runOnIdle { map!!.snapshot { shot=it } }
            compose.waitUntil(10_000) { shot!=null }
            compose.runOnIdle {
                val bitmap=shot!!
                fun containsColor(c: TrailCoordinate, magenta: Boolean): Boolean {
                    val p=map!!.projection.toScreenLocation(LatLng(c.latitude,c.longitude))
                    for(y in (p.y-4).coerceAtLeast(0)..(p.y+4).coerceAtMost(bitmap.height-1))
                        for(x in (p.x-4).coerceAtLeast(0)..(p.x+4).coerceAtMost(bitmap.width-1)) {
                            val color=bitmap.getPixel(x,y); val r=(color ushr 16) and 255; val g=(color ushr 8) and 255; val b=color and 255
                            if(b>180 && if(magenta) r>180 && g<90 else g>180 && r<90) return true
                        }
                    return false
                }
                assertTrue("native route pixel at camera $index",containsColor(trace,true))
                assertTrue("native vehicle pixel at camera $index",containsColor(head,false))
                assertEquals(.65,playback.progress,1e-12); assertFalse(playback.isPlaying)
                bitmap.recycle()
            }
        }
    }
}
