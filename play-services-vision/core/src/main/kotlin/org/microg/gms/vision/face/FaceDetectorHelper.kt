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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import java.io.ByteArrayOutputStream
import kotlin.math.hypot

const val TAG = "FaceDetection"

class FaceDetectorHelper(context: Context) {

    private var faceDetectorYN: FaceDetectorYN? = null
    private var inputSize = Size(320.0, 320.0)
    private lateinit var rootMat: Mat
    private lateinit var facesMat: Mat

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

    fun detectFaces(bitmap: Bitmap, rotation: Int): List<Face> {
        Log.d(TAG, "detectFaces: source is bitmap")
        rootMat = bitmapToMat(bitmap) ?: return emptyList()
        return processMat(rootMat, rotation)
    }

    fun detectFaces(nv21ByteArray: ByteArray, width: Int, height: Int, rotation: Int): List<Face> {
        Log.d(TAG, "detectFaces: source is nv21Buffer")
        rootMat = nv21ToMat(nv21ByteArray, width, height) ?: return emptyList()
        return processMat(rootMat, rotation)
    }

    fun detectFaces(image: Image, rotation: Int): List<Face> {
        Log.d(TAG, "detectFaces: source is image")
        rootMat = imageToMat(image) ?: return emptyList()
        return processMat(rootMat, rotation)
    }

    fun release() {
        try {
            rootMat.release()
            facesMat.release()
            faceDetectorYN = null
        } catch (e: Exception) {
            Log.d(TAG, "release failed", e)
        }
    }

    private fun processMat(mat: Mat, rotation: Int): List<Face> {
        val faceDetector = faceDetectorYN ?: return emptyList()
        facesMat = Mat()
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
        val result = Mat()
        val status = faceDetectorYN!!.detect(facesMat, result)
        Log.d(TAG, "processMat: detect: $status facesMat: ${result.size()}")
        return parseDetections(result)
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
     */
    private fun parseDetections(detections: Mat): List<Face> {
        val faces = mutableListOf<Face>()
        val faceData = FloatArray(detections.cols() * detections.channels())
        for (i in 0 until detections.rows()) {
            detections.get(i, 0, faceData)
            val trackingId = (faceData[14] * 100).toInt()
            val boundingBox = Rect(faceData[0].toInt(), faceData[1].toInt(), (faceData[0] + faceData[2]).toInt(), (faceData[1] + faceData[3]).toInt())

            val rightEyeMark = FaceLandmark(FaceLandmark.RIGHT_EYE, PointF(faceData[4], faceData[5]))
            val leftEyeMark = FaceLandmark(FaceLandmark.LEFT_EYE, PointF(faceData[6], faceData[7]))
            val noseMark = FaceLandmark(FaceLandmark.NOSE_BASE, PointF(faceData[8], faceData[9]))
            val rightMouthMark = FaceLandmark(FaceLandmark.MOUTH_RIGHT, PointF(faceData[10], faceData[11]))
            val leftMouthMark = FaceLandmark(FaceLandmark.MOUTH_LEFT, PointF(faceData[12], faceData[13]))
            val leftCheckMark = FaceLandmark(FaceLandmark.LEFT_CHEEK, calculateMidPoint(leftEyeMark, leftMouthMark))
            val rightCheckMark = FaceLandmark(FaceLandmark.RIGHT_CHEEK, calculateMidPoint(rightEyeMark, rightMouthMark))

            val smilingProbability = calculateSmilingProbability(rightMouthMark, leftMouthMark)
            val rightEyeOpenProbability = calculateEyeOpenProbability(rightEyeMark, rightMouthMark)
            val leftEyeOpenProbability = calculateEyeOpenProbability(leftEyeMark, leftMouthMark)

            val rightEyeContour = FaceContour(FaceContour.RIGHT_EYE, arrayListOf(rightEyeMark.position))
            val leftEyeContour = FaceContour(FaceContour.LEFT_EYE, arrayListOf(leftEyeMark.position))
            val noseContour = FaceContour(FaceContour.NOSE_BRIDGE, arrayListOf(noseMark.position))
            val leftCheckContour = FaceContour(FaceContour.LEFT_CHEEK, arrayListOf(leftCheckMark.position))
            val rightCheckContour = FaceContour(FaceContour.RIGHT_CHEEK, arrayListOf(rightCheckMark.position))

            faces.add(Face(
                trackingId,
                boundingBox,
                0f,
                0f,
                0f,
                leftEyeOpenProbability,
                rightEyeOpenProbability,
                smilingProbability,
                0f,
                arrayListOf(rightEyeMark, leftEyeMark, noseMark, rightMouthMark, leftMouthMark, leftCheckMark, rightCheckMark),
                arrayListOf(rightEyeContour, leftEyeContour, noseContour, leftCheckContour, rightCheckContour)
            ).also {
                Log.d(TAG, "parseDetections: face->$it")
            })
        }
        Log.d(TAG, "parseDetections: faces->${faces.size}")
        return faces
    }

    private fun calculateSmilingProbability(rightMouthCorner: FaceLandmark, leftMouthCorner: FaceLandmark): Float {
        val mouthWidth = hypot(
            (rightMouthCorner.position.x - leftMouthCorner.position.x).toDouble(), (rightMouthCorner.position.y - leftMouthCorner.position.y).toDouble()
        ).toFloat()
        return (mouthWidth / 100).coerceIn(0f, 1f)
    }

    private fun calculateEyeOpenProbability(eye: FaceLandmark, mouthCorner: FaceLandmark): Float {
        val eyeMouthDistance = hypot(
            (eye.position.x - mouthCorner.position.x).toDouble(), (eye.position.y - mouthCorner.position.y).toDouble()
        ).toFloat()
        return (eyeMouthDistance / 50).coerceIn(0f, 1f)
    }

    private fun calculateMidPoint(eye: FaceLandmark, mouth: FaceLandmark): PointF {
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
        image.close()
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

