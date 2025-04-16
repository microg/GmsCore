/*
 * SPDX-FileCopyrightText: 2025, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.face.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.face.FaceDetectionOptions
import com.google.mlkit.vision.face.FrameMetadataParcel
import com.google.mlkit.vision.face.aidls.FaceParcel
import com.google.mlkit.vision.face.aidls.IFaceDetector
import org.microg.gms.vision.face.TAG
import org.microg.gms.vision.face.FaceDetectorHelper
import java.nio.ByteBuffer

class FaceDetector(val context: Context, val options: FaceDetectionOptions?) : IFaceDetector.Stub() {

    private var mFaceDetector: FaceDetectorHelper? = null

    override fun detectFaces(wrapper: IObjectWrapper?, metadata: FrameMetadataParcel?): List<FaceParcel> {
        Log.d(TAG, "MLKit detectFaces method: metadata:${metadata}")
        if (wrapper == null || metadata == null || mFaceDetector == null) return arrayListOf()
        val format = metadata.format
        val rotation = metadata.rotation
        if (format == -1) {
            val bitmap = wrapper.unwrap<Bitmap>() ?: return arrayListOf()
            return mFaceDetector?.detectFaces(bitmap, rotation) ?: arrayListOf()
        }
        if (format == ImageFormat.NV21) {
            val byteBuffer = wrapper.unwrap<ByteBuffer>() ?: return arrayListOf()
            return mFaceDetector?.detectFaces(byteBuffer.array(), metadata.width, metadata.height, rotation) ?: arrayListOf()
        }
        if (format == ImageFormat.YUV_420_888) {
            val image = wrapper.unwrap<Image>() ?: return arrayListOf()
            return mFaceDetector?.detectFaces(image, rotation) ?: arrayListOf()
        }
        return arrayListOf()
    }

    override fun initDetector() {
        Log.d(TAG, "MLKit initDetector method isInitialized")
        if (mFaceDetector == null) {
            try {
                mFaceDetector = FaceDetectorHelper(context)
            } catch (e: Exception) {
                Log.d(TAG, "initDetector: failed", e)
            }
        }
    }

    override fun close() {
        Log.d(TAG, "MLKit close")
        mFaceDetector?.release()
        mFaceDetector = null
    }
}