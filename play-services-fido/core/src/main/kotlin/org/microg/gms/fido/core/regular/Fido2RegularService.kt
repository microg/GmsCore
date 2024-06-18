/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.regular

import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GmsService.FIDO2_REGULAR
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.fido.fido2.api.IBooleanCallback
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.internal.regular.IFido2AppCallbacks
import com.google.android.gms.fido.fido2.internal.regular.IFido2AppService

private const val TAG = "Fido2RegularService"

class Fido2RegularService : BaseService(TAG, FIDO2_REGULAR) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        if (request.packageName.isNullOrEmpty()) {
            Log.d(TAG, "Invalid service request")
            callback.onPostInitComplete(CommonStatusCodes.ERROR, null, null)
            return
        }
        Log.d(TAG, "handleServiceRequest start by ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, Fido2RegularServiceImpl().asBinder(), ConnectionInfo().apply {
            features = arrayOf(
                    Feature("is_user_verifying_platform_authenticator_available", 1),
                    Feature("is_user_verifying_platform_authenticator_available_for_credential", 1),
            )
        })
    }
}

class Fido2RegularServiceImpl : IFido2AppService.Stub() {

    override fun nativeAppRegister(callback: IFido2AppCallbacks?, options: PublicKeyCredentialCreationOptions?) {
        Log.d(TAG, "nativeAppRegister called")
        callback?.onResult(Status.INTERNAL_ERROR, null)
    }

    override fun nativeAppSignIn(callback: IFido2AppCallbacks?, options: PublicKeyCredentialRequestOptions?) {
        Log.d(TAG, "nativeAppSignIn called")
        callback?.onResult(Status.INTERNAL_ERROR, null)
    }

    override fun nativeAppIsUserVerifyingPlatformAuthenticatorAvailable(callback: IBooleanCallback?) {
        Log.d(TAG, "nativeAppIsUserVerifyingPlatformAuthenticatorAvailable called")
        callback?.onBoolean(false)
    }

    override fun nativeAppIsUserVerifyingPlatformAuthenticatorAvailableForCredential(callback: IBooleanCallback?, rpId: String?, keyHandles: ByteArray?) {
        Log.d(TAG, "nativeAppIsUserVerifyingPlatformAuthenticatorAvailableForCredential called")
        callback?.onBoolean(false)
    }
}