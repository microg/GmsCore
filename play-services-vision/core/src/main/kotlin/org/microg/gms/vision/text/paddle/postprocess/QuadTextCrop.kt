/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import org.microg.gms.vision.text.paddle.model.OCRBox
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max

object QuadTextCrop {
    private const val VERTICAL_CROP_RATIO = 1.5

    fun crop(src: Mat, box: OCRBox): Mat {
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
                        rotated
                    } catch (t: Throwable) {
                        rotated.release()
                        throw t
                    }
                } else {
                    keepDestination = true
                    dst
                }
            } finally {
                if (!keepDestination) dst.release()
            }
        } finally {
            m.release()
        }
    }
}
