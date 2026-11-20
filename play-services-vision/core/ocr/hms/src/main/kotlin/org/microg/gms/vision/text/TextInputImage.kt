/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.media.Image
import java.nio.ByteBuffer
import kotlin.math.roundToInt

object TextInputImage {
    private const val MAX_PIXELS = 16_000_000L
    private const val MAX_SIDE_LENGTH = 1600

    data class PreparedImage(val bitmap: Bitmap, val scaleX: Float, val scaleY: Float, val offsetX: Int, val offsetY: Int)

    /** The caller recycles the returned bitmap only when it differs from [source]. */
    fun prepare(source: Any, format: Int, width: Int, height: Int, rotation: Int, region: Rect? = null): PreparedImage? {
        require(width > 0 && height > 0 && width.toLong() * height <= MAX_PIXELS) { "Invalid or oversized OCR image" }
        require(rotation in 0..3) { "Invalid OCR rotation: $rotation" }
        val swapAxes = rotation % 2 != 0
        val uprightWidth = if (swapAxes) height else width
        val uprightHeight = if (swapAxes) width else height
        val uprightRegion = if (region == null || region.isEmpty) {
            Rect(0, 0, uprightWidth, uprightHeight)
        } else {
            Rect(
                region.left.coerceIn(0, uprightWidth),
                region.top.coerceIn(0, uprightHeight),
                region.right.coerceIn(0, uprightWidth),
                region.bottom.coerceIn(0, uprightHeight),
            )
        }
        // ROI coordinates describe the upright image; read only its corresponding source pixels.
        val sourceRegion = when (rotation) {
            1 -> Rect(uprightRegion.top, height - uprightRegion.right, uprightRegion.bottom, height - uprightRegion.left)
            2 -> Rect(width - uprightRegion.right, height - uprightRegion.bottom, width - uprightRegion.left, height - uprightRegion.top)
            3 -> Rect(width - uprightRegion.bottom, uprightRegion.left, width - uprightRegion.top, uprightRegion.right)
            else -> uprightRegion
        }
        val ratio = minOf(1.0, MAX_SIDE_LENGTH.toDouble() / maxOf(sourceRegion.width(), sourceRegion.height()).coerceAtLeast(1))
        val scaledWidth = (sourceRegion.width() * ratio).roundToInt().coerceAtLeast(1)
        val scaledHeight = (sourceRegion.height() * ratio).roundToInt().coerceAtLeast(1)
        val outputWidth = if (swapAxes) scaledHeight else scaledWidth
        val outputHeight = if (swapAxes) scaledWidth else scaledHeight
        val preparedBitmap = when (format) {
            -1 -> {
                val bitmap = source as? Bitmap ?: throw IllegalArgumentException("Expected Bitmap")
                require(bitmap.width == width && bitmap.height == height) { "Bitmap dimensions do not match metadata" }
                if (uprightRegion.isEmpty) return null
                val canReuse = rotation == 0 && outputWidth == width && outputHeight == height &&
                        bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.hasAlpha() &&
                        (android.os.Build.VERSION.SDK_INT < 26 || bitmap.colorSpace?.isSrgb == true)
                if (canReuse) {
                    bitmap
                } else {
                    val hardware = android.os.Build.VERSION.SDK_INT >= 26 && bitmap.config == Bitmap.Config.HARDWARE
                    if (android.os.Build.VERSION.SDK_INT >= 28 && hardware) {
                        val picture = Picture()
                        val canvas = picture.beginRecording(outputWidth, outputHeight)
                        try {
                            drawBitmap(canvas, bitmap, sourceRegion, scaledWidth, scaledHeight, rotation)
                        } finally {
                            picture.endRecording()
                        }
                        Bitmap.createBitmap(picture, outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                    } else {
                        val drawable = if (hardware) {
                            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: throw IllegalArgumentException("Unable to copy hardware Bitmap")
                        } else {
                            bitmap
                        }
                        try {
                            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
                            try {
                                drawBitmap(Canvas(output), drawable, sourceRegion, scaledWidth, scaledHeight, rotation)
                                output
                            } catch (t: Throwable) {
                                output.recycle()
                                throw t
                            }
                        } finally {
                            if (drawable !== bitmap) drawable.recycle()
                        }
                    }
                }
            }

            ImageFormat.NV21, ImageFormat.YV12 -> {
                val sourceBuffer = source as? ByteBuffer ?: throw IllegalArgumentException("Expected YUV ByteBuffer")
                val data = sourceBuffer.duplicate()
                data.rewind()

                require(width % 2 == 0 && height % 2 == 0) { "Packed YUV requires even dimensions" }
                val pixels = width * height
                val packedYv12 = format == ImageFormat.YV12 && data.limit() == pixels + pixels / 2
                // Camera preview YV12 aligns both luma and chroma rows to 16 bytes.
                val yStride = if (format == ImageFormat.YV12 && !packedYv12) (width + 15) / 16 * 16 else width
                val chromaStride = when {
                    format == ImageFormat.NV21 -> width
                    packedYv12 -> width / 2
                    else -> (yStride / 2 + 15) / 16 * 16
                }
                val yPlaneSize = yStride * height
                val chromaPlaneSize = chromaStride * (height / 2)
                val uPlaneOffset = yPlaneSize + chromaPlaneSize
                val requiredByteCount = yPlaneSize + chromaPlaneSize * (if (format == ImageFormat.YV12) 2 else 1)
                require(data.remaining() >= requiredByteCount) { "YUV buffer is truncated" }
                if (uprightRegion.isEmpty) return null
                yuvToBitmap(sourceRegion, scaledWidth, scaledHeight, rotation) { x, y ->
                    val luminance = data.get(y * yStride + x).toInt() and 255
                    val u: Int
                    val v: Int
                    if (format == ImageFormat.NV21) {
                        // NV21 stores interleaved V and U bytes after the Y plane.
                        val chromaOffset = yPlaneSize + (y / 2) * chromaStride + (x / 2) * 2
                        v = data.get(chromaOffset).toInt() and 255
                        u = data.get(chromaOffset + 1).toInt() and 255
                    } else {
                        // YV12 stores separate Y, V and U planes, in that order.
                        val chromaOffset = (y / 2) * chromaStride + x / 2
                        v = data.get(yPlaneSize + chromaOffset).toInt() and 255
                        u = data.get(uPlaneOffset + chromaOffset).toInt() and 255
                    }
                    (luminance shl 16) or (u shl 8) or v
                }
            }

            ImageFormat.YUV_420_888 -> {
                val image = source as? Image ?: throw IllegalArgumentException("Expected media.Image")
                require(image.width == width && image.height == height && image.planes.size == 3) { "Invalid YUV image" }
                if (uprightRegion.isEmpty) return null
                val planes = image.planes
                val buffers = planes.map { it.buffer.duplicate() }
                val positions = IntArray(3) { buffers[it].position() }
                val rowStrides = IntArray(3) { planes[it].rowStride }
                val pixelStrides = IntArray(3) { planes[it].pixelStride }
                yuvToBitmap(sourceRegion, scaledWidth, scaledHeight, rotation) { x, y ->
                    var packed = 0
                    for (index in 0..2) {
                        val planeX: Int
                        val planeY: Int
                        if (index == 0) {
                            planeX = x
                            planeY = y
                        } else {
                            planeX = x / 2
                            planeY = y / 2
                        }
                        val rowOffset = planeY * rowStrides[index]
                        val pixelOffset = planeX * pixelStrides[index]
                        val offset = positions[index] + rowOffset + pixelOffset
                        packed = (packed shl 8) or (buffers[index].get(offset).toInt() and 255)
                    }
                    packed
                }
            }

            else -> throw IllegalArgumentException("Unsupported OCR image format: ${format}")
        }
        return PreparedImage(
            preparedBitmap,
            uprightRegion.width().toFloat() / outputWidth,
            uprightRegion.height().toFloat() / outputHeight,
            uprightRegion.left,
            uprightRegion.top,
        )
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, region: Rect, width: Int, height: Int, rotation: Int) {
        // Match the opaque pixels produced by dropping premultiplied alpha.
        canvas.drawColor(Color.BLACK)
        when (rotation) {
            1 -> canvas.translate(height.toFloat(), 0f)
            2 -> canvas.translate(width.toFloat(), height.toFloat())
            3 -> canvas.translate(0f, width.toFloat())
        }
        canvas.rotate(rotation * 90f)
        canvas.drawBitmap(bitmap, region, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private inline fun yuvToBitmap(
        region: Rect,
        scaledWidth: Int,
        scaledHeight: Int,
        rotation: Int,
        sample: (Int, Int) -> Int,
    ): Bitmap {
        val swapAxes = rotation % 2 != 0
        val outputWidth = if (swapAxes) scaledHeight else scaledWidth
        val outputHeight = if (swapAxes) scaledWidth else scaledHeight
        val rowsPerBand = (64 * 1024 / (outputWidth * Int.SIZE_BYTES)).coerceIn(1, outputHeight)
        val pixels = IntArray(outputWidth * rowsPerBand)
        val sourceX = IntArray(scaledWidth) { x ->
            region.left + ((x + 0.5) * region.width() / scaledWidth).toInt().coerceAtMost(region.width() - 1)
        }
        val sourceY = IntArray(scaledHeight) { y ->
            region.top + ((y + 0.5) * region.height() / scaledHeight).toInt().coerceAtMost(region.height() - 1)
        }
        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        return try {
            for (bandStart in 0 until outputHeight step rowsPerBand) {
                val rows = minOf(rowsPerBand, outputHeight - bandStart)
                for (row in 0 until rows) {
                    val y = bandStart + row
                    for (x in 0 until outputWidth) {
                        val values = when (rotation) {
                            1 -> sample(sourceX[y], sourceY[scaledHeight - 1 - x])
                            2 -> sample(sourceX[scaledWidth - 1 - x], sourceY[scaledHeight - 1 - y])
                            3 -> sample(sourceX[scaledWidth - 1 - y], sourceY[x])
                            else -> sample(sourceX[x], sourceY[y])
                        }
                        val luminance = ((values ushr 16) - 16).coerceAtLeast(0)
                        val u = ((values ushr 8) and 255) - 128
                        val v = (values and 255) - 128
                        val red = ((298 * luminance + 409 * v + 128) shr 8).coerceIn(0, 255)
                        val green = ((298 * luminance - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255)
                        val blue = ((298 * luminance + 516 * u + 128) shr 8).coerceIn(0, 255)
                        pixels[row * outputWidth + x] = Color.rgb(red, green, blue)
                    }
                }
                bitmap.setPixels(pixels, 0, outputWidth, 0, bandStart, outputWidth, rows)
            }
            bitmap
        } catch (t: Throwable) {
            bitmap.recycle()
            throw t
        }
    }
}
