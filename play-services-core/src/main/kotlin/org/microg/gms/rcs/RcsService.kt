/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.Binder
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GmsRcsService"

/**
 * Google Messages binds [GmsService.RCS] (`com.google.android.gms.rcs.START`) during setup.
 * DummyService answers that bind with [com.google.android.gms.common.ConnectionResult.API_DISABLED].
 *
 * Unknown binder codes are logged via [warnOnTransactionIssues] so they can be mapped later.
 */
class RcsService : BaseService(TAG, GmsService.RCS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        if (!PackageUtils.isGooglePackage(this, packageName)) {
            throw SecurityException("$packageName is not a Google package")
        }
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RcsServiceImpl(),
            ConnectionInfo()
        )
    }
}

class RcsServiceImpl : Binder() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
