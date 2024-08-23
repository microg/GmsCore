/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.TelemetryData
import com.google.android.gms.common.internal.service.IClientTelemetryService
import org.microg.gms.BaseService

private const val TAG = "ClientTelemetryService"

class ClientTelemetryService : BaseService(TAG, GmsService.TELEMETRY) {
    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        callback?.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            ClientTelemetryServiceImpl(lifecycle).asBinder(),
            ConnectionInfo().apply {
                features = arrayOf(Feature("CLIENT_TELEMETRY", 1))
            }
        )
    }
}

class ClientTelemetryServiceImpl(override val lifecycle: Lifecycle) : IClientTelemetryService.Stub(), LifecycleOwner {

    override fun log(data: TelemetryData?) {
        Log.d(TAG, "log: $data")
    }

}