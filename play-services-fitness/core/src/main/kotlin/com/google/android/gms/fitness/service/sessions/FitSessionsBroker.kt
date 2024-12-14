/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service.sessions;

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.fitness.internal.IGoogleFitSessionsApi
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.fitness.request.SessionRegistrationRequest
import com.google.android.gms.fitness.request.SessionStartRequest
import com.google.android.gms.fitness.request.SessionStopRequest
import com.google.android.gms.fitness.request.SessionUnregistrationRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "FitSessionsBroker"

class FitSessionsBroker : BaseService(TAG, GmsService.FITNESS_SESSIONS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, FitSessionsBrokerImpl(), null)
    }
}

class FitSessionsBrokerImpl : IGoogleFitSessionsApi.Stub() {
    override fun startRequest(startRequest: SessionStartRequest?) {
        Log.d(TAG, "Not implemented startRequest: $startRequest")
    }

    override fun stopRequest(stopRequest: SessionStopRequest?) {
        Log.d(TAG, "Not implemented stopRequest: $stopRequest")
    }

    override fun insertRequest(insetRequest: SessionInsertRequest?) {
        Log.d(TAG, "Not implemented insertRequest: $insetRequest")
    }

    override fun readRequest(readRequest: SessionReadRequest?) {
        Log.d(TAG, "Not implemented readRequest: $readRequest")
    }

    override fun registrationRequest(registrationRequest: SessionRegistrationRequest?) {
        Log.d(TAG, "Not implemented registrationRequest: $registrationRequest")
    }

    override fun unRegistrationRequest(unRegistrationRequest: SessionUnregistrationRequest?) {
        Log.d(TAG, "Not implemented unRegistrationRequest: $unRegistrationRequest")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}