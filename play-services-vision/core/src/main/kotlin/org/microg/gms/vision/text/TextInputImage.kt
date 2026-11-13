/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.media.Image
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.math.roundToInt

internal object TextInputImage {
    private const val MAX_PIXELS = 16_000_000L
    private const val MAX_SIDE_LENGTH = 1600

    data class PreparedImage(val mat: Mat, val scaleX: Float, val scaleY: Float, val offsetX: Int, val offsetY: Int)

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
        val mat = when (format) {
            -1 -> {
                val bitmap = source as? Bitmap ?: throw IllegalArgumentException("Expected Bitmap")
                require(bitmap.width == width && bitmap.height == height) { "Bitmap dimensions do not match metadata" }
                if (uprightRegion.isEmpty) return null
                val hardware = android.os.Build.VERSION.SDK_INT >= 26 && bitmap.config == Bitmap.Config.HARDWARE
                val canReadDirectly =
                    rotation == 0 && scaledWidth == sourceRegion.width() && scaledHeight == sourceRegion.height() && bitmap.config == Bitmap.Config.ARGB_8888 && !bitmap.hasAlpha() && (android.os.Build.VERSION.SDK_INT < 26 || bitmap.colorSpace?.isSrgb == true)
                if (canReadDirectly) {
                    if (sourceRegion.width() == width && sourceRegion.height() == height) {
                        bitmapToBgrMat(bitmap)
                    } else {
                        bitmapRegionToBgrMat(bitmap, sourceRegion)
                    }
                } else {
                    val ownedBitmap = if (android.os.Build.VERSION.SDK_INT >= 28 && hardware) {
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
                    try {
                        bitmapToBgrMat(ownedBitmap)
                    } finally {
                        ownedBitmap.recycle()
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
                yuvToBgrMat(sourceRegion, scaledWidth, scaledHeight, rotation) { x, y ->
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
                yuvToBgrMat(sourceRegion, scaledWidth, scaledHeight, rotation) { x, y ->
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
            mat,
            uprightRegion.width().toFloat() / outputWidth,
            uprightRegion.height().toFloat() / outputHeight,
            uprightRegion.left,
            uprightRegion.top,
        )
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, region: Rect, width: Int, height: Int, rotation: Int) {
        when (rotation) {
            1 -> canvas.translate(height.toFloat(), 0f)
            2 -> canvas.translate(width.toFloat(), height.toFloat())
            3 -> canvas.translate(0f, width.toFloat())
        }
        canvas.rotate(rotation * 90f)
        canvas.drawBitmap(bitmap, region, Rect(0, 0, width, height), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun bitmapRegionToBgrMat(bitmap: Bitmap, region: Rect): Mat {
        val width = region.width()
        val height = region.height()
        // Copy only a band of pixels at a time so the ROI does not need a full-size Java buffer.
        val rowsPerBand = (64 * 1024 / (width * (Int.SIZE_BYTES + 3))).coerceIn(1, height)
        val colors = IntArray(width * rowsPerBand)
        val pixels = ByteArray(colors.size * 3)
        val bgr = Mat()
        return try {
            bgr.create(height, width, CvType.CV_8UC3)
            for (row in 0 until height step rowsPerBand) {
                val rows = minOf(rowsPerBand, height - row)
                bitmap.getPixels(colors, 0, width, region.left, region.top + row, width, rows)
                val count = width * rows
                for (index in 0 until count) {
                    val color = colors[index]
                    val offset = index * 3
                    pixels[offset] = color.toByte()
                    pixels[offset + 1] = (color ushr 8).toByte()
                    pixels[offset + 2] = (color ushr 16).toByte()
                }
                check(bgr.put(row, 0, pixels, 0, count * 3) == count * 3) { "OCR input pixels are truncated" }
            }
            bgr
        } catch (t: Throwable) {
            bgr.release()
            throw t
        }
    }

    private fun bitmapToBgrMat(bitmap: Bitmap): Mat {
        val bgr = Mat()
        return try {
            val rgba = Mat()
            try {
                Utils.bitmapToMat(bitmap, rgba)
                Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
                bgr
            } finally {
                rgba.release()
            }
        } catch (t: Throwable) {
            bgr.release()
            throw t
        }
    }

    private inline fun yuvToBgrMat(
        region: Rect,
        scaledWidth: Int,
        scaledHeight: Int,
        rotation: Int,
        sample: (Int, Int) -> Int,
    ): Mat {
        val swapAxes = rotation % 2 != 0
        val outputWidth = if (swapAxes) scaledHeight else scaledWidth
        val outputHeight = if (swapAxes) scaledWidth else scaledHeight
        // Sample directly into the bounded, upright image without copying the full YUV planes.
        val pixels = ByteArray(outputWidth * outputHeight * 3)
        val sourceX = IntArray(scaledWidth) { x ->
            region.left + ((x + 0.5) * region.width() / scaledWidth).toInt().coerceAtMost(region.width() - 1)
        }
        for (y in 0 until scaledHeight) {
            val sourceY = region.top + ((y + 0.5) * region.height() / scaledHeight).toInt().coerceAtMost(region.height() - 1)
            for (x in 0 until scaledWidth) {
                val values = sample(sourceX[x], sourceY)
                val luminance = ((values ushr 16) - 16).coerceAtLeast(0)
                val u = ((values ushr 8) and 255) - 128
                val v = (values and 255) - 128
                val red = ((298 * luminance + 409 * v + 128) shr 8).coerceIn(0, 255)
                val green = ((298 * luminance - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255)
                val blue = ((298 * luminance + 516 * u + 128) shr 8).coerceIn(0, 255)
                val offset = when (rotation) {
                    1 -> x * outputWidth + scaledHeight - 1 - y
                    2 -> (scaledHeight - 1 - y) * outputWidth + scaledWidth - 1 - x
                    3 -> (scaledWidth - 1 - x) * outputWidth + y
                    else -> y * outputWidth + x
                }
                val byteOffset = offset * 3
                pixels[byteOffset] = blue.toByte()
                pixels[byteOffset + 1] = green.toByte()
                pixels[byteOffset + 2] = red.toByte()
            }
        }
        val mat = Mat()
        return try {
            mat.create(outputHeight, outputWidth, CvType.CV_8UC3)
            check(mat.put(0, 0, pixels) == pixels.size) { "OCR input pixels are truncated" }
            mat
        } catch (t: Throwable) {
            mat.release()
            throw t
        }
    }
}
