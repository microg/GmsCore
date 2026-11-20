/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.postprocess

import android.graphics.PointF
import org.microg.gms.vision.text.paddle.model.OCRBox
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.round

object DBPostProcessor {
    private const val BINARIZATION_THRESHOLD = 0.3f
    private const val BOX_SCORE_THRESHOLD = 0.6f
    private const val UNCLIP_RATIO = 1.5f
    private const val MAX_CANDIDATES = 1000
    private const val MIN_SIZE_BEFORE_UNCLIP = 3f
    private const val MIN_SIZE_AFTER_UNCLIP = 5f

    fun process(
        pred: FloatArray,
        predShape: LongArray,
        originalH: Int,
        originalW: Int,
    ): List<OCRBox> {
        val pH = predShape[2].toInt()
        val pW = predShape[3].toInt()
        val scaleX = originalW.toDouble() / pW
        val scaleY = originalH.toDouble() / pH

        val rawProb = Mat()
        val mask = Mat()
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()

        return try {
            rawProb.create(pH, pW, CvType.CV_32FC1)
            rawProb.put(0, 0, pred)
            Core.compare(rawProb, Scalar(BINARIZATION_THRESHOLD.toDouble()), mask, Core.CMP_GT)

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val boxes = mutableListOf<OCRBox>()
            val numContours = minOf(contours.size, MAX_CANDIDATES)
            for (index in 0 until numContours) {
                val contour = contours[index]
                val contour2f = MatOfPoint2f(*contour.toArray())
                val rect = try {
                    Imgproc.minAreaRect(contour2f)
                } finally {
                    contour2f.release()
                }
                val sside = minOf(rect.size.width, rect.size.height)
                if (sside < MIN_SIZE_BEFORE_UNCLIP) {
                    continue
                }

                val rectPoints = Array(4) { Point() }
                rect.points(rectPoints)
                val orderedPoints = QuadGeometry.orderMinAreaRectPoints(rectPoints)
                val score = computeBoxScore(rawProb, orderedPoints)
                if (score < BOX_SCORE_THRESHOLD) {
                    continue
                }

                val expandedPts = PolygonUnclip.unclip(orderedPoints, UNCLIP_RATIO)
                val expanded2f = MatOfPoint2f()
                val expandedRect = try {
                    expanded2f.fromList(expandedPts)
                    Imgproc.minAreaRect(expanded2f)
                } finally {
                    expanded2f.release()
                }
                val sside2 = minOf(expandedRect.size.width, expandedRect.size.height)
                if (sside2 < MIN_SIZE_AFTER_UNCLIP) {
                    continue
                }

                val expandedBox = Array(4) { Point() }
                expandedRect.points(expandedBox)
                val ePts = QuadGeometry.orderMinAreaRectPoints(expandedBox)
                val scaled = listOf(
                    scalePoint(ePts[0], scaleX, scaleY, originalW, originalH),
                    scalePoint(ePts[1], scaleX, scaleY, originalW, originalH),
                    scalePoint(ePts[2], scaleX, scaleY, originalW, originalH),
                    scalePoint(ePts[3], scaleX, scaleY, originalW, originalH),
                )

                val boxW = kotlin.math.hypot(
                    scaled[1].x - scaled[0].x,
                    scaled[1].y - scaled[0].y,
                )
                val boxH = kotlin.math.hypot(
                    scaled[3].x - scaled[0].x,
                    scaled[3].y - scaled[0].y,
                )
                if (boxW <= 3 || boxH <= 3) {
                    continue
                }

                boxes.add(OCRBox(points = scaled))
            }
            boxes
        } finally {
            hierarchy.release()
            contours.forEach { it.release() }
            mask.release()
            rawProb.release()
        }
    }

    private fun scalePoint(
        point: Point,
        scaleX: Double,
        scaleY: Double,
        originalW: Int,
        originalH: Int,
    ): PointF {
        return PointF(
            round(point.x * scaleX).toInt().coerceIn(0, originalW).toFloat(),
            round(point.y * scaleY).toInt().coerceIn(0, originalH).toFloat(),
        )
    }

    private fun computeBoxScore(probMap: Mat, points: List<Point>): Float {
        val h = probMap.rows()
        val w = probMap.cols()
        val xmin = points.minOf { kotlin.math.floor(it.x).toInt() }.coerceIn(0, w - 1)
        val xmax = points.maxOf { kotlin.math.ceil(it.x).toInt() }.coerceIn(0, w - 1)
        val ymin = points.minOf { kotlin.math.floor(it.y).toInt() }.coerceIn(0, h - 1)
        val ymax = points.maxOf { kotlin.math.ceil(it.y).toInt() }.coerceIn(0, h - 1)

        val mask = Mat(ymax - ymin + 1, xmax - xmin + 1, CvType.CV_8UC1, Scalar(0.0))
        return try {
            val pts = MatOfPoint()
            try {
                pts.fromList(points.map { Point((it.x - xmin).toInt().toDouble(), (it.y - ymin).toInt().toDouble()) })
                Imgproc.fillPoly(mask, mutableListOf(pts), Scalar(1.0))
                val roi = probMap.submat(ymin, ymax + 1, xmin, xmax + 1)
                try {
                    Core.mean(roi, mask).`val`[0].toFloat()
                } finally {
                    roi.release()
                }
            } finally {
                pts.release()
            }
        } finally {
            mask.release()
        }
    }
}
