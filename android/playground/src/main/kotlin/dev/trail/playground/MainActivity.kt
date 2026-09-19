package dev.trail.playground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.effects.*
import dev.trail.googlemaps.GoogleMapsTrailOverlay
import dev.trail.plugin.*
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { TrailStudio() } }
}

private val Ink = Color(0xFF0C1921)
private val Surface = Color(0xFF132631)
private val Mint = Color(0xFF32D6AD)
private val Route = TrailPath(listOf(
    TrailPoint(0.0, 85.0), TrailPoint(30.0, 85.0), TrailPoint(44.0, 66.0), TrailPoint(68.0, 66.0),
    TrailPoint(84.0, 40.0), TrailPoint(120.0, 40.0), TrailPoint(140.0, 17.0), TrailPoint(170.0, 17.0),
    TrailPoint(190.0, 44.0), TrailPoint(222.0, 44.0), TrailPoint(246.0, 0.0), TrailPoint(280.0, 0.0),
))
private val MapRoute = TrailRoute("san-francisco", listOf(
    TrailCoordinate(37.779, -122.423), TrailCoordinate(37.779, -122.418), TrailCoordinate(37.783, -122.418),
    TrailCoordinate(37.783, -122.412), TrailCoordinate(37.787, -122.412), TrailCoordinate(37.790, -122.405),
))

@Composable fun TrailStudio() {
    var style by remember { mutableStateOf(TrailStylePreset.Cased) }
    var motion by remember { mutableStateOf(TrailMotionPreset.Reveal) }
    var duration by remember { mutableFloatStateOf(3f) }
    var plugin by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(true) }
    var reduced by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    val effect = remember(style, motion, duration, plugin, repeat) {
        if (plugin) trailEffect { style(MetroStyle()); animation(TrailAnimations.custom(QuadraticReveal(), duration.toDouble().seconds, repeat)) }
        else motion.effect(style.style(), duration.toDouble().seconds, repeat)
    }
    val playback = rememberTrailPlayback(effect)
    LaunchedEffect(effect) { playback.replay() }
    val reducedMotion = reduced || rememberSystemReducedMotion()
    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, onPrimary = Ink, background = Ink, onBackground = Color(0xFFE2EBEF), surface = Surface, onSurface = Color(0xFFE2EBEF))) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().background(Ink).systemBarsPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("trail.", fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
                Text("STUDIO / 2.0", color = Mint, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text("A little motion.\nA clear direction.", fontSize = 26.sp, fontWeight = FontWeight.Medium, lineHeight = 31.sp)
            Text("Native Kotlin · public plugins · live preview", color = Color(0xFF93A9B4), fontSize = 12.sp)
            Surface(shape = RoundedCornerShape(22.dp), color = Surface) {
                Box(Modifier.fillMaxWidth().height(235.dp).testTag("preview")) {
                    if (showMap && BuildConfig.HAS_MAPS_KEY) {
                        val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(37.784, -122.414), 13.5f) }
                        GoogleMap(Modifier.matchParentSize(), cameraPositionState = camera)
                        GoogleMapsTrailOverlay(MapRoute, camera, Modifier.matchParentSize(), effect, playback, reducedMotion)
                    } else {
                        Canvas(Modifier.matchParentSize()) {
                            val step = 22.dp.toPx()
                            var x = 0f
                            while (x <= size.width) { drawLine(Color(0xFF20343F), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                            var y = 0f
                            while (y <= size.height) { drawLine(Color(0xFF20343F), Offset(0f, y), Offset(size.width, y), 1f); y += step }
                        }
                        TrailCanvas(Route, Modifier.matchParentSize(), effect, playback, reducedMotion = reducedMotion)
                        Text(if (reducedMotion) "REDUCED MOTION" else "LIVE CANVAS", Modifier.align(Alignment.TopStart).padding(16.dp), color = Color(0xFF91AFBC), fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(if (plugin) "YOUR PLUGIN" else "${style.label.uppercase()} / ${motion.label.uppercase()}", Modifier.align(Alignment.BottomStart).padding(16.dp), color = Mint, fontSize = 10.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { if (playback.isPlaying) playback.pause() else playback.play() }, modifier = Modifier.weight(1f).testTag("playPause")) {
                    Text(if (playback.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(onClick = playback::replay, modifier = Modifier.weight(1f).testTag("replay")) { Text("Replay") }
            }
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PROGRESS", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFF93A9B4))
                    Text("${(playback.progress * 100).toInt()}%", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Slider(value = playback.progress.toFloat(), onValueChange = { playback.seek(it.toDouble()) }, modifier = Modifier.testTag("progress"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Choice("LINE STYLE", style.label, TrailStylePreset.entries.map { it.label }, Modifier.weight(1f)) { style = TrailStylePreset.entries[it]; plugin = false }
                Choice("ANIMATION", motion.label, TrailMotionPreset.entries.map { it.label }, Modifier.weight(1f)) { motion = TrailMotionPreset.entries[it]; plugin = false }
            }
            Text("Duration  ${"%.1f".format(duration)} s", fontSize = 13.sp)
            Slider(duration, onValueChange = { duration = it }, valueRange = 1f..8f, steps = 13, modifier = Modifier.testTag("duration"))
            Toggle("Loop animation", repeat) { repeat = it }
            Toggle("Reduced motion", reduced) { reduced = it }
            Toggle("Use my Metro plugin", plugin) { plugin = it }
            if (BuildConfig.HAS_MAPS_KEY) Toggle("Google Maps preview", showMap) { showMap = it }
            else Text("Map preview: add MAPS_API_KEY in local.properties.\nThis canvas playground works completely offline.", color = Color(0xFF93A9B4), fontSize = 12.sp)
            if (motion == TrailMotionPreset.DashFlow || motion == TrailMotionPreset.RevealThenFlow) Text("Flow is visible on Dashed, Dotted, and Chevrons.", color = Mint, fontSize = 12.sp)
            Surface(shape = RoundedCornerShape(16.dp), color = Surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MAKE IT YOURS", color = Mint, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(if (plugin) "trailEffect {\n    metro()\n}" else "TrailCanvas(path, effect =\n    TrailMotionPreset.${motion.name}.effect(\n        TrailStylePreset.${style.name}.style()\n    )\n)", fontSize = 12.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Text("Built for your next route.  /  alpha 01", color = Color(0xFF698591), fontSize = 11.sp)
        }
        }
    }
}

@Composable private fun Toggle(title: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontSize = 14.sp); Switch(value, change, modifier = Modifier.testTag(title))
    }
}

@Composable private fun Choice(label: String, value: String, options: List<String>, modifier: Modifier, select: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = Color(0xFF93A9B4), fontSize = 10.sp, letterSpacing = 1.sp)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag(label)) { Text(value, fontSize = 12.sp) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, title -> DropdownMenuItem(text = { Text(title) }, onClick = { select(index); expanded = false }) }
            }
        }
    }
}
