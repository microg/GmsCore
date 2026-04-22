/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "RcsService"

class RcsService : BaseService(TAG, GmsService.RCS) {

    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        Log.d(TAG, "handleServiceRequest for package: ${request.packageName}")
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, null, null)
    }
}
