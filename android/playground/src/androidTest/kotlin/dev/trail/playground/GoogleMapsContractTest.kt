package dev.trail.playground

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.googlemaps.GoogleMapsTrailOverlay
import dev.trail.playground.examples.GoogleJourneyMap
import dev.trail.playground.examples.loadJourneys
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
                GoogleMap(Modifier.matchParentSize(),cameraPositionState=camera,
                    properties=MapProperties(minZoomPreference=0f),onMapLoaded={ loaded=true })
                GoogleMapsTrailOverlay(route,camera,Modifier.matchParentSize(),effect,playback,onProjected={ projected=it })
            }
        }
        compose.waitUntil(30_000) { compose.mainClock.advanceTimeBy(32); loaded && projected!=null }
        fun moveCamera(target: CameraPosition) {
            compose.runOnIdle { camera.move(CameraUpdateFactory.newCameraPosition(target)) }
            // Maps renders on its own clock. Advancing Compose's virtual clock alone does
            // not wait for the native camera callbacks and the resulting recomposition.
            try {
                compose.waitUntil(10_000) {
                    compose.mainClock.advanceTimeBy(32)
                    compose.runOnIdle {
                        val position = camera.position
                        !camera.isMoving &&
                            kotlin.math.abs(position.target.latitude - target.target.latitude) < 1e-5 &&
                            kotlin.math.abs(position.target.longitude - target.target.longitude) < 1e-5 &&
                            kotlin.math.abs(position.zoom - target.zoom) < .01f &&
                            kotlin.math.abs(position.tilt - target.tilt) < .01f &&
                            kotlin.math.abs(position.bearing - target.bearing) < .01f
                    }
                }
            } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError("Native camera did not settle at $target; actual=${camera.position}, moving=${camera.isMoving}", failure)
            }
            compose.mainClock.advanceTimeBy(32)
        }
        compose.runOnIdle { playback.seek(.37) }
        for(zoom in listOf(14f,16f)) for(bearing in listOf(0f,45f,180f)) for(tilt in listOf(0f,40f)) {
            moveCamera(CameraPosition(LatLng(a.latitude,a.longitude),zoom,tilt,bearing))
            compose.runOnIdle {
                val native=camera.projection!!.toScreenLocation(LatLng(b.latitude,b.longitude))
                val actual=projected!!.points.last()
                val label="zoom=$zoom bearing=$bearing tilt=$tilt actual=${camera.position}"
                assertEquals("$label x",native.x/density.toDouble(),actual.x,1.0)
                assertEquals("$label y",native.y/density.toDouble(),actual.y,1.0)
                assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
            }
        }
        compose.runOnIdle {
            route=TrailRoute.direct("seam",TrailCoordinate(0.0,170.0),TrailCoordinate(0.0,-170.0))
        }
        // The tall full-screen map needs a higher zoom than the short journey viewport.
        moveCamera(CameraPosition.fromLatLngZoom(LatLng(0.0,0.0),3f))
        compose.runOnIdle {
            val path=projected!!; val sdk=camera.projection!!
            val quarterWorld=kotlin.math.abs(sdk.toScreenLocation(LatLng(0.0,90.0)).x-sdk.toScreenLocation(LatLng(0.0,0.0)).x)/density
            assertEquals(1,path.breakBefore.size)
            assertTrue(path.slices(0.0,1.0).all { TrailPath(it.points).length<quarterWorld })
            assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
        }
    }

    @Test fun journeyCamerasFitEntireFlightCabAndFerryRoutes() {
        assumeTrue("Configure MAPS_API_KEY to verify live journey camera fitting", BuildConfig.HAS_MAPS_KEY)
        compose.mainClock.autoAdvance=false
        val journeys=loadJourneys(InstrumentationRegistry.getInstrumentation().targetContext)
        val routes=journeys.map { it.route(it.defaultGeometry) }
        var selected by mutableIntStateOf(0)
        lateinit var camera: CameraPositionState
        var viewport=IntSize.Zero
        var paddingPixels=0.0
        val effect=trailEffect { stroke(TrailColor.Blue,6.0) }
        compose.setContent {
            camera=rememberCameraPositionState()
            val playback=rememberTrailPlayback(effect,autoPlay=false)
            paddingPixels=with(LocalDensity.current) { 44.dp.toPx().toDouble() }
            Box(Modifier.fillMaxSize().padding(18.dp)) {
                Box(Modifier.fillMaxWidth().height(330.dp).onSizeChanged { viewport=it }) {
                    GoogleJourneyMap(routes[selected],journeys[selected],effect,playback,true,camera)
                }
            }
        }
        journeys.indices.forEach { index ->
            compose.runOnIdle { selected=index }
            try {
                compose.waitUntil(30_000) {
                    compose.mainClock.advanceTimeBy(32)
                    val loaded=compose.onAllNodesWithText("Google Maps · ready").fetchSemanticsNodes().isNotEmpty()
                    loaded && compose.runOnIdle {
                        val projection=camera.projection
                        if(projection==null || camera.isMoving || viewport==IntSize.Zero) false
                        else {
                            val points=routes[index].coordinates.map { projection.toScreenLocation(LatLng(it.latitude,it.longitude)) }
                            val inside=points.all {
                                it.x>=paddingPixels-2 && it.x<=viewport.width-paddingPixels+2 &&
                                    it.y>=paddingPixels-2 && it.y<=viewport.height-paddingPixels+2
                            }
                            // A world overview can contain a city route without actually fitting it.
                            // At least one dimension must fill the space between the requested insets.
                            val width=(points.maxOf { it.x }-points.minOf { it.x })/(viewport.width-2*paddingPixels)
                            val height=(points.maxOf { it.y }-points.minOf { it.y })/(viewport.height-2*paddingPixels)
                            inside && maxOf(width,height) in .97..1.02
                        }
                    }
                }
            } catch(failure: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError("${journeys[index].id}: complete route did not fit inside the map; camera=${camera.position}",failure)
            }
        }
    }
}
