/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.assistedsignin

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

const val TAG = "AssistedSignInService"

class AssistedSignInService : BaseService(TAG, GmsService.IDENTITY_SIGN_IN) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(
            Feature("auth_api_credentials_begin_sign_in", 8L),
            Feature("auth_api_credentials_sign_out", 2L),
            Feature("auth_api_credentials_authorize", 1L),
            Feature("auth_api_credentials_revoke_access", 1L),
            Feature("auth_api_credentials_save_password", 4L),
            Feature("auth_api_credentials_get_sign_in_intent", 6L),
            Feature("auth_api_credentials_save_account_linking_token", 3L),
            Feature("auth_api_credentials_get_phone_number_hint_intent", 3L)
        )
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            AssistedSignInServiceImpl(this, request.packageName).asBinder(),
            connectionInfo
        )
    }
}