package dev.trail.sizeprobe
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.LatLng
class MainActivity: ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent {
 val camera=rememberCameraPositionState()
 val points=remember { listOf(LatLng(40.0,-73.0),LatLng(40.01,-72.99)) }
 GoogleMap(cameraPositionState=camera) { Polyline(points=points) }
} } }
