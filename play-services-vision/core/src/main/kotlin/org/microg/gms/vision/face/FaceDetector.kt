/*
 * SPDX-FileCopyrightText: 2025, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.face

import android.content.Context
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.face.Contour
import com.google.android.gms.vision.face.Landmark
import com.google.android.gms.vision.face.internal.client.DetectionOptions
import com.google.android.gms.vision.face.internal.client.FaceParcel
import com.google.android.gms.vision.face.internal.client.INativeFaceDetector
import com.google.android.gms.vision.internal.FrameMetadataParcel
import com.google.mlkit.vision.face.Face
import java.nio.ByteBuffer

class FaceDetector(val context: Context, private val options: DetectionOptions?) : INativeFaceDetector.Stub() {

    private val mFaceDetector by lazy { FaceDetectorHelper(context) }

    override fun closeDetectorJni() {
        Log.d(TAG, "closeDetectorJni")
        mFaceDetector.release()
    }

    override fun isNativeFaceDetectorAvailable(i: Int): Boolean {
        Log.d(TAG, "isNativeFaceDetectorAvailable type:${i}")
        return true
    }

    override fun detectFacesFromPlanes(
        planeFirst: IObjectWrapper?,
        planeSencond: IObjectWrapper?,
        planeThird: IObjectWrapper?,
        firstPixelStride: Int,
        secondPixelStride: Int,
        thirdPixelStride: Int,
        firstRowStride: Int,
        secondRowStride: Int,
        thirdRowStride: Int,
        metadataParcel: FrameMetadataParcel?
    ): Array<FaceParcel> {
        Log.d(
            TAG,
            "detectFacesFromPlanes planeFirst:${planeFirst} ,planeSecond:${planeSencond} ,planeThird:${planeThird}," + "firstPixelStride:${firstPixelStride} ,secondPixelStride:${secondPixelStride} ,thirdPixelStride:${thirdPixelStride} ," + "firstRowStride:${firstRowStride} ,secondRowStride:${secondRowStride} ,thirdRowStride:${thirdRowStride}," + "metadataParcel:${metadataParcel}"
        )
        val yBuffer = planeFirst?.unwrap<ByteBuffer>() ?: return emptyArray()
        val uBuffer = planeSencond?.unwrap<ByteBuffer>() ?: return emptyArray()
        val vBuffer = planeThird?.unwrap<ByteBuffer>() ?: return emptyArray()
        val width = metadataParcel?.width ?: return emptyArray()
        val height = metadataParcel?.height ?: return emptyArray()
        val rotation = metadataParcel.rotation
        val nv21 = ByteArray(width * height * 3 / 2)
        var offset = 0
        for (row in 0 until height) {
            yBuffer.position(row * firstRowStride)
            yBuffer.get(nv21, offset, width)
            offset += width
        }
        val chromaWidth = width / 2
        val chromaHeight = height / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * secondRowStride + col * secondPixelStride
                val vIndex = row * thirdRowStride + col * thirdPixelStride
                nv21[offset++] = vBuffer.get(vIndex)
                nv21[offset++] = uBuffer.get(uIndex)
            }
        }
        return mFaceDetector.detectFaces(nv21, width, height, rotation).map {
            it.toFaceParcel()
        }.toTypedArray().also {
            it.forEach { Log.d(TAG, "detectFacesFromPlanes: $it") }
        }
    }

    override fun detectFaceParcels(wrapper: IObjectWrapper?, metadata: FrameMetadataParcel?): Array<FaceParcel> {
        Log.d(TAG, "detectFaceParcels byteBuffer:${wrapper} ,metadataParcel:${metadata}")
        if (wrapper == null || metadata == null) return emptyArray()
        val buffer = wrapper.unwrap<ByteBuffer>() ?: return emptyArray()
        return mFaceDetector.detectFaces(buffer.array(), metadata.width, metadata.height, metadata.rotation).map {
            it.toFaceParcel()
        }.toTypedArray().also {
            it.forEach { Log.d(TAG, "detectFaceParcels: $it") }
        }
    }
}

private fun com.google.mlkit.vision.face.aidls.FaceParcel.toFaceParcel() = FaceParcel(
    1,
    id,
    (boundingBox.left + boundingBox.width() / 2).toFloat(),
    (boundingBox.top + boundingBox.height() / 2).toFloat(),
    boundingBox.width().toFloat(),
    boundingBox.height().toFloat(),
    panAngle,
    rollAngle,
    tiltAngle,
    landmarkParcelList.map { landmark -> Landmark(landmark.type, landmark.position.x, landmark.position.y, landmark.type) }.toTypedArray(),
    leftEyeOpenProbability,
    rightEyeOpenProbability,
    smileProbability,
    contourParcelList.map { contour -> Contour(contour.type, contour.pointsList) }.toTypedArray(),
    confidenceScore
)