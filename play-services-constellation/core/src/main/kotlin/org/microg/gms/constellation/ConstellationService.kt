/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ConstellationService"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(
            Feature("constellation", 1),
            Feature("constellation_phone_number_verification", 1)
        )
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            ConstellationServiceImpl(this, request.packageName).asBinder(),
            connectionInfo
        )
    }
}
