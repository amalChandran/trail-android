package dev.trail.playground

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.trail.compose.*
import dev.trail.core.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MapBindingTest {
    @get:Rule val compose=createComposeRule()
    @Test fun suppliedPlaybackKeepsItsEffectWhenTheEffectArgumentIsOmitted() {
        val route=TrailRoute.direct("controller",TrailCoordinate(0.0,0.0),TrailCoordinate(0.0,1.0))
        var effect by mutableStateOf(trailEffect { stroke(TrailColor.Coral,12.0); reveal(8.seconds) })
        lateinit var playback: TrailPlayback
        compose.setContent {
            playback=rememberTrailPlayback(effect,autoPlay=false)
            TrailProjectedOverlay(route,TrailProjection { TrailPoint(it.longitude*200,50.0) },0,
                Modifier.size(220.dp,100.dp).testTag("controlled"),playback=playback,reducedMotion=true)
        }
        val pixels=compose.onNodeWithTag("controlled").captureToImage().toPixelMap()
        assertEquals(Color(0xFFFF785A),pixels[pixels.width/2,pixels.height/2])
        compose.runOnIdle { assertFalse(playback.isPlaying); playback.seek(.4) }
        compose.runOnIdle { assertEquals(.4,playback.frame().windows.single().end,1e-9) }
        compose.runOnIdle { effect=trailEffect { stroke(TrailColor.Mint,12.0); reveal(4.seconds) } }
        val updated=compose.onNodeWithTag("controlled").captureToImage().toPixelMap()
        assertEquals(Color(0xFF32D6AD),updated[updated.width/2,updated.height/2])
        compose.runOnIdle { assertEquals(.4,playback.progress,1e-9); assertFalse(playback.isPlaying) }
    }
    @Test fun projectedRoutesAreClippedToTheMapViewport() {
        val route=TrailRoute.direct("outside",TrailCoordinate(0.0,0.0),TrailCoordinate(0.0,1.0))
        compose.setContent {
            Box(Modifier.size(240.dp,100.dp).background(Color.Green).testTag("host")) {
                TrailProjectedOverlay(route,TrailProjection { TrailPoint(it.longitude*400,50.0) },0,
                    Modifier.size(120.dp,100.dp),effect=trailEffect { stroke(TrailColor.Blue,12.0) },reducedMotion=true)
            }
        }
        val pixels=compose.onNodeWithTag("host").captureToImage().toPixelMap()
        assertEquals(Color(0xFF2563EB),pixels[pixels.width/4,pixels.height/2])
        assertEquals(Color.Green,pixels[pixels.width*3/4,pixels.height/2])
    }
    @Test fun missingProjectionReducedMotionBackgroundAndDisposalStopTheClock() {
        compose.mainClock.autoAdvance=false
        val route=TrailRoute.direct("route",TrailCoordinate(0.0,0.0),TrailCoordinate(1.0,1.0))
        val effect=trailEffect { reveal(8.seconds,repeat=true) }
        var projection by mutableStateOf<TrailProjection?>(TrailProjection { TrailPoint(it.longitude*100,it.latitude*100) })
        var reduced by mutableStateOf(false)
        var mounted by mutableStateOf(true)
        lateinit var playback: TrailPlayback
        lateinit var lifecycle: LifecycleRegistry
        compose.setContent {
            val owner=remember { object: LifecycleOwner {
                override val lifecycle=LifecycleRegistry(this).apply { currentState=Lifecycle.State.RESUMED }
            } }
            lifecycle=owner.lifecycle
            playback=rememberTrailPlayback(effect)
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if(mounted) TrailProjectedOverlay(route,projection,0,Modifier.size(240.dp),effect,playback,reduced)
            }
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertTrue(playback.progress>0); projection=null }
        compose.mainClock.advanceTimeBy(32)
        var frozen=0.0
        compose.runOnIdle { frozen=playback.progress }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(frozen,playback.progress,1e-12); projection=TrailProjection { TrailPoint(it.longitude*100,it.latitude*100) } }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertTrue(playback.progress>frozen); reduced=true }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { frozen=playback.progress }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(frozen,playback.progress,1e-12); reduced=false }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertTrue(playback.progress>frozen); lifecycle.currentState=Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { frozen=playback.progress }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(frozen,playback.progress,1e-12); lifecycle.currentState=Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertTrue(playback.progress>frozen); mounted=false }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { frozen=playback.progress }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(frozen,playback.progress,1e-12) }
    }

    @Test fun cameraAndRouteUpdatesPreserveAnExternalPlayersPauseAndSeek() {
        compose.mainClock.autoAdvance=false
        var revision by mutableIntStateOf(0)
        var scale=100.0
        var route by mutableStateOf(TrailRoute.direct("route",TrailCoordinate(0.0,0.0),TrailCoordinate(1.0,1.0)))
        val projection=TrailProjection { TrailPoint(it.longitude*scale,it.latitude*scale) }
        val effect=trailEffect { reveal(8.seconds) }
        lateinit var playback: TrailPlayback
        var path: TrailPath?=null
        compose.setContent {
            playback=rememberTrailPlayback(effect,autoPlay=false)
            TrailProjectedOverlay(route,projection,revision,Modifier.size(240.dp),effect,playback,onProjected={ path=it })
        }
        compose.mainClock.advanceTimeBy(100)
        var originalLength=0.0
        compose.runOnIdle { originalLength=path!!.length; playback.seek(.37); scale=200.0; revision++ }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle {
            assertEquals(originalLength*2,path!!.length,1e-9)
            assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
            route=TrailRoute.direct("route",TrailCoordinate(0.0,0.0),TrailCoordinate(2.0,2.0),revision=1)
        }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle {
            assertEquals(originalLength*4,path!!.length,1e-9)
            assertEquals(.37,playback.progress,1e-12); assertFalse(playback.isPlaying)
        }
    }
}
