/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.credentials.buildAssistedSignInIntent
import org.microg.gms.auth.signin.GET_SIGN_IN_INTENT_REQUEST

private const val TAG = "SignInProxyActivity"
private const val REQUEST_CODE_SIGN_IN = 100

@RequiresApi(34)
class SignInProxyActivity : CredentialProviderActivity() {

    override fun onProviderGetCredentialRequest(request: ProviderGetCredentialRequest) {
        val serverClientId = intent.getStringExtra(GOOGLE_ID_SIWG_SERVER_CLIENT_ID).orEmpty()
        val signInRequest = GetSignInIntentRequest.builder()
            .setServerClientId(serverClientId)
            .apply { intent.getStringExtra(GOOGLE_ID_SIWG_NONCE)?.let { setNonce(it) } }
            .build()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .apply { intent.getStringExtra(GOOGLE_ID_SIWG_ACCOUNT_NAME)?.let { setAccountName(it) } }
            .build()
        val signInIntent = buildAssistedSignInIntent(
            requestExtraKey = GET_SIGN_IN_INTENT_REQUEST,
            serializedRequest = SafeParcelableSerializer.serializeToBytes(signInRequest),
            googleSignInOptions = googleSignInOptions,
            callingPackage = intent.getStringExtra(GOOGLE_ID_SIWG_CALLER_PACKAGE)
        )
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onProviderCreateCredentialRequest(request: ProviderCreateCredentialRequest) {
        finishWithException("Unsupported create credential request")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_SIGN_IN) return
        if (resultCode != RESULT_OK || data == null) {
            Log.w(TAG, "SignIn activity canceled or failed")
            return finishWithException("Sign in canceled", getExceptionCreator = ::GetCredentialCancellationException)
        }
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")
        handleSignInSuccess(data)
    }

    private fun handleSignInSuccess(data: Intent?) = runCatching {
        Log.d(TAG, "handleSignInSuccess")

        val credentialBytes = data?.getByteArrayExtra(AuthConstants.SIGN_IN_CREDENTIAL)
            ?: run {
                Log.e(TAG, "No credential data in result")
                return@runCatching finishWithException("No credential data")
            }

        val signInCredential = SafeParcelableSerializer.deserializeFromBytes(
            credentialBytes,
            SignInCredential.CREATOR
        )

        Log.d(TAG, "Got SignInCredential: email=${signInCredential.id}, googleIdToken=${signInCredential.googleIdToken?.take(50)}...")

        val credentialData = Bundle().apply {
            putString(GOOGLE_ID_BUNDLE_KEY_ID, signInCredential.id)
            putString(GOOGLE_ID_BUNDLE_KEY_ID_TOKEN, signInCredential.googleIdToken)
            putString(GOOGLE_ID_BUNDLE_KEY_DISPLAY_NAME, signInCredential.displayName)
            putString(GOOGLE_ID_BUNDLE_KEY_GIVEN_NAME, signInCredential.givenName)
            putString(GOOGLE_ID_BUNDLE_KEY_FAMILY_NAME, signInCredential.familyName)
            putString(GOOGLE_ID_BUNDLE_KEY_PHONE_NUMBER, signInCredential.phoneNumber)
            putParcelable(GOOGLE_ID_BUNDLE_KEY_PROFILE_PICTURE_URI, signInCredential.profilePictureUri)
        }

        val credential = CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, credentialData)
        val response = GetCredentialResponse(credential)

        val resultIntent = Intent()
        PendingIntentHandler.setGetCredentialResponse(resultIntent, response)
        setResult(RESULT_OK, resultIntent)

        Log.d(TAG, "Returning credential to Credential Manager")
        finish()
    }.onFailure { e ->
        Log.e(TAG, "Error processing sign-in result", e)
        finishWithException(e.message)
    }
}
