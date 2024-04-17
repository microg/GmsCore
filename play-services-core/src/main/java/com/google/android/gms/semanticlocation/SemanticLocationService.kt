/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.semanticlocation

import android.app.PendingIntent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.semanticlocation.internal.ISemanticLocationService
import com.google.android.gms.semanticlocation.internal.SemanticLocationParameters
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "SemanticLocationService"

private val FEATURES = arrayOf(
    Feature("semanticlocation_events", 1L),
)

class SemanticLocationService : BaseService(TAG, GmsService.SEMANTIC_LOCATION) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo().apply {
            features = FEATURES
        }
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            SemanticLocationServiceImpl().asBinder(),
            connectionInfo
        )
    }
}

class SemanticLocationServiceImpl : ISemanticLocationService.Stub() {
    override fun registerSemanticLocationEvents(
        params: SemanticLocationParameters,
        callback: IStatusCallback,
        request: SemanticLocationEventRequest,
        pendingIntent: PendingIntent
    ) {
        Log.d(TAG, "registerSemanticLocationEvents: $params")
    }

    override fun setIncognitoMode(params: SemanticLocationParameters, callback: IStatusCallback, mode: Boolean) {
        Log.d(TAG, "setIncognitoMode: $params")
    }

    override fun unregisterSemanticLocationEvents(params: SemanticLocationParameters, callback: IStatusCallback, pendingIntent: PendingIntent) {
        Log.d(TAG, "unregisterSemanticLocationEvents: $params")
    }
}
