package dev.trail.playground.examples

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dev.trail.android.TrailView
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.googlemaps.GoogleMapsTrailOverlay
import dev.trail.plugin.MetroStyle
import dev.trail.plugin.QuadraticReveal
import kotlin.time.Duration.Companion.seconds

// example:start setup
val examplePath = TrailPath(listOf(
    TrailPoint(0.0, 90.0), TrailPoint(60.0, 90.0), TrailPoint(100.0, 30.0),
    TrailPoint(180.0, 30.0), TrailPoint(220.0, 0.0), TrailPoint(280.0, 0.0),
))
val exampleRoute = TrailRoute("demo", listOf(
    TrailCoordinate(37.779, -122.423), TrailCoordinate(37.779, -122.418),
    TrailCoordinate(37.783, -122.418), TrailCoordinate(37.783, -122.412),
    TrailCoordinate(37.787, -122.412),
))
// example:end setup

// example:start preset
val deliveryTrail = trailEffect {
    stroke(TrailColor.Blue, width = 6.0)
    reveal(duration = 2.seconds)
}
// example:end preset

// example:start basic
@Composable fun BasicRouteExample() {
    TrailCanvas(examplePath, Modifier.fillMaxSize(), effect = deliveryTrail)
}
// example:end basic

// example:start runtime-color
@Composable fun RuntimeColorExample(brandColor: TrailColor) {
    val effect = remember(brandColor) {
        trailEffect {
            stroke(brandColor, width = 6.0)
            reveal(duration = 2.seconds)
        }
    }
    TrailCanvas(examplePath, Modifier.fillMaxSize(), effect = effect)
}
// example:end runtime-color

// example:start plugin
@Composable fun PluginExample(brandColor: TrailColor) {
    val effect = remember(brandColor) {
        trailEffect {
            style(MetroStyle(color = brandColor))
            animation(TrailAnimations.custom(QuadraticReveal(), duration = 2.seconds))
        }
    }
    TrailCanvas(examplePath, Modifier.fillMaxSize(), effect = effect)
}
// example:end plugin

// example:start playback
@Composable fun PlaybackExample() {
    val playback = rememberTrailPlayback(deliveryTrail)
    Column {
        TrailCanvas(examplePath, Modifier.weight(1f).fillMaxWidth(), deliveryTrail, playback)
        Slider(playback.progress.toFloat(), onValueChange = { playback.seek(it.toDouble()) }, modifier = Modifier.testTag("exampleProgress"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (playback.isPlaying) playback.pause() else playback.play() }, modifier = Modifier.testTag("examplePlayPause")) {
                Text(if (playback.isPlaying) "Pause" else "Play")
            }
            OutlinedButton(onClick = playback::replay, modifier = Modifier.testTag("exampleReplay")) { Text("Replay") }
        }
    }
}
// example:end playback

// example:start sequence
val revealThenErase = trailEffect {
    stroke(TrailColor.Mint, width = 6.0)
    sequence {
        reveal(duration = 2.seconds)
        erase(duration = 1.seconds)
    }
}

@Composable fun SequenceExample() {
    TrailCanvas(examplePath, Modifier.fillMaxSize(), effect = revealThenErase)
}
// example:end sequence

// example:start layers
val highlightedRoute = trailEffect {
    layer { stroke(TrailColor.White, width = 10.0) }
    layer {
        stroke(TrailColor.Blue, width = 6.0)
        reveal(duration = 2.seconds)
    }
}

@Composable fun LayersExample() {
    TrailCanvas(examplePath, Modifier.fillMaxSize(), effect = highlightedRoute)
}
// example:end layers

// example:start google-maps
@Composable fun GoogleMapsExample() {
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.783, -122.417), 14f)
    }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(Modifier.matchParentSize(), cameraPositionState = camera)
        GoogleMapsTrailOverlay(
            route = exampleRoute,
            cameraPositionState = camera,
            modifier = Modifier.matchParentSize(),
            effect = deliveryTrail,
        )
    }
}
// example:end google-maps

// example:start android-view
// In an existing Activity, use setContentView(createTrailView(this)).
fun createTrailView(context: Context): TrailView = TrailView(context).apply {
    path = examplePath
    effect = deliveryTrail
}
// example:end android-view

@Composable fun AndroidViewExample() {
    AndroidView(factory = ::createTrailView, modifier = Modifier.fillMaxSize())
}
