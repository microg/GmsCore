/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.BaseService
import org.microg.gms.auth.signin.ACTION_ASSISTED_SIGN_IN
import org.microg.gms.auth.signin.BEGIN_SIGN_IN_REQUEST
import org.microg.gms.auth.signin.GET_SIGN_IN_INTENT_REQUEST
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.CLIENT_PACKAGE_NAME
import org.microg.gms.auth.signin.GOOGLE_SIGN_IN_OPTIONS
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.auth.signin.performSignOut
import org.microg.gms.common.AccountUtils
import org.microg.gms.common.GmsService
import org.microg.gms.fido.core.Database
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_OPTIONS
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SERVICE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SOURCE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_TYPE

private const val TAG = "IdentitySignInService"

class IdentitySignInService : BaseService(TAG, GmsService.AUTH_API_IDENTITY_SIGNIN) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = FEATURES
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, IdentitySignInServiceImpl(this, request.packageName, lifecycle).asBinder(), connectionInfo
        )
    }
}

class IdentitySignInServiceImpl(private val context: Context, private val clientPackageName: String, override val lifecycle: Lifecycle) :
    ISignInService.Stub(), LifecycleOwner {

    private val requestMap = mutableMapOf<String, GoogleSignInOptions>()

    override fun beginSignIn(callback: IBeginSignInCallback, request: BeginSignInRequest) {
        Log.d(TAG, "method 'beginSignIn' called")
        Log.d(TAG, "request-> $request")
        if (request.googleIdTokenRequestOptions.isSupported) {
            if (request.googleIdTokenRequestOptions.serverClientId.isNullOrEmpty()) {
                Log.d(TAG, "serverClientId is empty, return CANCELED ")
                callback.onResult(Status.CANCELED, null)
                return
            }
            val bundle = Bundle().apply {
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(request.googleIdTokenRequestOptions.serverClientId).build()
                putByteArray(BEGIN_SIGN_IN_REQUEST, SafeParcelableSerializer.serializeToBytes(request))
                putByteArray(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(options))
                putString(CLIENT_PACKAGE_NAME, clientPackageName)
                requestMap[request.sessionId] = options
            }
            callback.onResult(Status.SUCCESS, BeginSignInResult(performGoogleSignIn(bundle)))
        } else if (request.passkeyJsonRequestOptions.isSupported) {
            fun JSONObject.getArrayOrNull(key: String) = if (has(key)) getJSONArray(key) else null
            fun <T> JSONArray.map(fn: (JSONObject) -> T): List<T> =  (0 until length()).map { fn(getJSONObject(it)) }
            fun <T> JSONArray.map(fn: (String) -> T): List<T> =  (0 until length()).map { fn(getString(it)) }
            val json = JSONObject(request.passkeyJsonRequestOptions.requestJson)
            val rpId = json.getString("rpId")
            val options = PublicKeyCredentialRequestOptions.Builder()
                .setAllowList(json.getArrayOrNull("allowCredentials")?.let { allowCredentials -> allowCredentials.map { credential: JSONObject ->
                    PublicKeyCredentialDescriptor(
                        credential.getString("type"),
                        Base64.decode(credential.getString("id"), Base64.URL_SAFE),
                        credential.getArrayOrNull("transports")?.let { transports -> transports.map { transportString: String ->
                            Transport.fromString(transportString)
                        } }.orEmpty()
                    )
                } })
                .setChallenge(Base64.decode(json.getString("challenge"), Base64.URL_SAFE))
                .setRequireUserVerification(json.optString("userVerification").takeIf { it.isNotBlank() }?.let { UserVerificationRequirement.fromString(it) })
                .setRpId(rpId)
                .setTimeoutSeconds(json.optDouble("timeout", -1.0).takeIf { it > 0 })
                .build()
            if (request.isPreferImmediatelyAvailableCredentials && Database(context).getKnownRegistrationInfo(rpId).isEmpty()) {
                Log.d(TAG, "need available Credential")
                callback.onResult(Status.CANCELED, null)
                return
            }
            val bundle = bundleOf(
                KEY_SERVICE to GmsService.AUTH_API_IDENTITY_SIGNIN.SERVICE_ID,
                KEY_SOURCE to "app",
                KEY_TYPE to "sign",
                KEY_OPTIONS to options.serializeToBytes()
            )
            callback.onResult(Status.SUCCESS, BeginSignInResult(performFidoSignIn(bundle)))
        } else {
            callback.onResult(Status.INTERNAL_ERROR, null)
        }
    }

    override fun signOut(callback: IStatusCallback, requestTag: String) {
        Log.d(TAG, "method signOut called, requestTag=$requestTag")
        lifecycleScope.launch {
            val signInAccount = SignInConfigurationService.getDefaultAccount(context, clientPackageName)
            val authOptions = SignInConfigurationService.getAuthOptions(context, clientPackageName).plus(requestMap[requestTag])
            if (signInAccount != null && authOptions.isNotEmpty()) {
                authOptions.forEach {
                    performSignOut(context, clientPackageName, it, signInAccount)
                }
            }
            AccountUtils.get(context).removeSelectedAccount(clientPackageName)
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
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken(request.serverClientId)
                    .build()
            putByteArray(GET_SIGN_IN_INTENT_REQUEST, SafeParcelableSerializer.serializeToBytes(request))
            putByteArray(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(options))
            putString(CLIENT_PACKAGE_NAME, clientPackageName)
            requestMap[request.sessionId] = options
        }
        callback.onResult(Status.SUCCESS, performGoogleSignIn(bundle))
    }

    override fun getPhoneNumberHintIntent(
        callback: IGetPhoneNumberHintIntentCallback, request: GetPhoneNumberHintIntentRequest
    ) {
        Log.w(TAG, "method 'getPhoneNumberHintIntent' not fully implemented, return status is CANCELED.")
        callback.onResult(Status.CANCELED, null)
    }

    private fun performGoogleSignIn(bundle: Bundle): PendingIntent {
        val intent = Intent(ACTION_ASSISTED_SIGN_IN).apply {
            `package` = context.packageName
            putExtras(bundle)
        }
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntentCompat.getActivity(context, 0, intent, flags, false)!!
    }

    private fun performFidoSignIn(bundle: Bundle): PendingIntent {
        val intent = Intent(context, IdentityFidoProxyActivity::class.java).apply {
            putExtras(bundle)
        }
        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntentCompat.getActivity(context, 0, intent, flags, false)!!
    }

}