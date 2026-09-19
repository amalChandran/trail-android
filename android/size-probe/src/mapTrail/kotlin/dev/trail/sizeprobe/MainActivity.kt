package dev.trail.sizeprobe
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.LatLng
import dev.trail.core.*
import dev.trail.googlemaps.GoogleMapsTrail
import kotlin.time.Duration.Companion.seconds
class MainActivity: ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent {
 val camera=rememberCameraPositionState()
 val route=remember { TrailRoute.direct("route",TrailCoordinate(40.0,-73.0),TrailCoordinate(40.01,-72.99)) }
 val effect=remember { trailEffect { stroke(TrailColor.Blue,6.0); reveal(2.seconds) } }
 GoogleMap(cameraPositionState=camera) { GoogleMapsTrail(route,camera,effect) }
} } }
