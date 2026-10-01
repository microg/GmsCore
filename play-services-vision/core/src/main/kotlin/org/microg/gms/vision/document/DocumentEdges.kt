/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

private const val DETECTION_SIZE = 640
private const val LIVE_DETECTION_SIZE = 320
private const val MIN_DOCUMENT_AREA = 0.05
private const val MIN_OUTER_FILL = 0.85

internal val openCvLoaded by lazy { OpenCVLoader.initLocal() }

fun fullPageCorners(width: Int, height: Int) = floatArrayOf(0f, 0f, width.toFloat(), 0f, width.toFloat(), height.toFloat(), 0f, height.toFloat())

fun scaleCorners(file: File, relative: FloatArray): FloatArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    return FloatArray(8) { relative[it] * if (it % 2 == 0) bounds.outWidth else bounds.outHeight }
}

fun detectDocumentCorners(file: File): FloatArray? {
    if (!openCvLoaded) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= DETECTION_SIZE) sampleSize *= 2
    val bitmap = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize }) ?: return null
    val scale = bounds.outWidth.toDouble() / bitmap.width

    val image = Mat()
    Utils.bitmapToMat(bitmap, image)
    bitmap.recycle()
    val gray = Mat()
    Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGBA2GRAY)
    val quad = findDocumentQuad(gray) ?: findPaperQuad(image)
    gray.release()
    image.release()
    return quad?.flatMap { listOf((it.x * scale).toFloat(), (it.y * scale).toFloat()) }?.toFloatArray()
}

fun detectLiveCorners(luminance: ByteBuffer, rowStride: Int, width: Int, height: Int, rotationDegrees: Int): FloatArray? {
    if (!openCvLoaded) return null
    val bytes = ByteArray(width * height)
    if (rowStride == width) {
        luminance.get(bytes, 0, bytes.size)
    } else {
        for (row in 0 until height) {
            luminance.position(row * rowStride)
            luminance.get(bytes, row * width, width)
        }
    }
    val gray = Mat(height, width, CvType.CV_8UC1)
    gray.put(0, 0, bytes)
    val scale = LIVE_DETECTION_SIZE.toDouble() / max(width, height)
    if (scale < 1) Imgproc.resize(gray, gray, Size(width * scale, height * scale), 0.0, 0.0, Imgproc.INTER_AREA)
    when (rotationDegrees) {
        90 -> Core.rotate(gray, gray, Core.ROTATE_90_CLOCKWISE)
        180 -> Core.rotate(gray, gray, Core.ROTATE_180)
        270 -> Core.rotate(gray, gray, Core.ROTATE_90_COUNTERCLOCKWISE)
    }
    val quad = findDocumentQuad(gray)
    val frameWidth = gray.cols().toFloat()
    val frameHeight = gray.rows().toFloat()
    gray.release()
    return quad?.flatMap { listOf(it.x.toFloat() / frameWidth, it.y.toFloat() / frameHeight) }?.toFloatArray()
}

private fun findDocumentQuad(gray: Mat): Array<Point>? {
    val edges = Mat()
    Imgproc.GaussianBlur(gray, edges, Size(5.0, 5.0), 0.0)
    Imgproc.Canny(edges, edges, 50.0, 150.0)
    Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
    hierarchy.release()

    val minArea = gray.rows() * gray.cols() * MIN_DOCUMENT_AREA
    var best: Array<Point>? = null
    var bestArea = 0.0
    for (contour in contours) {
        val curve = MatOfPoint2f(*contour.toArray())
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(curve, approx, 0.02 * Imgproc.arcLength(curve, true), true)
        val points = approx.toArray()
        val area = abs(Imgproc.contourArea(approx))
        if (points.size == 4 && area > minArea && area > bestArea && Imgproc.isContourConvex(MatOfPoint(*points))) {
            best = points
            bestArea = area
        }
        curve.release()
        approx.release()
        contour.release()
    }

    val quad = best ?: findOuterQuad(edges, minArea)
    edges.release()
    return quad?.let { orderCorners(it) }
}

private fun findOuterQuad(edges: Mat, minArea: Double): Array<Point>? {
    val kernelSize = (max(edges.rows(), edges.cols()) / 29.0).roundToInt().coerceAtLeast(3).toDouble()
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize, kernelSize))
    val closed = Mat()
    Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel)
    kernel.release()
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(closed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    closed.release()
    hierarchy.release()

    val quad = largestFilledQuad(contours, minArea)
    contours.forEach { it.release() }
    return quad
}

private fun largestFilledQuad(contours: List<MatOfPoint>, minArea: Double): Array<Point>? {
    val largest = contours.maxByOrNull { Imgproc.contourArea(it) } ?: return null
    val contourArea = Imgproc.contourArea(largest)
    val quad = quadFromHull(largest) ?: return null
    val quadCurve = MatOfPoint2f(*quad)
    val quadArea = abs(Imgproc.contourArea(quadCurve))
    quadCurve.release()
    val quadPolygon = MatOfPoint(*quad)
    val convex = Imgproc.isContourConvex(quadPolygon)
    quadPolygon.release()
    return quad.takeIf { convex && quadArea > minArea && contourArea / quadArea >= MIN_OUTER_FILL }
}

private fun findPaperQuad(rgba: Mat): Array<Point>? {
    val hsv = Mat()
    Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGBA2RGB)
    Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV)
    val mask = Mat()
    Core.extractChannel(hsv, mask, 1)
    hsv.release()
    Imgproc.GaussianBlur(mask, mask, Size(5.0, 5.0), 0.0)
    Imgproc.threshold(mask, mask, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(9.0, 9.0))
    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel)
    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
    kernel.release()
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    val minArea = mask.rows() * mask.cols() * MIN_DOCUMENT_AREA
    mask.release()
    hierarchy.release()

    val quad = largestFilledQuad(contours, minArea)
    contours.forEach { it.release() }
    return quad?.let { orderCorners(it) }
}

private fun quadFromHull(contour: MatOfPoint): Array<Point>? {
    val points = contour.toArray()
    val indices = MatOfInt()
    Imgproc.convexHull(contour, indices)
    val hull = MatOfPoint2f(*indices.toArray().map { points[it] }.toTypedArray())
    indices.release()
    val perimeter = Imgproc.arcLength(hull, true)
    var quad: Array<Point>? = null
    for (epsilon in doubleArrayOf(0.02, 0.03, 0.04, 0.05, 0.06, 0.08)) {
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(hull, approx, epsilon * perimeter, true)
        val candidate = approx.toArray()
        approx.release()
        if (candidate.size == 4) {
            quad = candidate
            break
        }
    }
    hull.release()
    return quad
}

private fun orderCorners(corners: Array<Point>) = arrayOf(
    corners.minBy { it.x + it.y },
    corners.minBy { it.y - it.x },
    corners.maxBy { it.x + it.y },
    corners.maxBy { it.y - it.x }
)

fun cropPage(file: File, corners: FloatArray) {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: throw IllegalArgumentException("Failed to decode $file")
    if (corners.contentEquals(fullPageCorners(bitmap.width, bitmap.height))) {
        bitmap.recycle()
        return
    }
    if (!openCvLoaded) throw IllegalStateException("Unable to load OpenCV")
    val points = Array(4) { Point(corners[it * 2].toDouble(), corners[it * 2 + 1].toDouble()) }
    val width = max(distance(points[0], points[1]), distance(points[3], points[2])).roundToInt().coerceAtLeast(1)
    val height = max(distance(points[0], points[3]), distance(points[1], points[2])).roundToInt().coerceAtLeast(1)
    val target = arrayOf(Point(0.0, 0.0), Point(width.toDouble(), 0.0), Point(width.toDouble(), height.toDouble()), Point(0.0, height.toDouble()))

    val source = Mat()
    Utils.bitmapToMat(bitmap, source)
    bitmap.recycle()
    val transform = Imgproc.getPerspectiveTransform(MatOfPoint2f(*points), MatOfPoint2f(*target))
    val warped = Mat()
    Imgproc.warpPerspective(source, warped, transform, Size(width.toDouble(), height.toDouble()), Imgproc.INTER_LINEAR)
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(warped, result)
    source.release()
    transform.release()
    warped.release()
    writePage(result, 0f, file)
}

private fun distance(a: Point, b: Point) = hypot(a.x - b.x, a.y - b.y)
