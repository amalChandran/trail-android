package dev.trail.playground.examples

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import dev.trail.compose.*
import dev.trail.core.*
import dev.trail.effects.*
import dev.trail.googlemaps.GoogleMapsTrail
import dev.trail.googlemaps.GoogleMapsTrailVehicle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import org.json.JSONObject

internal data class Journey(
    val id: String, val title: String, val origin: String, val destination: String,
    val symbol: String, val description: String, val provenance: String,
    val coordinates: List<TrailCoordinate>, val defaultGeometry: String,
) {
    val color: TrailColor get() = when (id) { "cab" -> TrailColor(0xFFFFC857.toInt()); "ferry" -> TrailColor.Mint; else -> TrailColor(0xFF8FB8FF.toInt()) }
    fun route(mode: String): TrailRoute {
        val key = "$id/$mode"; val from=coordinates.first(); val to=coordinates.last()
        return when (mode) {
            "arc" -> TrailRoute.arc(key,from,to,bend=.28)
            "direct" -> TrailRoute.direct(key,from,to)
            "greatCircle" -> TrailRoute.greatCircle(key,from,to)
            else -> if (id == "flight") TrailRoute.greatCircle(key,from,to) else TrailRoute(key,coordinates)
        }
    }
}

internal fun loadJourneys(context: Context): List<Journey> {
    val json=context.assets.open("journeys.json").bufferedReader().use { JSONObject(it.readText()) }.getJSONArray("journeys")
    return (0 until json.length()).map { index ->
        val row=json.getJSONObject(index); val points=row.getJSONArray("coordinates")
        Journey(row.getString("id"),row.getString("title"),row.getString("origin"),row.getString("destination"),
            row.getString("symbol"),row.getString("description"),row.getString("provenance"),
            (0 until points.length()).map { points.getJSONArray(it).let { p -> TrailCoordinate(p.getDouble(0),p.getDouble(1)) } },row.getString("defaultGeometry"))
    }
}

private val drawingModes=linkedMapOf("route" to "Full route", "direct" to "Two points", "arc" to "Arc", "greatCircle" to "Great circle")

@Composable fun JourneyExamples(mapsEnabled: Boolean, initialJourney: String? = null, onBack: () -> Unit) {
    val context=LocalContext.current
    val journeys=remember { loadJourneys(context) }
    val fleet=remember(context) { loadVehicleArtwork(context) }
    var selected by remember { mutableIntStateOf(journeys.indexOfFirst { it.id == initialJourney }.coerceAtLeast(0)) }
    var mode by remember { mutableStateOf(journeys[selected].defaultGeometry) }
    var style by remember { mutableStateOf(TrailStylePreset.Cased) }
    var motion by remember { mutableStateOf(TrailMotionPreset.Reveal) }
    var reduced by remember { mutableStateOf(false) }
    var loadingDemo by remember { mutableStateOf(false) }
    var directionsAttempt by remember { mutableIntStateOf(0) }
    val journey=journeys[selected]
    val vehicle=fleet.getValue(journey.id)
    val route=remember(journey,mode) { journey.route(mode) }
    val reducedMotion=reduced || rememberSystemReducedMotion()
    val transition=rememberTrailRouteTransition(route.coordinates.first(),route.coordinates.last(),reducedMotion=reducedMotion)
    LaunchedEffect(loadingDemo,journey.id,mode,directionsAttempt) {
        if (loadingDemo) {
            val request=transition.begin(route.coordinates.first(),route.coordinates.last())
            // Bundled directions simulate a response; no backend credentials or network required.
            try { delay(2400); transition.resolve(request,route) }
            finally { transition.cancel(request) }
        }
    }
    val phase=if(loadingDemo) transition.phase else TrailRoutePhase.Ready
    val displayedRoute=if(loadingDemo) transition.route else route
    val effect=remember(journey,style,motion,phase) { trailEffect {
        layer { stroke(journey.color.withOpacity(.22),6.0) }
        layer {
            style(style.style(journey.color))
            when(phase) {
                TrailRoutePhase.Loading -> animation(TrailMotionPreset.Comet.animation(1.2.seconds,repeat=true))
                TrailRoutePhase.Ready -> animation(vehicle.animation(motion))
                else -> Unit
            }
        }
    } }
    val playback=rememberTrailPlayback(effect,listOf(route.key,phase))
    MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF32D6AD),onPrimary=Color(0xFF0C1921),onBackground=Color(0xFFE2EBEF))) {
        Surface(Modifier.fillMaxSize(),color=Color(0xFF0C1921),contentColor=Color(0xFFE2EBEF)) {
            Column(Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                TextButton(onClick=onBack,modifier=Modifier.testTag("journeysBack")) { Text("← Trail Studio") }
                Text("Routes, in the real world.",fontSize=25.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    journeys.forEachIndexed { i,item ->
                        FilterChip(selected==i,onClick={ selected=i; mode=item.defaultGeometry },label={ Text("${item.symbol} ${item.id.replaceFirstChar { it.uppercase() }}") },modifier=Modifier.testTag("journey-${item.id}"))
                    }
                }
                Text(journey.title,fontSize=18.sp,fontWeight=FontWeight.Medium,modifier=Modifier.testTag("journeyTitle"))
                Text(journey.description,color=Color(0xFF93A9B4),fontSize=12.sp)
                Surface(shape=RoundedCornerShape(20.dp),color=Color(0xFF162C37),modifier=Modifier.fillMaxWidth().height(330.dp).testTag("journeyMap")) {
                    if (mapsEnabled) GoogleJourneyMap(displayedRoute,journey,effect,playback,reducedMotion,vehicle=vehicle,
                        showVehicle=phase==TrailRoutePhase.Ready,fitRoute=route)
                    else Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.Center) {
                        Text("Google Maps is ready to configure",fontSize=18.sp)
                        Text("Add MAPS_API_KEY to android/local.properties and rebuild to see these coordinates on the map.",fontSize=13.sp)
                        Text("${journey.origin} → ${journey.destination}",Modifier.padding(top=16.dp))
                    }
                }
                Text("${drawingModes.getValue(mode)} · ${route.coordinates.size} points · ${"%.1f".format(route.distanceMeters/1000)} km",fontSize=12.sp,modifier=Modifier.testTag("journeyGeometry"))
                if (loadingDemo) Text(when(phase) {
                    TrailRoutePhase.Loading -> "Finding a route…"
                    TrailRoutePhase.Morphing -> "Directions found · settling onto the route"
                    TrailRoutePhase.Ready -> "Route ready"
                    else -> "Route unavailable · try again"
                },fontSize=12.sp,modifier=Modifier.testTag("journeyRoutePhase"))
                JourneyPlayback(playback)
                OutlinedButton({ loadingDemo=true; directionsAttempt++ },Modifier.fillMaxWidth().testTag("journeyLoadRoute")) { Text("Try loading arc → route") }
                JourneyChoice("Drawing",drawingModes.getValue(mode),drawingModes.values.toList(),"journeyDrawing") { mode=drawingModes.keys.elementAt(it) }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    JourneyChoice("Line",style.label,TrailStylePreset.entries.map { it.label },"journeyStyle",Modifier.weight(1f)) { style=TrailStylePreset.entries[it] }
                    JourneyChoice("Motion",motion.label,TrailMotionPreset.entries.map { it.label },"journeyMotion",Modifier.weight(1f)) { motion=TrailMotionPreset.entries[it] }
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Reduced motion",Modifier.weight(1f)); Switch(reduced,{ reduced=it },Modifier.testTag("journeyReduced"))
                }
                Text(when(mode) { "arc" -> "A decorative connection from two endpoints."; "direct" -> "A direct connection; intermediate waypoints are intentionally omitted."; "greatCircle" -> "A sampled shortest path on a spherical Earth."; else -> journey.provenance },color=Color(0xFF93A9B4),fontSize=12.sp)
                if (journey.id=="cab") Text("Route data © OpenStreetMap contributors · ODbL",fontSize=11.sp,color=Color(0xFF93A9B4))
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable internal fun GoogleJourneyMap(
    route: TrailRoute, journey: Journey, effect: TrailEffect, playback: TrailPlayback, reduced: Boolean,
    camera: CameraPositionState = rememberCameraPositionState {
        val center=checkNotNull(route.bounds).center
        position=CameraPosition.fromLatLngZoom(LatLng(center.latitude,center.longitude),3f)
    },
    vehicle: VehicleArtwork = rememberVehicleArtwork(journey.id),
    showVehicle: Boolean = true,
    fitRoute: TrailRoute = route,
) {
    val bounds=checkNotNull(fitRoute.bounds)
    var loaded by remember { mutableStateOf(false) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var visible by remember { mutableStateOf(false) }
    val rootView=LocalView.current
    val density=LocalDensity.current.density
    val padding=with(LocalDensity.current) { 44.dp.roundToPx() }
    Box(Modifier.fillMaxSize().onSizeChanged { viewport=it }.onGloballyPositioned {
        val bounds=it.boundsInWindow()
        visible=bounds.width>0 && bounds.height>0 && bounds.bottom>0 && bounds.right>0 && bounds.top<rootView.height && bounds.left<rootView.width
    }) {
        GoogleMap(Modifier.matchParentSize(),cameraPositionState=camera,onMapLoaded={ loaded=true },
            // Maps Compose defaults to a minimum zoom of 3, which clips a transatlantic fit.
            properties=MapProperties(minZoomPreference=0f),
            uiSettings=MapUiSettings(zoomControlsEnabled=false,mapToolbarEnabled=false)) {
            MapEffect(fitRoute.key,viewport) { nativeMap ->
        // Fit as soon as layout is known. Waiting for tiles first fetched a world map, then
        // fetched a second viewport; it also spent playback time before the route was visible.
        if (viewport.width>0 && viewport.height>0) nativeMap.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds(
            LatLng(bounds.center.latitude-bounds.latitudeSpan/2,bounds.center.longitude-bounds.longitudeSpan/2),
            LatLng(bounds.center.latitude+bounds.latitudeSpan/2,bounds.center.longitude+bounds.longitudeSpan/2)),viewport.width,viewport.height,padding))
    }
            Marker(state=rememberUpdatedMarkerState(LatLng(route.coordinates.first().latitude,route.coordinates.first().longitude)),title=journey.origin)
            Marker(state=rememberUpdatedMarkerState(LatLng(route.coordinates.last().latitude,route.coordinates.last().longitude)),title=journey.destination)
            val icon=remember(vehicle,density) { BitmapDescriptorFactory.fromBitmap(vehicle.bitmap(density.toDouble())) }
            GoogleMapsTrail(route,camera,effect,playback,
                vehicle=if(showVehicle && loaded) GoogleMapsTrailVehicle(icon,journey.id) else null,reducedMotion=reduced,active=loaded && visible)
        }
        Text(if (loaded) "Google Maps · ready" else "Loading Google Maps…",Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xCC0C1921)).padding(5.dp).testTag("journeyMapStatus"),fontSize=10.sp,color=Color.White)
    }
}

@Composable private fun JourneyPlayback(playback: TrailPlayback) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button({ if(playback.isPlaying) playback.pause() else playback.play() },Modifier.weight(1f).testTag("journeyPlayPause")) { Text(if(playback.isPlaying) "Pause" else "Play") }
        OutlinedButton(playback::replay,Modifier.weight(1f).testTag("journeyReplay")) { Text("Replay") }
    }
    Slider(playback.progress.toFloat(),{ playback.seek(it.toDouble()) },modifier=Modifier.testTag("journeyProgress"))
}

@Composable private fun JourneyChoice(label: String, value: String, options: List<String>, tag: String, modifier: Modifier=Modifier, choose: (Int)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label,fontSize=11.sp,color=Color(0xFF93A9B4))
        Box {
            OutlinedButton({ expanded=true },Modifier.fillMaxWidth().testTag(tag)) { Text(value) }
            DropdownMenu(expanded,{ expanded=false }) { options.forEachIndexed { i,option -> DropdownMenuItem(text={ Text(option) },onClick={ choose(i); expanded=false }) } }
        }
    }
}
