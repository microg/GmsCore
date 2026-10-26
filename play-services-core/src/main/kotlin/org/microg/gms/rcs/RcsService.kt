/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import android.os.Parcel
import android.os.RemoteCallbackList
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.rcs.RcsConfigRequest
import com.google.android.gms.rcs.RcsConfiguration
import com.google.android.gms.rcs.internal.IRcsApiService
import com.google.android.gms.rcs.internal.IRcsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "RcsService"

class RcsService : BaseService(TAG, GmsService.RCS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, RcsServiceImpl(this, packageName).asBinder(), null)
    }
}

class RcsServiceImpl(private val context: Context, private val packageName: String?) : IRcsApiService.Stub() {
    private val listeners = RemoteCallbackList<IRcsCallbacks>()

    override fun getRcsConfiguration(request: RcsConfigRequest?, callbacks: IRcsCallbacks) {
        Log.d(TAG, "getRcsConfiguration: $request")
        val config = RcsConfiguration(
            true, // rcsEnabled
            "https://rcs.google.com",
            request?.msisdn ?: "",
            byteArrayOf(0x01)
        )
        try {
            callbacks.onRcsConfig(Status.SUCCESS, config)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deliver onRcsConfig callback", e)
        }
    }

    override fun registerRcsListener(callbacks: IRcsCallbacks?) {
        if (callbacks != null) {
            listeners.register(callbacks)
            try {
                callbacks.onRcsStatusChanged(1) // 1 = CONNECTED / READY
            } catch (e: Exception) {
                Log.w(TAG, "Failed to notify status on register", e)
            }
        }
    }

    override fun unregisterRcsListener(callbacks: IRcsCallbacks?) {
        if (callbacks != null) {
            listeners.unregister(callbacks)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
