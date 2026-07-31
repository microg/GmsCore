/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import com.google.firebase.auth.api.internal.WearOsSupportAidlRequest

private const val TAG = "GmsFirebaseAuthWearOs"

class WearOsSupportService : BaseService(TAG, GmsService.FIREBASE_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService?) {
        Log.d(TAG, "WearOS support service request received")
        // For now, just respond with success without additional implementation
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, null, null)
    }
}
