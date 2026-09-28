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
import androidx.core.os.bundleOf
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

class AgeSignalsService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind")
        return AgeSignalsServiceImpl(this, lifecycle).asBinder()
    }
}

internal class AgeSignalsServiceImpl(
    private val context: Context,
    override val lifecycle: Lifecycle,
) : IAgeSignalsService.Stub(), LifecycleOwner {

    override fun checkAgeSignals(packageName: String?, bundle: Bundle?, callback: IAgeSignalsServiceCallback?) {
        if (callback == null) {
            Log.w(TAG, "checkAgeSignals called without a callback")
            return
        }
        try {
            PackageUtils.getAndCheckCallingPackage(context, packageName)!!
        } catch (e: Exception) {
            Log.w(TAG, "checkAgeSignals called with invalid caller package: $packageName", e)
            sendError(callback::onError)
            return
        }

        val version = bundle?.getInt(KEY_PLAY_CORE_VERSION, 0) ?: 0
        val response = Bundle().apply {
            if (version >= CURRENT_API_VERSION) {
                putInt(KEY_AGE_SIGNALS_STATUS, AGE_SIGNALS_STATUS_NOT_SHARED)
            }
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
        try {
            PackageUtils.getAndCheckCallingPackage(context, packageName)!!
        } catch (e: Exception) {
            Log.w(TAG, "requestAgeSignalsAccess called with invalid caller package: $packageName", e)
            sendError(callback::onError)
            return
        }

        val response = bundleOf(KEY_AGE_SIGNALS_STATUS to AGE_SIGNALS_STATUS_NOT_SHARED)
        try {
            callback.onCompleteRequestAgeSignalsAccess(response)
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to deliver requestAgeSignalsAccess response for $packageName", e)
        }
    }

    private fun sendError(onError: (Bundle) -> Unit) {
        try {
            onError(bundleOf(KEY_ERROR_CODE to ERROR_CODE_INTERNAL_ERROR))
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to deliver error", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
