package dev.trail.sizeprobe
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.graphics.Canvas
import android.graphics.Paint
class MainActivity: Activity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContentView(object: View(this) {
 val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=0xff2563eb.toInt(); strokeWidth=6f }
 override fun onDraw(canvas: Canvas) { canvas.drawLine(20f,20f,200f,200f,paint) }
}) } }
