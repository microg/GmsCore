/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.internal.IBeginSignInCallback
import com.google.android.gms.auth.api.identity.internal.IGetPhoneNumberHintIntentCallback
import com.google.android.gms.auth.api.identity.internal.IGetSignInIntentCallback
import com.google.android.gms.auth.api.identity.internal.ISignInService
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.AuthSignInActivity
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService

const val TAG = "IdentitySignInService"

class IdentitySignInService : BaseService(TAG, GmsService.IDENTITY_SIGN_IN) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = FEATURES
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS,
            IdentitySignInServiceImpl(this, request.packageName).asBinder(),
            connectionInfo
        )
    }
}

class IdentitySignInServiceImpl(private val mContext: Context, private val clientPackageName: String) :
    ISignInService.Stub() {
    override fun beginSignIn(callback: IBeginSignInCallback, request: BeginSignInRequest) {
        Log.d(TAG, "method 'beginSignIn' not fully implemented, return status is CANCELED")
        callback.onResult(Status.CANCELED, null)
    }

    override fun signOut(callback: IStatusCallback, requestTag: String) {
        Log.d(TAG, "method signOut called, requestTag=$requestTag")
        callback.onResult(Status.SUCCESS)
    }

    override fun getSignInIntent(
        callback: IGetSignInIntentCallback,
        request: GetSignInIntentRequest
    ) {
        val signInConfiguration = SignInConfiguration().apply {
            options = GoogleSignInOptions.Builder()
                .requestIdToken(request.clientId)
                .requestId()
                .requestEmail()
                .requestProfile()
                .build()
            packageName = clientPackageName
        }
        val intent = Intent(mContext, AuthSignInActivity::class.java).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            putExtra("config", signInConfiguration)
        }
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        val activity = PendingIntent.getActivity(mContext, 0, intent, flags)
        callback.onResult(Status.SUCCESS, activity)
    }

    override fun getPhoneNumberHintIntent(
        callback: IGetPhoneNumberHintIntentCallback,
        request: GetPhoneNumberHintIntentRequest
    ) {
        Log.w(TAG, "method 'getPhoneNumberHintIntent' not fully implemented, return status is CANCELED.")
        callback.onResult(Status.CANCELED, null)
    }
}