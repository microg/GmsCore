/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.google.android.gms.fido.Fido.FIDO2_KEY_CREDENTIAL_EXTRA
import com.google.android.gms.fido.fido2.api.common.*
import org.json.JSONObject
import org.microg.gms.common.GmsService
import org.microg.gms.fido.core.ui.ACTION_FIDO_AUTHENTICATE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CALLER
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CREDENTIAL_ID
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_OPTIONS
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SERVICE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_SOURCE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_TYPE
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.SOURCE_APP
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.SOURCE_BROWSER
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.TYPE_REGISTER
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.TYPE_SIGN

private const val TAG = "PublicKeyProxyActivity"
private const val REQUEST_CODE_FIDO = 1001

fun String?.isHttpsUrl(): Boolean = this?.startsWith("https://") == true

@RequiresApi(34)
class PublicKeyProxyActivity : CredentialProviderActivity() {

    override fun onProviderGetCredentialRequest(request: ProviderGetCredentialRequest) {
        val option = request.credentialOptions.firstOrNull() as? GetPublicKeyCredentialOption ?: throw IllegalArgumentException()

        Log.d(TAG, "get request json: ${option.requestJson}")

        val isBrowserRequest = request.callingAppInfo.origin.isHttpsUrl()

        val options = JSONObject(option.requestJson).parsePublicKeyCredentialRequestOptions()
        val credentialIdString = intent.getStringExtra(KEY_CREDENTIAL_ID)

        val (optionsBytes, source) = buildRequestOptions(options, isBrowserRequest, request.callingAppInfo.origin, option.clientDataHash)
        val fidoIntent = createFidoIntent(source, optionsBytes, request.callingAppInfo.packageName, TYPE_SIGN, credentialIdString)
        startActivityForResult(fidoIntent, REQUEST_CODE_FIDO)
    }

    fun buildRequestOptions(
        baseOptions: PublicKeyCredentialRequestOptions, isBrowserRequest: Boolean, origin: String?, clientDataHash: ByteArray?
    ): Pair<ByteArray, String> = if (isBrowserRequest && origin != null) {
        BrowserPublicKeyCredentialRequestOptions.Builder().setPublicKeyCredentialRequestOptions(baseOptions).setOrigin(origin.toUri()).apply { clientDataHash?.let(::setClientDataHash) }.build()
            .serializeToBytes() to SOURCE_BROWSER
    } else {
        baseOptions.serializeToBytes() to SOURCE_APP
    }

    override fun onProviderCreateCredentialRequest(request: ProviderCreateCredentialRequest) {
        val callingPackage = request.callingAppInfo.packageName
        val origin = request.callingAppInfo.origin
        val isBrowserRequest = origin.isHttpsUrl()
        val publicKeyRequest = request.callingRequest as CreatePublicKeyCredentialRequest

        Log.d(TAG, "Creating passkey for: $callingPackage, browser=$isBrowserRequest")

        val options = JSONObject(publicKeyRequest.requestJson).parsePublicKeyCredentialCreationOptions()

        Log.d(TAG, "handlePasskeyCreate: options: $options")

        val (optionsBytes, source) = buildCreationOptions(options, isBrowserRequest, origin, publicKeyRequest.clientDataHash)
        val fidoIntent = createFidoIntent(source, optionsBytes, callingPackage, TYPE_REGISTER)

        startActivityForResult(fidoIntent, REQUEST_CODE_FIDO)
        Log.d(TAG, "Launched FIDO authenticator by PasskeyCreate")
    }

    fun buildCreationOptions(
        baseOptions: PublicKeyCredentialCreationOptions, isBrowserRequest: Boolean, origin: String?, clientDataHash: ByteArray?
    ): Pair<ByteArray, String> = if (isBrowserRequest && origin != null) {
        BrowserPublicKeyCredentialCreationOptions.Builder().setPublicKeyCredentialCreationOptions(baseOptions).setOrigin(origin.toUri()).apply { clientDataHash?.let(::setClientDataHash) }.build()
            .serializeToBytes() to SOURCE_BROWSER
    } else {
        baseOptions.serializeToBytes() to SOURCE_APP
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE_FIDO) return
        if (resultCode != RESULT_OK || data == null) {
            Log.w(TAG, "FIDO activity canceled or failed")
            return finishWithException("Sign in canceled")
        }

        runCatching {
            val credentialBytes = data.getByteArrayExtra(FIDO2_KEY_CREDENTIAL_EXTRA)
                ?: return@runCatching finishWithException("No credential data in FIDO result")

            val publicKeyCredential = PublicKeyCredential.deserializeFromBytes(credentialBytes)

            (publicKeyCredential.response as? AuthenticatorErrorResponse)?.let { error ->
                Log.e(TAG, "FIDO error: ${error.errorMessage}")
                return@runCatching finishWithException(error.errorMessage)
            }

            handleFidoSuccess(publicKeyCredential)
        }.onFailure { e ->
            Log.e(TAG, "Error processing FIDO result", e)
            finishWithException(e.localizedMessage)
        }
    }

    fun createFidoIntent(
        source: String, optionsBytes: ByteArray, callingPackage: String, type: String, credentialIdString: String? = null
    ): Intent = Intent(ACTION_FIDO_AUTHENTICATE).apply {
        `package` = packageName
        putExtra(KEY_SERVICE, GmsService.FIDO2_API.SERVICE_ID)
        putExtra(KEY_SOURCE, source)
        putExtra(KEY_TYPE, type)
        putExtra(KEY_OPTIONS, optionsBytes)
        putExtra(KEY_CALLER, callingPackage)
        credentialIdString?.let { putExtra(KEY_CREDENTIAL_ID, it) }
    }


    private fun handleFidoSuccess(publicKeyCredential: PublicKeyCredential) = runCatching {
        when (val response = publicKeyCredential.response) {
            is AuthenticatorAttestationResponse -> {
                val responseJson = publicKeyCredential.toJson()
                Log.d(TAG, "Passkey created successfully: $responseJson")
                finishWithSuccess(CreatePublicKeyCredentialResponse(responseJson))
            }
            is AuthenticatorAssertionResponse -> {
                val responseJson = publicKeyCredential.toJson()
                Log.d(TAG, "Passkey authentication successful: $responseJson")
                finishWithSuccess(GetCredentialResponse(androidx.credentials.PublicKeyCredential(responseJson)))
            }
            else -> {
                Log.e(TAG, "Unknown response type: ${response.javaClass.simpleName}")
                finishWithException()
            }
        }
    }.onFailure { e ->
        Log.e(TAG, "Error handling FIDO success", e)
        finishWithException(e.localizedMessage)
    }
}
