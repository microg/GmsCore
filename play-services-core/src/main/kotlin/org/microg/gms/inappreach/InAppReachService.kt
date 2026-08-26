/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.inappreach

import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "InAppReachService"
private val FEATURES = arrayOf(
    Feature("account_health_alerts", 1),
    Feature("account_messages", 1),
    Feature("account_data_response", 1),
    Feature("account_data_response_v2", 1)
)

class InAppReachService : BaseService(TAG, GmsService.INAPP_REACH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            InAppReachServiceImpl(this, packageName, lifecycle).asBinder(),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}
