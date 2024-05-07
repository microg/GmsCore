/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials

import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.internal.*
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "CredentialService"

val FEATURES = arrayOf(
    Feature("auth_api_credentials_begin_sign_in", 9),
    Feature("auth_api_credentials_sign_out", 2),
    Feature("auth_api_credentials_authorize", 1),
    Feature("auth_api_credentials_revoke_access", 1),
    Feature("auth_api_credentials_save_password", 4),
    Feature("auth_api_credentials_get_sign_in_intent", 6),
    Feature("auth_api_credentials_save_account_linking_token", 3),
    Feature("auth_api_credentials_get_phone_number_hint_intent", 3)
)

class CredentialsService : BaseService(TAG, GmsService.CREDENTIALS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = CredentialsServiceImpl(packageName).asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply { features = FEATURES })
    }
}

class CredentialsServiceImpl(private val packageName: String) : ICredentialsService.Stub() {
    override fun request(callbacks: ICredentialsCallbacks, request: CredentialRequest) {
        Log.d(TAG, "request($request)")
        callbacks.onStatus(Status.CANCELED)
    }

    override fun save(callbacks: ICredentialsCallbacks, request: SaveRequest) {
        Log.d(TAG, "save($request)")
        callbacks.onStatus(Status.CANCELED)
    }

    override fun delete(callbacks: ICredentialsCallbacks, request: DeleteRequest) {
        Log.d(TAG, "delete($request)")
        callbacks.onStatus(Status.CANCELED)
    }

    override fun disableAutoSignIn(callbacks: ICredentialsCallbacks) {
        Log.d(TAG, "disableAutoSignIn()")
        callbacks.onStatus(Status.SUCCESS)
    }

    override fun generatePassword(callbacks: ICredentialsCallbacks, request: GeneratePasswordRequest) {
        Log.d(TAG, "generatePassword($request)")
        callbacks.onStatus(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
