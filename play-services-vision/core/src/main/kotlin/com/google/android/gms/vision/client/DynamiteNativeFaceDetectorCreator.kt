/*
 * SPDX-FileCopyrightText: 2025, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.client

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.unwrap
import com.google.android.gms.vision.face.internal.client.DetectionOptions
import com.google.android.gms.vision.face.internal.client.INativeFaceDetector
import com.google.android.gms.vision.face.internal.client.INativeFaceDetectorCreator
import org.microg.gms.vision.face.TAG
import org.microg.gms.vision.face.FaceDetector
import org.opencv.android.OpenCVLoader

@Keep
class DynamiteNativeFaceDetectorCreator : INativeFaceDetectorCreator.Stub() {

    override fun newFaceDetector(context: IObjectWrapper?, faceDetectionOptions: DetectionOptions?): INativeFaceDetector? {
        Log.d(TAG, "DynamiteNativeFaceDetectorCreator newFaceDetector faceDetectionOptions:${faceDetectionOptions.toString()}")
        try {
            val elapsedRealtime = SystemClock.elapsedRealtime()
            val context = context.unwrap<Context>() ?: throw RuntimeException("Context is null")
            val remoteContext = GooglePlayServicesUtil.getRemoteContext(context) ?: throw RuntimeException("remoteContext is null")
            Log.d(TAG, "newFaceDetector: context: ${context.packageName} remoteContext: ${remoteContext.packageName}")
            if (!OpenCVLoader.initLocal()) {
                throw RuntimeException("Unable to load OpenCV")
            }
            Log.d(TAG, "DynamiteNativeFaceDetectorCreator newFaceDetector: load <openCV> library in ${SystemClock.elapsedRealtime() - elapsedRealtime}ms")
            return FaceDetector(remoteContext, faceDetectionOptions)
        } catch (e: Throwable) {
            Log.w(TAG, "DynamiteNativeFaceDetectorCreator newFaceDetector load failed ", e)
            return null
        }
    }
}