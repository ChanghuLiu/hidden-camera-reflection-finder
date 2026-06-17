package com.hidden.camera.reflection.finder

import androidx.camera.core.ImageProxy
import kotlin.math.max
import kotlin.math.min

class BrightSpotDetector {
    data class Spot(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val averageBrightness: Float
    )

    fun detect(image: ImageProxy, sensitivity: Float): List<Spot> {
        val plane = image.planes.firstOrNull() ?: return emptyList()
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val step = 4
        val gridWidth = (width + step - 1) / step
        val gridHeight = (height + step - 1) / step
        val threshold = (255 - (sensitivity.coerceIn(0f, 1f) * 95f)).toInt().coerceIn(140, 252)
        val bright = BooleanArray(gridWidth * gridHeight)
        val visited = BooleanArray(gridWidth * gridHeight)

        for (gy in 0 until gridHeight) {
            val y = min(gy * step, height - 1)
            for (gx in 0 until gridWidth) {
                val x = min(gx * step, width - 1)
                val value = buffer.get(y * rowStride + x * pixelStride).toInt() and 0xFF
                if (value >= threshold) {
                    bright[gy * gridWidth + gx] = true
                }
            }
        }

        val spots = mutableListOf<Spot>()
        val queueX = IntArray(gridWidth * gridHeight)
        val queueY = IntArray(gridWidth * gridHeight)
        val maxBlobCells = max(18, (gridWidth * gridHeight * 0.004f).toInt())

        for (gy in 0 until gridHeight) {
            for (gx in 0 until gridWidth) {
                val startIndex = gy * gridWidth + gx
                if (!bright[startIndex] || visited[startIndex]) continue

                var head = 0
                var tail = 0
                queueX[tail] = gx
                queueY[tail] = gy
                tail++
                visited[startIndex] = true

                var count = 0
                var sumX = 0f
                var sumY = 0f
                var sumBrightness = 0f
                var minX = gx
                var maxX = gx
                var minY = gy
                var maxY = gy

                while (head < tail) {
                    val cx = queueX[head]
                    val cy = queueY[head]
                    head++

                    val sampleX = min(cx * step, width - 1)
                    val sampleY = min(cy * step, height - 1)
                    val value = buffer.get(sampleY * rowStride + sampleX * pixelStride).toInt() and 0xFF
                    count++
                    sumX += sampleX
                    sumY += sampleY
                    sumBrightness += value
                    minX = min(minX, cx)
                    maxX = max(maxX, cx)
                    minY = min(minY, cy)
                    maxY = max(maxY, cy)

                    for (ny in cy - 1..cy + 1) {
                        for (nx in cx - 1..cx + 1) {
                            if (nx !in 0 until gridWidth || ny !in 0 until gridHeight) continue
                            val nextIndex = ny * gridWidth + nx
                            if (bright[nextIndex] && !visited[nextIndex]) {
                                visited[nextIndex] = true
                                queueX[tail] = nx
                                queueY[tail] = ny
                                tail++
                            }
                        }
                    }
                }

                val blobWidth = (maxX - minX + 1) * step
                val blobHeight = (maxY - minY + 1) * step
                val compactEnough = blobWidth <= width * 0.12f && blobHeight <= height * 0.12f
                if (count in 1..maxBlobCells && compactEnough) {
                    spots += Spot(
                        centerX = sumX / count,
                        centerY = sumY / count,
                        radius = max(18f, max(blobWidth, blobHeight) * 0.8f),
                        averageBrightness = sumBrightness / count
                    )
                }
            }
        }

        return spots.sortedByDescending { it.averageBrightness }.take(12)
    }
}
