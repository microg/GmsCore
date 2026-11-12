/*
 * SPDX-FileCopyrightText: 2026 e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.checkin

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.checkin.internal.ICheckinApiCallbacks
import com.google.android.gms.checkin.internal.ICheckinApiService
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "CheckinApiService"

private const val EXTRA_CHECKIN_NOW = "com.google.android.gms.checkin.EXTRA_CHECKIN_NOW"
private const val STATUS_CHECKIN_SUCCEEDED = 21021

class CheckinApiService : BaseService(TAG, GmsService.CHECKIN_API) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, CheckinApiServiceImpl(this, lifecycle), null)
    }
}

class CheckinApiServiceImpl(
    private val context: Context,
    override val lifecycle: Lifecycle
) : ICheckinApiService.Stub(), LifecycleOwner {

    override fun getAndroidId(callbacks: ICheckinApiCallbacks?) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.CHECKIN)
        val androidId = LastCheckinInfo.read(context).androidId
        callbacks?.onAndroidId(Status.SUCCESS, androidId.toString())
    }

    override fun checkin(callback: IStatusCallback?, extras: Bundle?) {
        PackageUtils.assertGooglePackagePermission(context, GooglePackagePermission.CHECKIN)
        val force = extras?.getBoolean(EXTRA_CHECKIN_NOW, false) ?: false
        lifecycleScope.launchWhenStarted {
            val statusCode = withContext(Dispatchers.IO) { runCheckin(force) }
            runCatching { callback?.onResult(Status(statusCode)) }
        }
    }

    private fun runCheckin(force: Boolean): Int {
        return try {
            CheckinManager.checkin(context, force)
            if (LastCheckinInfo.read(context).androidId != 0L) {
                STATUS_CHECKIN_SUCCEEDED
            } else {
                CommonStatusCodes.ERROR
            }
        } catch (e: Exception) {
            Log.w(TAG, "Checkin failed", e)
            CommonStatusCodes.NETWORK_ERROR
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
