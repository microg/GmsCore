/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gmscompliance

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.gmscompliance.GmsDeviceComplianceResponse
import com.google.android.gms.gmscompliance.IGmsDeviceComplianceService
import com.google.android.gms.gmscompliance.IGmsDeviceComplianceServiceCallback
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

const val TAG = "DeviceCompliance"

class GmsDeviceComplianceService : BaseService(TAG, GmsService.GMS_COMPLIANCE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, GmsDeviceComplianceServiceImpl(lifecycle).asBinder(), ConnectionInfo().apply {
            features = arrayOf(
                Feature("gmscompliance_api", 1)
            )
        });
    }
}

class GmsDeviceComplianceServiceImpl(override val lifecycle: Lifecycle) : IGmsDeviceComplianceService.Stub(), LifecycleOwner {
    override fun getDeviceCompliance(callback: IGmsDeviceComplianceServiceCallback?) {
        Log.d(TAG, "getDeviceCompliance()")
        lifecycleScope.launchWhenStarted {
            try {
                callback?.onResponse(Status.SUCCESS, GmsDeviceComplianceResponse().apply { compliant = true })
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }


}
