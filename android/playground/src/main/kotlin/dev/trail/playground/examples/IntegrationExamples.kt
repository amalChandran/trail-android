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
import dev.trail.googlemaps.GoogleMapsTrail
import kotlinx.coroutines.CancellationException
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

// example:start geographic-routes
val jfk = TrailCoordinate(40.6413, -73.7781)
val heathrow = TrailCoordinate(51.4706, -0.461941)
val flightArc = TrailRoute.arc("JFK-LHR/arc", jfk, heathrow)
val flightGeodesic = TrailRoute.greatCircle("JFK-LHR/great-circle", jfk, heathrow)
val flightDirect = TrailRoute.direct("JFK-LHR/direct", jfk, heathrow)

// Pass every waypoint from your directions service, in order. Trail does not fetch directions.
fun cabRoute(waypoints: List<TrailCoordinate>, revision: Long) =
    TrailRoute("cab/current-trip", waypoints, revision)

// The same model works with an encoded route response; precision must match the service.
fun decodedCabRoute(encoded: String) = TrailRoute.encodedPolyline("cab/decoded", encoded, precision = 5)
// example:end geographic-routes

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
    GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera) {
        GoogleMapsTrail(exampleRoute, camera, effect = deliveryTrail)
    }
}
// example:end google-maps

// example:start custom-map-adapter
// An adapter supplies local dp, or null until ready, and changes cameraRevision on camera/inset changes.
// Place this above your map with identical bounds; no SDK types enter the route or effect.
@Composable fun CustomMapOverlayExample(route: TrailRoute, projection: TrailProjection?, cameraRevision: Int) {
    TrailProjectedOverlay(route, projection, cameraRevision, Modifier.fillMaxSize(), effect = deliveryTrail)
}
// example:end custom-map-adapter

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

// example:start loading-route
@Composable fun DirectionsLoadingExample(
    from: TrailCoordinate, to: TrailCoordinate, fetchRoute: suspend () -> TrailRoute,
) {
    val transition = rememberTrailRouteTransition(from, to)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(from.latitude, from.longitude), 14f)
    }
    val effect = remember(transition.phase) { trailEffect {
        stroke(TrailColor.Blue, 6.0)
        when (transition.phase) {
            TrailRoutePhase.Loading -> animation(TrailAnimations.loading())
            TrailRoutePhase.Ready -> reveal(2.seconds)
            else -> Unit // Full line while morphing; static arc on failure/cancellation.
        }
    } }
    LaunchedEffect(transition) {
        val request = transition.request
        try { transition.resolve(request, fetchRoute()) }
        catch (cancelled: CancellationException) { transition.cancel(request); throw cancelled }
        catch (failure: Exception) { transition.fail(request) } // App presents its error/retry UI.
    }
    GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera) {
        GoogleMapsTrail(transition.route, camera, effect = effect)
    }
}
// example:end loading-route
