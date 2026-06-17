package com.hidden.camera.reflection.finder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private var spots: List<BrightSpotDetector.Spot> = emptyList()
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var rotationDegrees = 0

    fun updateDetections(
        newSpots: List<BrightSpotDetector.Spot>,
        imageWidth: Int,
        imageHeight: Int,
        imageRotationDegrees: Int
    ) {
        spots = newSpots
        sourceWidth = imageWidth
        sourceHeight = imageHeight
        rotationDegrees = imageRotationDegrees
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.18f
        canvas.drawCircle(cx, cy, radius, crosshairPaint)
        canvas.drawLine(cx - radius * 0.55f, cy, cx + radius * 0.55f, cy, crosshairPaint)
        canvas.drawLine(cx, cy - radius * 0.55f, cx, cy + radius * 0.55f, crosshairPaint)

        if (sourceWidth <= 0 || sourceHeight <= 0) return
        spots.forEach { spot ->
            val point = mapImagePoint(spot.centerX, spot.centerY)
            canvas.drawCircle(point.x, point.y, spot.radius * currentScale(), spotPaint)
        }
    }

    private fun mapImagePoint(x: Float, y: Float): PointF {
        val rotatedWidth: Float
        val rotatedHeight: Float
        val rx: Float
        val ry: Float
        when ((rotationDegrees % 360 + 360) % 360) {
            90 -> {
                rotatedWidth = sourceHeight.toFloat()
                rotatedHeight = sourceWidth.toFloat()
                rx = sourceHeight - y
                ry = x
            }
            180 -> {
                rotatedWidth = sourceWidth.toFloat()
                rotatedHeight = sourceHeight.toFloat()
                rx = sourceWidth - x
                ry = sourceHeight - y
            }
            270 -> {
                rotatedWidth = sourceHeight.toFloat()
                rotatedHeight = sourceWidth.toFloat()
                rx = y
                ry = sourceWidth - x
            }
            else -> {
                rotatedWidth = sourceWidth.toFloat()
                rotatedHeight = sourceHeight.toFloat()
                rx = x
                ry = y
            }
        }
        val scale = max(width / rotatedWidth, height / rotatedHeight)
        val offsetX = (width - rotatedWidth * scale) / 2f
        val offsetY = (height - rotatedHeight * scale) / 2f
        return PointF(offsetX + rx * scale, offsetY + ry * scale)
    }

    private fun currentScale(): Float {
        val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) sourceHeight else sourceWidth
        val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) sourceWidth else sourceHeight
        if (rotatedWidth <= 0 || rotatedHeight <= 0) return 1f
        return max(width / rotatedWidth.toFloat(), height / rotatedHeight.toFloat())
    }
}
