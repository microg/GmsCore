/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.clearcut

import android.content.Context
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import com.google.android.gms.clearcut.internal.IBootCountCallbacks
import com.google.android.gms.clearcut.internal.IBootCountService
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "BootCountService"

class BootCountService : BaseService(TAG, GmsService.BOOT_COUNT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest")
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, BootCountServiceImpl(this), null)
    }
}

class BootCountServiceImpl(private val context: Context) : IBootCountService.Stub() {
    override fun getBootCount(callbacks: IBootCountCallbacks?) {
        Log.d(TAG, "getBootCount called")
        val bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 1)
        callbacks?.onBootCount(Status.SUCCESS, bootCount)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
