/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.audit

import android.util.Log
import com.google.android.gms.audit.LogAuditRecordsRequest
import com.google.android.gms.audit.internal.IAuditService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AuditApiService"

class AuditService : BaseService(TAG, GmsService.AUDIT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(ConnectionResult.SUCCESS, AuditServiceImpl().asBinder(), null)
    }

}

class AuditServiceImpl : IAuditService.Stub() {

    override fun logAuditRecords(request: LogAuditRecordsRequest?, callback: IStatusCallback) {
        Log.d(TAG, "method 'logAuditRecords' not fully implemented, only return Status.SUCCESS")
        when (request?.writeMode) {
            1 -> {
                callback.onResult(Status.SUCCESS)
            }
            2 -> {
                callback.onResult(Status.SUCCESS_CACHE)
            }
            else -> {
                callback.onResult(Status.SUCCESS)
            }
        }
    }

}