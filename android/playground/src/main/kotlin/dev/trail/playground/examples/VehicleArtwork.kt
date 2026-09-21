package dev.trail.playground.examples

import android.content.Context
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import dev.trail.compose.TrailPlayback
import dev.trail.core.*
import dev.trail.effects.TrailMotionPreset
import org.json.JSONObject
import kotlin.math.PI
import kotlin.time.Duration.Companion.seconds

internal class VehicleArtwork(
    val id: String, val height: Double, val anchor: TrailPoint,
    val size: Double, val headingWindow: Double, val duration: Double,
    private val layers: List<VehicleLayer>,
) {
    fun bitmap(density: Double): Bitmap {
        val extent = (size * 1.8 * density).toInt().coerceAtLeast(1)
        return Bitmap.createBitmap(extent, extent, Bitmap.Config.ARGB_8888).also { bitmap ->
            draw(NativeCanvas(bitmap), TrailPose(TrailPoint(extent / density / 2, extent / density / 2), -PI/2, 0), density)
        }
    }
    /** Cached paths/paints; draw with a transform, without rebuilding artwork each frame. */
    fun draw(canvas: NativeCanvas, pose: TrailPose, density: Double = 1.0) {
        val saved=canvas.save()
        canvas.translate((pose.point.x*density).toFloat(),(pose.point.y*density).toFloat())
        canvas.rotate(Math.toDegrees(pose.headingRadians+PI/2).toFloat())
        val scale=(density*size/height).toFloat(); canvas.scale(scale,scale)
        canvas.translate(-anchor.x.toFloat(),-anchor.y.toFloat())
        for(layer in layers) {
            canvas.drawPath(layer.path,layer.fill)
            layer.stroke?.let { canvas.drawPath(layer.path,it) }
        }
        canvas.restoreToCount(saved)
    }
    fun animation(motion: TrailMotionPreset): TrailAnimationSpec {
        val base=motion.animation(duration.seconds,repeat=true)
        return TrailAnimations.custom(TrailAnimation { time ->
            base.sampler.sample(TrailTime(journeyProgress(time.progress),time.cycle,time.elapsedSeconds))
        },duration.seconds,repeat=true,reducedMotion=base.reducedMotion)
    }
}

/** Gentle departure/arrival; position and line head receive exactly the same eased clock. */
internal fun journeyProgress(p: Double): Double = (p*p*p*(10+p*(-15+6*p))).coerceIn(0.0,1.0)

internal data class VehicleLayer(val path: Path, val fill: Paint, val stroke: Paint?)

@Composable internal fun rememberVehicleArtwork(id: String): VehicleArtwork {
    val context=LocalContext.current
    return remember(context) { loadVehicleArtwork(context) }.getValue(id)
}

internal fun loadVehicleArtwork(context: Context): Map<String,VehicleArtwork> {
    val rows=context.assets.open("vehicles.json").bufferedReader().use { JSONObject(it.readText()) }.getJSONArray("vehicles")
    return (0 until rows.length()).associate { index ->
        val row=rows.getJSONObject(index); val layers=row.getJSONArray("layers")
        val prepared=(0 until layers.length()).map { i ->
            val layer=layers.getJSONObject(i); val commands=layer.getJSONArray("path")
            val path=Path()
            for(j in 0 until commands.length()) {
                val command=commands.getJSONArray(j)
                fun p(n: Int)=command.getDouble(n).toFloat()
                when(command.getInt(0)) {
                    0 -> path.moveTo(p(1),p(2)); 1 -> path.lineTo(p(1),p(2))
                    2 -> path.cubicTo(p(1),p(2),p(3),p(4),p(5),p(6))
                    3 -> path.quadTo(p(1),p(2),p(3),p(4)); 4 -> path.close()
                    else -> error("Unknown vehicle path command")
                }
            }
            fun paint(color: String, stroke: Boolean)=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color=android.graphics.Color.parseColor(color)
                style=if(stroke) Paint.Style.STROKE else Paint.Style.FILL
                strokeWidth=layer.optDouble("width",1.0).toFloat(); strokeJoin=Paint.Join.ROUND
            }
            VehicleLayer(path,paint(layer.getString("fill"),false),
                if(layer.has("stroke")) paint(layer.getString("stroke"),true) else null)
        }
        val anchor=row.getJSONArray("anchor")
        row.getString("id") to VehicleArtwork(row.getString("id"),row.getDouble("height"),
            TrailPoint(anchor.getDouble(0),anchor.getDouble(1)),row.getDouble("size"),
            row.getDouble("headingWindow"),row.getDouble("duration"),prepared)
    }
}

@Composable internal fun JourneyVehicle(
    path: TrailPath, artwork: VehicleArtwork, playback: TrailPlayback, layer: Int, reduced: Boolean,
) {
    Canvas(Modifier.fillMaxSize().testTag("journeyVehicle")) {
        // Observe time while drawing: vehicle movement does not recompose the map.
        val frame=playback.frame(layer)
        val fraction=if(reduced) 1.0 else frame.head ?: journeyProgress(playback.progress)
        val pose=path.poseAt(fraction,artwork.headingWindow,if(reduced) TrailDirection.Forward else frame.headDirection)
        if(pose!=null) drawIntoCanvas { artwork.draw(it.nativeCanvas,pose,density.toDouble()) }
    }
}
