/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.mlkit.vision.face.bundled.internal

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.mlkit.vision.face.FaceDetectionOptions
import com.google.mlkit.vision.face.aidls.IFaceDetector
import com.google.mlkit.vision.face.aidls.IFaceDetectorCreator
import org.microg.gms.vision.face.TAG
import org.microg.gms.vision.face.mlkit.FaceDetector
import org.opencv.android.OpenCVLoader

@Keep
class ThickFaceDetectorCreator : IFaceDetectorCreator.Stub() {

    override fun newFaceDetector(context: IObjectWrapper?, faceDetectionOptions: FaceDetectionOptions?): IFaceDetector? {
        Log.d(TAG, "MLKit newFaceDetector options:${faceDetectionOptions}")
        try {
            val elapsedRealtime = SystemClock.elapsedRealtime()
            val context = context.unwrap<Context>() ?: throw RuntimeException("Context is null")
            val remoteContext = GooglePlayServicesUtil.getRemoteContext(context) ?: throw RuntimeException("remoteContext is null")
            Log.d(TAG, "ThickFaceDetectorCreator newFaceDetector: context: ${context.packageName} remoteContext: ${remoteContext.packageName}")
            if (!OpenCVLoader.initLocal()) {
                throw RuntimeException("Unable to load OpenCV")
            }
            Log.d(TAG, "ThickFaceDetectorCreator newFaceDetector: load <openCV> library in ${SystemClock.elapsedRealtime() - elapsedRealtime}ms")
            return FaceDetector(remoteContext, faceDetectionOptions)
        } catch (e: Throwable) {
            Log.w(TAG, "ThickFaceDetectorCreator newFaceDetector load failed ", e)
            return null
        }
    }
}