/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.accounts.AccountManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.internal.IBeginSignInCallback
import com.google.android.gms.auth.api.identity.internal.IGetPhoneNumberHintIntentCallback
import com.google.android.gms.auth.api.identity.internal.IGetSignInIntentCallback
import com.google.android.gms.auth.api.identity.internal.ISignInService
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.signin.ACTION_ASSISTED_SIGN_IN
import org.microg.gms.auth.signin.BEGIN_SIGN_IN_REQUEST
import org.microg.gms.auth.signin.GET_SIGN_IN_INTENT_REQUEST
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.CLIENT_PACKAGE_NAME
import org.microg.gms.auth.signin.GOOGLE_SIGN_IN_OPTIONS
import org.microg.gms.auth.signin.performSignOut
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService

private const val TAG = "IdentitySignInService"

class IdentitySignInService : BaseService(TAG, GmsService.IDENTITY_SIGN_IN) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = FEATURES
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, IdentitySignInServiceImpl(this, request.packageName).asBinder(), connectionInfo
        )
    }
}

class IdentitySignInServiceImpl(private val mContext: Context, private val clientPackageName: String) :
    ISignInService.Stub() {

    private val requestMap = mutableMapOf<String, GoogleSignInOptions>()

    override fun beginSignIn(callback: IBeginSignInCallback, request: BeginSignInRequest) {
        Log.d(TAG, "method 'beginSignIn' called")
        Log.d(TAG, "request-> $request")
        if (request.googleIdTokenRequestOptions.serverClientId.isNullOrEmpty()) {
            Log.d(TAG, "serverClientId is empty, return CANCELED ")
            callback.onResult(Status.CANCELED, null)
            return
        }
        val bundle = Bundle().apply {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(request.googleIdTokenRequestOptions.serverClientId).build()
            putByteArray(BEGIN_SIGN_IN_REQUEST, SafeParcelableSerializer.serializeToBytes(request))
            putByteArray(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(options))
            putString(CLIENT_PACKAGE_NAME, clientPackageName)
            requestMap[request.sessionId] = options
        }
        callback.onResult(Status.SUCCESS, BeginSignInResult(performSignIn(bundle)))
    }

    override fun signOut(callback: IStatusCallback, requestTag: String) {
        Log.d(TAG, "method signOut called, requestTag=$requestTag")
        if (requestMap.containsKey(requestTag)) {
            val accounts = AccountManager.get(mContext).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            if (accounts.isNotEmpty()) {
                accounts.forEach { performSignOut(mContext, clientPackageName, requestMap[requestTag], it) }
            }
        }
        callback.onResult(Status.SUCCESS)
    }

    override fun getSignInIntent(
        callback: IGetSignInIntentCallback, request: GetSignInIntentRequest
    ) {
        Log.d(TAG, "method 'getSignInIntent' called")
        Log.d(TAG, "request-> $request")
        if (request.serverClientId.isNullOrEmpty()) {
            Log.d(TAG, "serverClientId is empty, return CANCELED ")
            callback.onResult(Status.CANCELED, null)
            return
        }
        val bundle = Bundle().apply {
            val options =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(request.serverClientId)
                    .build()
            putByteArray(GET_SIGN_IN_INTENT_REQUEST, SafeParcelableSerializer.serializeToBytes(request))
            putByteArray(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(options))
            putString(CLIENT_PACKAGE_NAME, clientPackageName)
            requestMap[request.sessionId] = options
        }
        callback.onResult(Status.SUCCESS, performSignIn(bundle))
    }

    override fun getPhoneNumberHintIntent(
        callback: IGetPhoneNumberHintIntentCallback, request: GetPhoneNumberHintIntentRequest
    ) {
        Log.w(TAG, "method 'getPhoneNumberHintIntent' not fully implemented, return status is CANCELED.")
        callback.onResult(Status.CANCELED, null)
    }

    private fun performSignIn(bundle: Bundle): PendingIntent {
        val intent = Intent(ACTION_ASSISTED_SIGN_IN).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            putExtras(bundle)
        }
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(mContext, 0, intent, flags)
    }

}