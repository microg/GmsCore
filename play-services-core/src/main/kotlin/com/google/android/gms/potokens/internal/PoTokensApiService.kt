/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.internal

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.potokens.utils.PoTokenConstants
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

const val TAG = "PoTokensApi"

class PoTokensApiService : BaseService(TAG, GmsService.POTOKENS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(Feature(PoTokenConstants.PO_TOKENS, 1))
        Log.d(TAG, "PoTokensApiService handleServiceRequest")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS, PoTokensApiServiceImpl(
                applicationContext, request.packageName, lifecycleScope
            ), connectionInfo
        )
    }
}
