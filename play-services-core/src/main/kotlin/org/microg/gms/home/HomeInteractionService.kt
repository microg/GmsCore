/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.home

import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.home.interaction.OnRequestParams
import com.google.android.gms.home.interaction.internal.IInteractionService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "HomeInteractionSvc"
private const val RESPONSE_COLUMN = "InteractionClientResponse"

private val FEATURES = arrayOf(
    Feature("home_interaction", 1),
    Feature("home_wifi_presence", 1),
    Feature("home_send_commands_large", 1),
    Feature("home_consume_batch_notifications", 1),
    Feature("home_client_session_handshake", 1),
    Feature("home_matter_commission_device_headless", 1),
    Feature("home_cursor_window_for_interaction_client", 1),
)

class HomeInteractionService : BaseService(TAG, GmsService.HOME) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            HomeInteractionServiceImpl().asBinder(),
            ConnectionInfo().apply { features = FEATURES },
        )
    }
}

class HomeInteractionServiceImpl : IInteractionService.Stub() {
    override fun onRequest(params: OnRequestParams?) {
        val response = DataHolder.builder(arrayOf(RESPONSE_COLUMN))
            .withRow(hashMapOf(RESPONSE_COLUMN to byteArrayOf()))
            .build(CommonStatusCodes.SUCCESS)
        runCatching { params?.completionCallback?.onComplete(response) }
            .onFailure { Log.w(TAG, "Failed to complete interaction request", it) }
    }
}
