/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.ageverification.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.google.android.play.agesignals.protocol.IAgeSignalsAccessCallback
import com.google.android.play.agesignals.protocol.IAgeSignalsService
import com.google.android.play.agesignals.protocol.IAgeSignalsServiceCallback
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AgeSignalsService"

private const val KEY_PLAY_CORE_VERSION = "playcore.version.code"
private const val KEY_AGE_SIGNALS_STATUS = "age.signals.status"
private const val KEY_ERROR_CODE = "error.code"

private const val AGE_SIGNALS_STATUS_NOT_SHARED = 2
private const val ERROR_CODE_INTERNAL_ERROR = -100
private const val CURRENT_API_VERSION = 4

internal fun createCheckAgeSignalsResponse(version: Int): Bundle = Bundle().apply {
    if (version >= CURRENT_API_VERSION) {
        putInt(KEY_AGE_SIGNALS_STATUS, AGE_SIGNALS_STATUS_NOT_SHARED)
    }
}

internal fun createAgeSignalsAccessResponse(): Bundle = Bundle().apply {
    putInt(KEY_AGE_SIGNALS_STATUS, AGE_SIGNALS_STATUS_NOT_SHARED)
}

internal fun createErrorResponse(): Bundle = Bundle().apply {
    putInt(KEY_ERROR_CODE, ERROR_CODE_INTERNAL_ERROR)
}

class AgeSignalsService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return AgeSignalsServiceImpl(this, lifecycle).asBinder()
    }
}

internal fun interface CallingPackageVerifier {
    fun verify(context: Context, packageName: String): String?
}

internal class AgeSignalsServiceImpl(
    private val context: Context,
    override val lifecycle: Lifecycle,
    private val verifier: CallingPackageVerifier = CallingPackageVerifier { verifierContext, packageName ->
        PackageUtils.getAndCheckCallingPackage(verifierContext, packageName)
    }
) : IAgeSignalsService.Stub(), LifecycleOwner {

    override fun checkAgeSignals(packageName: String?, bundle: Bundle?, callback: IAgeSignalsServiceCallback?) {
        if (callback == null) {
            Log.w(TAG, "checkAgeSignals called without a callback")
            return
        }
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "checkAgeSignals called without a package name")
            sendCheckError(callback)
            return
        }
        if (!verifyCallingPackage(packageName, "checkAgeSignals", callback::onError)) return

        val response = try {
            createCheckAgeSignalsResponse(bundle?.getInt(KEY_PLAY_CORE_VERSION, 0) ?: 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create checkAgeSignals response for $packageName", e)
            sendCheckError(callback)
            return
        }
        try {
            callback.onCompleteCheckAgeSignals(response)
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to deliver checkAgeSignals response for $packageName", e)
        }
    }

    override fun requestAgeSignalsAccess(packageName: String?, bundle: Bundle?, callback: IAgeSignalsAccessCallback?) {
        if (callback == null) {
            Log.w(TAG, "requestAgeSignalsAccess called without a callback")
            return
        }
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "requestAgeSignalsAccess called without a package name")
            sendAccessError(callback)
            return
        }
        if (!verifyCallingPackage(packageName, "requestAgeSignalsAccess", callback::onError)) return

        val response = try {
            createAgeSignalsAccessResponse()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create requestAgeSignalsAccess response for $packageName", e)
            sendAccessError(callback)
            return
        }
        try {
            callback.onCompleteRequestAgeSignalsAccess(response)
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to deliver requestAgeSignalsAccess response for $packageName", e)
        }
    }

    private fun verifyCallingPackage(packageName: String, method: String, onError: (Bundle) -> Unit): Boolean {
        val verifiedPackageName = try {
            verifier.verify(context, packageName)
        } catch (e: Exception) {
            Log.w(TAG, "$method rejected caller for $packageName", e)
            sendError(method, onError)
            return false
        }
        if (verifiedPackageName == null) {
            Log.w(TAG, "$method could not verify caller for $packageName")
            sendError(method, onError)
            return false
        }
        return true
    }

    private fun sendCheckError(callback: IAgeSignalsServiceCallback) {
        sendError("checkAgeSignals", callback::onError)
    }

    private fun sendAccessError(callback: IAgeSignalsAccessCallback) {
        sendError("requestAgeSignalsAccess", callback::onError)
    }

    private fun sendError(method: String, onError: (Bundle) -> Unit) {
        try {
            onError(createErrorResponse())
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to deliver $method error", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
