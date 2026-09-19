package dev.trail.playground

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.googlemaps.GoogleMapsTrailOverlay
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** An explicit skipped test without a key; compile-only validation is not a live-map pass. */
class GoogleMapsContractTest {
    @get:Rule val compose=createComposeRule()
    @Test fun liveGoogleProjectionTracksZoomBearingTiltAndPreservesPlayback() {
        assumeTrue("Configure a restricted MAPS_API_KEY to run the live Google Maps contract",BuildConfig.HAS_MAPS_KEY)
        compose.mainClock.autoAdvance=false
        val a=TrailCoordinate(40.758,-73.9855); val b=TrailCoordinate(40.7527,-73.9772)
        var route by mutableStateOf(TrailRoute.direct("cab",a,b))
        val effect=trailEffect { reveal(8.seconds) }
        lateinit var camera: CameraPositionState
        lateinit var playback: TrailPlayback
        var projected: TrailPath?=null
        var loaded=false; var density=1f
        compose.setContent {
            density=LocalDensity.current.density
            camera=rememberCameraPositionState { position=CameraPosition.fromLatLngZoom(LatLng(a.latitude,a.longitude),14f) }
            playback=rememberTrailPlayback(effect,autoPlay=false)
            Box(Modifier.fillMaxSize()) {
                GoogleMap(Modifier.matchParentSize(),cameraPositionState=camera,onMapLoaded={ loaded=true })
                GoogleMapsTrailOverlay(route,camera,Modifier.matchParentSize(),effect,playback,onProjected={ projected=it })
            }
        }
        compose.waitUntil(30_000) { compose.mainClock.advanceTimeBy(32); loaded && projected!=null }
        compose.runOnIdle { playback.seek(.37) }
        for(zoom in listOf(12f,16f)) for(bearing in listOf(0f,45f,180f)) for(tilt in listOf(0f,40f)) {
            compose.runOnIdle { camera.move(CameraUpdateFactory.newCameraPosition(CameraPosition(LatLng(a.latitude,a.longitude),zoom,tilt,bearing))) }
            compose.mainClock.advanceTimeBy(200)
            compose.runOnIdle {
                val native=camera.projection!!.toScreenLocation(LatLng(b.latitude,b.longitude))
                val actual=projected!!.points.last()
                assertEquals(native.x/density.toDouble(),actual.x,1.0)
                assertEquals(native.y/density.toDouble(),actual.y,1.0)
                assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
            }
        }
        compose.runOnIdle {
            route=TrailRoute.direct("seam",TrailCoordinate(0.0,170.0),TrailCoordinate(0.0,-170.0))
            camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(0.0,0.0),1f))
        }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            val path=projected!!; val sdk=camera.projection!!
            val quarterWorld=kotlin.math.abs(sdk.toScreenLocation(LatLng(0.0,90.0)).x-sdk.toScreenLocation(LatLng(0.0,0.0)).x)/density
            assertEquals(1,path.breakBefore.size)
            assertTrue(path.slices(0.0,1.0).all { TrailPath(it.points).length<quarterWorld })
            assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
        }
    }
}
