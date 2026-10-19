/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.aidls.ContourParcel
import com.google.mlkit.vision.face.aidls.FaceParcel
import com.google.mlkit.vision.face.aidls.LandmarkParcel
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

const val TAG = "FaceDetection"

/**
 * Number of points per contour, matching what ML Kit returns. Consumers may index into these
 * lists, so the counts (not just the types) are part of the API.
 */
private const val POINTS_FACE = 36
private const val POINTS_EYEBROW = 5
private const val POINTS_EYE = 16
private const val POINTS_UPPER_LIP_TOP = 11
private const val POINTS_LIP = 9
private const val POINTS_NOSE_BRIDGE = 2
private const val POINTS_NOSE_BOTTOM = 3

class FaceDetectorHelper(context: Context) {

    private var faceDetectorYN: FaceDetectorYN? = null
    private var inputSize = Size(320.0, 320.0)

    init {
        try {
            val buffer: ByteArray
            context.assets.open("face_detection_yunet_2023mar.onnx").use {
                val size = it.available()
                buffer = ByteArray(size)
                it.read(buffer)
            }
            faceDetectorYN = FaceDetectorYN.create("onnx", MatOfByte(*buffer), MatOfByte(), inputSize, 0.7f, 0.3f, 5000)
        } catch (e: Exception) {
            throw RuntimeException("faceDetectorYN initialization failed")
        }
    }

    fun detectFaces(bitmap: Bitmap, rotation: Int): List<FaceParcel> {
        Log.d(TAG, "detectFaces: source is bitmap")
        val rootMat = bitmapToMat(bitmap) ?: return emptyList()
        return try {
            processMat(rootMat, rotation)
        } finally {
            rootMat.release()
        }
    }

    fun detectFaces(nv21ByteArray: ByteArray, width: Int, height: Int, rotation: Int): List<FaceParcel> {
        Log.d(TAG, "detectFaces: source is nv21Buffer")
        val rootMat = nv21ToMat(nv21ByteArray, width, height) ?: return emptyList()
        return try {
            processMat(rootMat, rotation)
        } finally {
            rootMat.release()
        }
    }

    fun detectFaces(image: Image, rotation: Int): List<FaceParcel> {
        Log.d(TAG, "detectFaces: source is image")
        val rootMat = imageToMat(image) ?: return emptyList()
        return try {
            processMat(rootMat, rotation)
        } finally {
            rootMat.release()
        }
    }

    fun release() {
        try {
            faceDetectorYN = null
        } catch (e: Exception) {
            Log.d(TAG, "release failed", e)
        }
    }

    private fun processMat(mat: Mat, rotation: Int): List<FaceParcel> {
        val faceDetector = faceDetectorYN ?: return emptyList()
        if (mat.cols() <= 0 || mat.rows() <= 0) return emptyList()
        val facesMat = Mat()
        val result = Mat()
        try {
            val degree = degree(rotation)
            Log.d(TAG, "processMat: degree: $degree")
            when (degree) {
                2 -> Core.rotate(mat, facesMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                3 -> Core.rotate(mat, facesMat, Core.ROTATE_180)
                4 -> Core.rotate(mat, facesMat, Core.ROTATE_90_CLOCKWISE)
                else -> mat.copyTo(facesMat)
            }
            val matSize = Size(facesMat.cols().toDouble(), facesMat.rows().toDouble())
            Log.d(TAG, "processMat: inputSize: $inputSize")
            if (inputSize != matSize) {
                inputSize = matSize
                faceDetector.inputSize = matSize
            }
            Log.d(TAG, "processMat: matSize: $matSize")
            val status = faceDetector.detect(facesMat, result)
            Log.d(TAG, "processMat: detect: $status facesMat: ${result.size()}")
            return parseDetections(result, facesMat.cols(), facesMat.rows())
        } catch (e: Exception) {
            Log.w(TAG, "processMat: failed", e)
            return emptyList()
        } finally {
            facesMat.release()
            result.release()
        }
    }

    /**
     * faces: detection results stored in a 2D cv::Mat of shape [num_faces, 15]
     * 0-1: x, y of bbox top left corner
     * 2-3: width, height of bbox
     * 4-5: x, y of right eye (blue point in the example image)
     * 6-7: x, y of left eye (red point in the example image)
     * 8-9: x, y of nose tip (green point in the example image)
     * 10-11: x, y of right corner of mouth (pink point in the example image)
     * 12-13: x, y of left corner of mouth (yellow point in the example image)
     * 14: face score
     *
     * [frameWidth] and [frameHeight] are the dimensions of the (rotated) analysis frame the
     * detections were computed on. The model happily reports boxes that stick out of the frame,
     * so the box is clamped here - ML Kit guarantees an in-bounds bounding box.
     *
     * All returned coordinates are relative to that analysis frame, see [FaceFrameMapping].
     */
    private fun parseDetections(detections: Mat, frameWidth: Int, frameHeight: Int): List<FaceParcel> {
        val faces = mutableListOf<FaceParcel>()
        if (detections.rows() <= 0 || detections.cols() < 15) {
            Log.d(TAG, "parseDetections: no usable detections (rows=${detections.rows()}, cols=${detections.cols()})")
            return faces
        }
        val faceData = FloatArray(detections.cols() * detections.channels())
        for (i in 0 until detections.rows()) {
            detections.get(i, 0, faceData)
            val confidence = faceData[14]
            val boundingBox = Rect(faceData[0].toInt(), faceData[1].toInt(), (faceData[0] + faceData[2]).toInt(), (faceData[1] + faceData[3]).toInt())
            boundingBox.left = boundingBox.left.coerceIn(0, frameWidth)
            boundingBox.top = boundingBox.top.coerceIn(0, frameHeight)
            boundingBox.right = boundingBox.right.coerceIn(boundingBox.left, frameWidth)
            boundingBox.bottom = boundingBox.bottom.coerceIn(boundingBox.top, frameHeight)

            val leftEyeMark = LandmarkParcel(FaceLandmark.LEFT_EYE, PointF(faceData[4], faceData[5]))
            val mouthLeftMark = LandmarkParcel(FaceLandmark.MOUTH_LEFT, PointF(faceData[10], faceData[11]))
            val noseBaseMark = LandmarkParcel(FaceLandmark.NOSE_BASE, PointF(faceData[8], faceData[9]))
            val rightEyeMark = LandmarkParcel(FaceLandmark.RIGHT_EYE, PointF(faceData[6], faceData[7]))
            val mouthRightMark = LandmarkParcel(FaceLandmark.MOUTH_RIGHT, PointF(faceData[12], faceData[13]))

            // These are calculated for better compatibility, the model doesn't actually provide proper values here
            val mouthBottomMark = LandmarkParcel(FaceLandmark.MOUTH_BOTTOM, calculateMidPoint(mouthLeftMark, mouthRightMark))
            val leftCheekMark = LandmarkParcel(FaceLandmark.LEFT_CHEEK, calculateMidPoint(leftEyeMark, mouthLeftMark))
            val leftEarMark = LandmarkParcel(FaceLandmark.LEFT_EAR, PointF(boundingBox.right.toFloat(), noseBaseMark.position.y))
            val rightCheekMark = LandmarkParcel(FaceLandmark.RIGHT_CHEEK, calculateMidPoint(rightEyeMark, mouthRightMark))
            val rightEarMark = LandmarkParcel(FaceLandmark.RIGHT_EAR, PointF(boundingBox.left.toFloat(), noseBaseMark.position.y))

            val smilingProbability = calculateSmilingProbability(mouthLeftMark, mouthRightMark)
            val leftEyeOpenProbability = calculateEyeOpenProbability(rightEyeMark, mouthRightMark)
            val rightEyeOpenProbability = calculateEyeOpenProbability(leftEyeMark, mouthLeftMark)

            val eyeDistance = distance(leftEyeMark.position, rightEyeMark.position)
            val mouthWidth = distance(mouthLeftMark.position, mouthRightMark.position)

            // Contour point counts match ML Kit, the points themselves are interpolated from the
            // landmarks the model provides.
            val faceContour = ContourParcel(
                FaceContour.FACE,
                ellipsePoints(boundingBox.exactCenterX(), boundingBox.exactCenterY(), boundingBox.width() / 2f, boundingBox.height() / 2f, POINTS_FACE),
            )
            val leftEyebrowTopContour = ContourParcel(FaceContour.LEFT_EYEBROW_TOP, linePoints(leftEyeMark.position, eyeDistance, 0.32f, POINTS_EYEBROW))
            val leftEyebrowBottomContour = ContourParcel(FaceContour.LEFT_EYEBROW_BOTTOM, linePoints(leftEyeMark.position, eyeDistance, 0.22f, POINTS_EYEBROW))
            val rightEyebrowTopContour = ContourParcel(FaceContour.RIGHT_EYEBROW_TOP, linePoints(rightEyeMark.position, eyeDistance, 0.32f, POINTS_EYEBROW))
            val rightEyebrowBottomContour = ContourParcel(FaceContour.RIGHT_EYEBROW_BOTTOM, linePoints(rightEyeMark.position, eyeDistance, 0.22f, POINTS_EYEBROW))
            val leftEyeContour = ContourParcel(FaceContour.LEFT_EYE, ellipsePoints(leftEyeMark.position.x, leftEyeMark.position.y, eyeDistance * 0.16f, eyeDistance * 0.08f, POINTS_EYE))
            val rightEyeContour = ContourParcel(FaceContour.RIGHT_EYE, ellipsePoints(rightEyeMark.position.x, rightEyeMark.position.y, eyeDistance * 0.16f, eyeDistance * 0.08f, POINTS_EYE))
            val upperLipTopContour = ContourParcel(FaceContour.UPPER_LIP_TOP, curvePoints(mouthLeftMark.position, mouthRightMark.position, mouthWidth * 0.12f, POINTS_UPPER_LIP_TOP))
            val upperLipBottomContour = ContourParcel(FaceContour.UPPER_LIP_BOTTOM, curvePoints(mouthLeftMark.position, mouthRightMark.position, mouthWidth * 0.04f, POINTS_LIP))
            val lowerLipTopContour = ContourParcel(FaceContour.LOWER_LIP_TOP, curvePoints(mouthLeftMark.position, mouthRightMark.position, -mouthWidth * 0.04f, POINTS_LIP))
            val lowerLipBottomContour = ContourParcel(FaceContour.LOWER_LIP_BOTTOM, curvePoints(mouthLeftMark.position, mouthRightMark.position, -mouthWidth * 0.12f, POINTS_LIP))
            val noseBridgeContour = ContourParcel(FaceContour.NOSE_BRIDGE, verticalPoints(noseBaseMark.position, mouthBottomMark.position, POINTS_NOSE_BRIDGE))
            val noseBottomContour = ContourParcel(FaceContour.NOSE_BOTTOM, horizontalPoints(noseBaseMark.position, eyeDistance * 0.25f, POINTS_NOSE_BOTTOM))
            val leftCheekContour = ContourParcel(FaceContour.LEFT_CHEEK, arrayListOf(leftCheekMark.position))
            val rightCheekContour = ContourParcel(FaceContour.RIGHT_CHEEK, arrayListOf(rightCheekMark.position))

            faces.add(FaceParcel(
                i,
                boundingBox,
                0f,
                0f,
                0f,
                leftEyeOpenProbability,
                rightEyeOpenProbability,
                smilingProbability,
                confidence,
                arrayListOf(mouthBottomMark, leftCheekMark, leftEarMark, leftEyeMark, mouthLeftMark, noseBaseMark, rightCheekMark, rightEarMark, rightEyeMark, mouthRightMark),
                arrayListOf(faceContour, leftEyebrowTopContour, leftEyebrowBottomContour, rightEyebrowTopContour, rightEyebrowBottomContour, leftEyeContour, rightEyeContour, upperLipTopContour, upperLipBottomContour, lowerLipTopContour, lowerLipBottomContour, noseBridgeContour, noseBottomContour, leftCheekContour, rightCheekContour)
            ).also {
                Log.d(TAG, "parseDetections: face->$it")
            })
        }
        Log.d(TAG, "parseDetections: faces->${faces.size}")
        return faces
    }

    /** [count] points on an ellipse around ([cx], [cy]), starting at the top and going clockwise. */
    private fun ellipsePoints(cx: Float, cy: Float, rx: Float, ry: Float, count: Int): ArrayList<PointF> {
        val points = ArrayList<PointF>(count)
        for (i in 0 until count) {
            val t = 2.0 * PI * i / count
            points.add(PointF(cx + rx * sin(t).toFloat(), cy - ry * cos(t).toFloat()))
        }
        return points
    }

    /** [count] points on a horizontal line through [center], bent by [bulge] pixels in the middle. */
    private fun curvePoints(from: PointF, to: PointF, bulge: Float, count: Int): ArrayList<PointF> {
        val points = ArrayList<PointF>(count)
        for (i in 0 until count) {
            val t = if (count > 1) i / (count - 1f) else 0f
            points.add(PointF(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t + bulge * sin(PI * t).toFloat()))
        }
        return points
    }

    /** [count] points on a horizontal line of width [length] centered on [center]. */
    private fun horizontalPoints(center: PointF, length: Float, count: Int): ArrayList<PointF> {
        val points = ArrayList<PointF>(count)
        for (i in 0 until count) {
            val t = if (count > 1) i / (count - 1f) else 0.5f
            points.add(PointF(center.x - length / 2f + length * t, center.y))
        }
        return points
    }

    /** [count] points on a vertical line from [from] to [to]. */
    private fun verticalPoints(from: PointF, to: PointF, count: Int): ArrayList<PointF> {
        val points = ArrayList<PointF>(count)
        for (i in 0 until count) {
            val t = if (count > 1) i / (count - 1f) else 0f
            points.add(PointF(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t))
        }
        return points
    }

    /** Short curve above an eye, used for both eyebrow contours. */
    private fun linePoints(eye: PointF, eyeDistance: Float, rise: Float, count: Int): ArrayList<PointF> {
        val half = eyeDistance * 0.2f
        val y = eye.y - eyeDistance * rise
        return curvePoints(PointF(eye.x - half, y), PointF(eye.x + half, y), -eyeDistance * 0.04f, count)
    }

    private fun distance(a: PointF, b: PointF): Float = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun calculateSmilingProbability(rightMouthCorner: LandmarkParcel, leftMouthCorner: LandmarkParcel): Float {
        return (distance(rightMouthCorner.position, leftMouthCorner.position) / 100).coerceIn(0f, 1f)
    }

    private fun calculateEyeOpenProbability(eye: LandmarkParcel, mouthCorner: LandmarkParcel): Float {
        return (distance(eye.position, mouthCorner.position) / 50).coerceIn(0f, 1f)
    }

    private fun calculateMidPoint(eye: LandmarkParcel, mouth: LandmarkParcel): PointF {
        return PointF((eye.position.x + mouth.position.x) / 2, (eye.position.y + mouth.position.y) / 2)
    }

    private fun yuv420ToBitmap(image: Image): Bitmap? {
        val width = image.width
        val height = image.height

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21toBitmap(nv21, width, height)
    }

    private fun nv21toBitmap(byteArray: ByteArray, width: Int, height: Int): Bitmap? {
        try {
            val yuvImage = YuvImage(byteArray, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
            val jpegBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "nv21toBitmap: failed ", e)
            return null
        }
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat? {
        try {
            val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGR)
            return mat
        } catch (e: Exception) {
            Log.w(TAG, "bitmapToMat: failed", e)
            return null
        }
    }

    private fun imageToMat(image: Image): Mat? {
        val bitmap = when (image.format) {
            ImageFormat.JPEG -> {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            ImageFormat.YUV_420_888 -> {
                yuv420ToBitmap(image)
            }

            else -> {
                null
            }
        }
        return bitmap?.let { bitmapToMat(it) }
    }

    private fun nv21ToMat(nv21ByteArray: ByteArray, width: Int, height: Int): Mat? {
        val bitmap = nv21toBitmap(nv21ByteArray, width, height)
        return bitmap?.let { bitmapToMat(it) }
    }

    private fun degree(rotation: Int): Int {
        if (rotation == 0) return 1
        if (rotation == 1) return 4
        if (rotation == 2) return 3
        if (rotation == 3) return 2
        return 1
    }

}
