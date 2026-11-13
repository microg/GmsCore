/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import android.graphics.PointF
import android.graphics.RectF
import org.microg.gms.vision.text.paddle.model.OCRBox
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

internal object QuadTextCrop {
    private const val VERTICAL_CROP_RATIO = 1.5

    class Crop(
        val image: Mat,
        private val sourceToCrop: Mat,
        private val rotated90: Boolean,
        private val sourceWidth: Int,
        private val sourceHeight: Int,
    ) {
        fun toSourceBoxes(bounds: List<RectF>, rotated180: Boolean): List<OCRBox>? {
            val matrix = DoubleArray(9)
            val inverse = Mat()
            try {
                if (Core.invert(sourceToCrop, inverse) == 0.0 || inverse.get(0, 0, matrix) <= 0) return null
            } finally {
                inverse.release()
            }
            val width = image.cols() - 1.0
            val height = image.rows() - 1.0
            fun map(x: Float, y: Float): PointF? {
                // Bounds are pixel edges. OpenCV rotates and transforms pixel centers.
                var u = (x - 0.5).coerceIn(0.0, width)
                var v = (y - 0.5).coerceIn(0.0, height)
                if (rotated180) {
                    u = width - u
                    v = height - v
                }
                if (rotated90) {
                    val unrotatedX = height - v
                    v = u
                    u = unrotatedX
                }
                val w = matrix[6] * u + matrix[7] * v + matrix[8]
                if (!w.isFinite() || abs(w) < 1e-9) return null
                val sourceX = (matrix[0] * u + matrix[1] * v + matrix[2]) / w
                val sourceY = (matrix[3] * u + matrix[4] * v + matrix[5]) / w
                if (sourceX !in 0.0..sourceWidth.toDouble() || sourceY !in 0.0..sourceHeight.toDouble()) return null
                return PointF(sourceX.toFloat(), sourceY.toFloat())
            }
            return bounds.map { rect ->
                val topLeft = map(rect.left, rect.top) ?: return null
                val topRight = map(rect.right, rect.top) ?: return null
                val bottomRight = map(rect.right, rect.bottom) ?: return null
                val bottomLeft = map(rect.left, rect.bottom) ?: return null
                if (hypot(topRight.x - topLeft.x, topRight.y - topLeft.y) < 1f ||
                    hypot(bottomLeft.x - topLeft.x, bottomLeft.y - topLeft.y) < 1f
                ) return null
                OCRBox(listOf(topLeft, topRight, bottomRight, bottomLeft))
            }
        }

        fun release() {
            try {
                image.release()
            } finally {
                sourceToCrop.release()
            }
        }
    }

    fun crop(src: Mat, box: OCRBox): Crop {
        // Use the minimum-area rectangle to correct perspective before cropping.
        val rectPoints = box.points.map { Point(it.x.toDouble(), it.y.toDouble()) }
        val rectInput = MatOfPoint2f()
        val boundingBox = try {
            rectInput.fromList(rectPoints)
            Imgproc.minAreaRect(rectInput)
        } finally {
            rectInput.release()
        }

        val boxPoints = Array(4) { Point() }
        boundingBox.points(boxPoints)
        val ordered = QuadGeometry.orderMinAreaRectPoints(boxPoints)

        val widthTop = hypot(ordered[0].x - ordered[1].x, ordered[0].y - ordered[1].y)
        val widthBottom = hypot(ordered[2].x - ordered[3].x, ordered[2].y - ordered[3].y)
        val heightLeft = hypot(ordered[0].x - ordered[3].x, ordered[0].y - ordered[3].y)
        val heightRight = hypot(ordered[1].x - ordered[2].x, ordered[1].y - ordered[2].y)

        val dstW = max(widthTop, widthBottom).toInt().coerceAtLeast(1)
        val dstH = max(heightLeft, heightRight).toInt().coerceAtLeast(1)

        val srcPts = MatOfPoint2f()
        val m = try {
            srcPts.fromList(ordered)
            val dstPts = MatOfPoint2f()
            try {
                dstPts.fromList(
                    listOf(
                        Point(0.0, 0.0),
                        Point(dstW.toDouble(), 0.0),
                        Point(dstW.toDouble(), dstH.toDouble()),
                        Point(0.0, dstH.toDouble()),
                    )
                )
                Imgproc.getPerspectiveTransform(srcPts, dstPts)
            } finally {
                dstPts.release()
            }
        } finally {
            srcPts.release()
        }

        return try {
            val dst = Mat(dstH, dstW, CvType.CV_8UC3)
            var keepDestination = false
            try {
                Imgproc.warpPerspective(
                    src,
                    dst,
                    m,
                    Size(dstW.toDouble(), dstH.toDouble()),
                    Imgproc.INTER_CUBIC,
                    Core.BORDER_REPLICATE,
                )
                if (dst.rows().toDouble() / dst.cols() >= VERTICAL_CROP_RATIO) {
                    val rotated = Mat()
                    try {
                        Core.rotate(dst, rotated, Core.ROTATE_90_COUNTERCLOCKWISE)
                        Crop(rotated, m, true, src.cols(), src.rows())
                    } catch (t: Throwable) {
                        rotated.release()
                        throw t
                    }
                } else {
                    Crop(dst, m, false, src.cols(), src.rows()).also { keepDestination = true }
                }
            } finally {
                if (!keepDestination) dst.release()
            }
        } catch (t: Throwable) {
            m.release()
            throw t
        }
    }
}
