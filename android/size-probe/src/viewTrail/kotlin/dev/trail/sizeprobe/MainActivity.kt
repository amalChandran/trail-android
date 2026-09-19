package dev.trail.sizeprobe
import android.app.Activity
import android.os.Bundle
import dev.trail.android.TrailView
import dev.trail.core.*
import kotlin.time.Duration.Companion.seconds
class MainActivity: Activity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContentView(TrailView(this).apply {
 path=TrailPath(listOf(TrailPoint(20.0,20.0),TrailPoint(200.0,200.0)))
 effect=trailEffect { stroke(TrailColor.Blue,6.0); reveal(2.seconds) }
}) } }
