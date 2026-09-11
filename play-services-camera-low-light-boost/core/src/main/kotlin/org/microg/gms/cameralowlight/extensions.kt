/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cameralowlight

import android.util.Log
import android.view.Surface
import com.google.android.gms.common.Feature
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostCallback
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostSession
import com.google.android.libraries.camera.capture.lowlightboost.internal.LowLightBoostOptionsParcelable

internal const val TAG = "CameraLowLight"

private const val LOW_LIGHT_BOOST_MODE_DISABLED = 1
private const val LOW_LIGHT_BOOST_MODE_ENABLED = 2
private const val MAX_CAPTURE_DIMENSION = 8_192
private const val MAX_CAPTURE_PIXELS = 33_554_432L

internal fun cameraLowLightFeatures(): Array<Feature> = arrayOf(
    Feature("CAMERA_LOW_LIGHT_MANAGEMENT", 1),
    Feature("LOW_LIGHT_BOOST", 1),
    Feature("VIDEO_TIMESTAMP_FIX", 1),
)

internal object LowLightBoostWireStatus {
    const val SUCCESS = 12
    const val MAX_SESSIONS_REACHED = 1
    const val SESSION_INIT_FAILED = 3
    const val SERVICE_RELEASED = 4
    const val RENDER_FAILED = 7
    const val BINDER_DIED = 16
}

internal val Int.isSupportedLowLightBoostMode: Boolean
    get() = this == LOW_LIGHT_BOOST_MODE_DISABLED || this == LOW_LIGHT_BOOST_MODE_ENABLED

internal val Int.isLowLightBoostEnabled: Boolean
    get() = this == LOW_LIGHT_BOOST_MODE_ENABLED

internal fun LowLightBoostOptionsParcelable.isValidForSession(): Boolean {
    return target?.isValid == true &&
            !cameraId.isNullOrBlank() &&
            captureWidth in 1..MAX_CAPTURE_DIMENSION &&
            captureHeight in 1..MAX_CAPTURE_DIMENSION &&
            captureWidth.toLong() * captureHeight <= MAX_CAPTURE_PIXELS &&
            enableLowLightBoost.isSupportedLowLightBoostMode
}

internal fun ILowLightBoostCallback.tryNotifySessionStatus(status: Int): Boolean {
    return try {
        onSessionCreated(status, null, null)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to deliver session status $status", e)
        false
    }
}

internal fun ILowLightBoostCallback.tryNotifySessionCreated(
    session: ILowLightBoostSession,
    cameraSurface: Surface,
): Boolean {
    return try {
        onSessionCreated(LowLightBoostWireStatus.SUCCESS, session, cameraSurface)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Client disconnected before receiving the low light boost session", e)
        false
    }
}

internal fun ILowLightBoostCallback.tryNotifySessionDestroyed(): Boolean {
    return try {
        onSessionDestroyed()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to notify client that the session was destroyed", e)
        false
    }
}

internal fun ILowLightBoostCallback.tryNotifySessionDisconnected(status: Int): Boolean {
    return try {
        onSessionDisconnected(status)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to report session disconnection; client disconnected", e)
        false
    }
}

internal fun ILowLightBoostCallback.tryNotifySceneBrightness(boostStrength: Float): Boolean {
    return try {
        onSceneBrightnessChanged(boostStrength)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Failed to report boost strength; client disconnected", e)
        false
    }
}
