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
import org.microg.gms.auth.signin.ACTION_ASSISTED_SIGN_IN
import org.microg.gms.auth.signin.CLIENT_PACKAGE_NAME
import org.microg.gms.auth.signin.GET_SIGN_IN_INTENT_REQUEST
import org.microg.gms.auth.signin.GOOGLE_SIGN_IN_OPTIONS

private const val TAG = "SignInProxyActivity"
private const val REQUEST_CODE_SIGN_IN = 100

@RequiresApi(34)
class SignInProxyActivity : CredentialProviderActivity() {

    override fun onProviderGetCredentialRequest(request: ProviderGetCredentialRequest) {
        val bundle = Bundle().apply {
            val signInRequest = GetSignInIntentRequest.builder()
                .setServerClientId(intent.getStringExtra(GOOGLE_ID_SIWG_SERVER_CLIENT_ID) ?: "")
                .apply {
                    intent.getStringExtra(GOOGLE_ID_SIWG_NONCE)?.let { setNonce(it) }
                }
                .build()

            val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(intent.getStringExtra(GOOGLE_ID_SIWG_SERVER_CLIENT_ID) ?: "")
                .apply { intent.getStringExtra(GOOGLE_ID_SIWG_ACCOUNT_NAME)?.let { setAccountName(it) } }
                .build()

            putByteArray(GET_SIGN_IN_INTENT_REQUEST, SafeParcelableSerializer.serializeToBytes(signInRequest))
            putByteArray(GOOGLE_SIGN_IN_OPTIONS, SafeParcelableSerializer.serializeToBytes(googleSignInOptions))
            putString(CLIENT_PACKAGE_NAME, intent.getStringExtra(GOOGLE_ID_SIWG_CALLER_PACKAGE))
        }
        startActivityForResult(
            Intent(ACTION_ASSISTED_SIGN_IN).apply {
                `package` = packageName
                putExtras(bundle)
            },
            REQUEST_CODE_SIGN_IN
        )
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
            putString(GOOGLE_ID_BUNDLE_KEY_PROFILE_PICTURE_URI, signInCredential.profilePictureUri?.toString())
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
