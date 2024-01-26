/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.audit.internal

import android.util.Log
import com.google.android.gms.audit.LogAuditRecordsRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AuditApiService"

class AuditApiService : BaseService(TAG, GmsService.AUDIT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(ConnectionResult.SUCCESS, AuditApiServiceImpl().asBinder(), null)
    }

}

class AuditApiServiceImpl : IAuditService.Stub() {

    override fun logAuditRecords(logAuditRecordsRequest: LogAuditRecordsRequest?, callback: IStatusCallback) {
        Log.d(TAG, "method 'logAuditRecords' not fully implemented, only return Status.SUCCESS")
        callback.onResult(Status.SUCCESS)
    }

}