/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.fido.Fido.FIDO2_KEY_CREDENTIAL_EXTRA
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.CredentialOption
import com.google.android.gms.identitycredentials.GetCredentialRequest
import org.json.JSONObject
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.credentials.buildAssistedSignInIntent
import org.microg.gms.auth.credentials.buildFidoAuthenticateIntent
import org.microg.gms.identitycredentials.EXTRA_CALLING_PACKAGE
import org.microg.gms.identitycredentials.EXTRA_CREATE_REQUEST
import org.microg.gms.identitycredentials.EXTRA_GET_REQUEST
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_ANDROIDX_AUTO_SELECT
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_DISPLAY_NAME
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_FAMILY_NAME
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_GIVEN_NAME
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_ID
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_ID_TOKEN
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_BUNDLE_KEY_PROFILE_PICTURE_URI
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_FILTER_BY_AUTHORIZED_ACCOUNTS
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_NONCE
import org.microg.gms.auth.credentials.provider.GOOGLE_ID_SERVER_CLIENT_ID
import org.microg.gms.auth.credentials.provider.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import org.microg.gms.auth.credentials.provider.parsePublicKeyCredentialCreationOptions
import org.microg.gms.auth.credentials.provider.parsePublicKeyCredentialRequestOptions
import org.microg.gms.auth.credentials.provider.toJson
import org.microg.gms.auth.signin.BEGIN_SIGN_IN_REQUEST
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.SOURCE_APP
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.TYPE_REGISTER
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.TYPE_SIGN
import org.microg.gms.profile.Build

private const val TAG = "IdentityCredChooser"

class IdentityCredentialChooserActivity : AppCompatActivity() {

    private var callingPackage: String = ""
    private var isCreatePath = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        callingPackage = intent.getStringExtra(EXTRA_CALLING_PACKAGE).orEmpty()
        runCatching {
            val getReq: GetCredentialRequest? = readParcelableExtra(EXTRA_GET_REQUEST, GetCredentialRequest::class.java)
            val createReq: CreateCredentialRequest? = readParcelableExtra(EXTRA_CREATE_REQUEST, CreateCredentialRequest::class.java)
            when {
                getReq != null -> {
                    isCreatePath = false
                    Log.d(TAG, "onCreate get pkg=$callingPackage options=${getReq.credentialOptions.size}")
                    routeGet(getReq)
                }
                createReq != null -> {
                    isCreatePath = true
                    Log.d(TAG, "onCreate create pkg=$callingPackage type=${createReq.type} origin=${createReq.origin}")
                    routeCreate(createReq)
                }
                else -> finishWithGetException(GetCredentialUnknownException("Missing request payload"))
            }
        }.onFailure { e ->
            Log.e(TAG, "onCreate parse failed", e)
            if (isCreatePath) finishWithCreateException(CreateCredentialUnknownException(e.message ?: "Internal error"))
            else finishWithGetException(GetCredentialUnknownException(e.message ?: "Internal error"))
        }
    }

    private fun routeGet(req: GetCredentialRequest) {
        val option = req.credentialOptions.firstOrNull()
            ?: return finishWithGetException(NoCredentialException("No credential options requested"))
        when (option.type) {
            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> startPasskeyGet(option)
            TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> startGoogleSignIn(option)
            PasswordCredential.TYPE_PASSWORD_CREDENTIAL ->
                finishWithGetException(NoCredentialException("Password credentials are not stored"))
            else -> finishWithGetException(NoCredentialException("Unsupported type: ${option.type}"))
        }
    }

    private fun routeCreate(req: CreateCredentialRequest) {
        Log.d(TAG, "routeCreate type=${req.type} pkg=$callingPackage")
        when (req.type) {
            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> startPasskeyCreate(req)
            else -> finishWithCreateException(CreateCredentialNoCreateOptionException("Unsupported create type: ${req.type}"))
        }
    }

    private fun startPasskeyGet(option: CredentialOption) {
        val data = option.credentialRetrievalData
        val requestJson = data?.getString(PUBKEY_REQ_JSON_KEY)
            ?: return finishWithGetException(NoCredentialException("Passkey request missing requestJson"))
        val pkOptions = runCatching {
            JSONObject(requestJson).parsePublicKeyCredentialRequestOptions()
        }.getOrElse {
            Log.e(TAG, "Passkey JSON parse failed", it)
            return finishWithGetException(GetCredentialUnknownException("Invalid passkey requestJson"))
        }
        val fidoIntent = buildFidoAuthenticateIntent(SOURCE_APP, pkOptions.serializeToBytes(), callingPackage, TYPE_SIGN)
        startActivityForResult(fidoIntent, REQ_CODE_FIDO)
    }

    private fun startPasskeyCreate(req: CreateCredentialRequest) {
        val requestJson = req.requestJson ?: req.credentialData.getString(PUBKEY_REQ_JSON_KEY)
        if (requestJson.isNullOrBlank()) {
            return finishWithCreateException(CreateCredentialUnknownException("Passkey create missing requestJson"))
        }
        val pkOptions = runCatching {
            JSONObject(requestJson).parsePublicKeyCredentialCreationOptions()
        }.getOrElse {
            Log.e(TAG, "Passkey create JSON parse failed", it)
            return finishWithCreateException(CreateCredentialUnknownException("Invalid passkey requestJson"))
        }
        val fidoIntent = buildFidoAuthenticateIntent(SOURCE_APP, pkOptions.serializeToBytes(), callingPackage, TYPE_REGISTER)
        startActivityForResult(fidoIntent, REQ_CODE_FIDO)
    }

    private fun startGoogleSignIn(option: CredentialOption) {
        val data = option.credentialRetrievalData ?: Bundle()
        val serverClientId = data.getString(GOOGLE_ID_SERVER_CLIENT_ID).orEmpty()
        if (serverClientId.isBlank()) {
            return finishWithGetException(NoCredentialException("GoogleIdToken option missing serverClientId"))
        }
        val nonce = data.getString(GOOGLE_ID_NONCE)
        val filterByAuthorized = data.getBoolean(GOOGLE_ID_FILTER_BY_AUTHORIZED_ACCOUNTS, false)
        val autoSelect = data.getBoolean(GOOGLE_ID_ANDROIDX_AUTO_SELECT, false)

        val signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(filterByAuthorized)
                    .apply { nonce?.let { setNonce(it) } }
                    .build()
            )
            .setAutoSelectEnabled(autoSelect)
            .build()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()

        val signInIntent = buildAssistedSignInIntent(
            requestExtraKey = BEGIN_SIGN_IN_REQUEST,
            serializedRequest = SafeParcelableSerializer.serializeToBytes(signInRequest),
            googleSignInOptions = gso,
            callingPackage = callingPackage
        )
        startActivityForResult(signInIntent, REQ_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode: $requestCode resultCode: $resultCode")
        when (requestCode) {
            REQ_CODE_FIDO -> handleFidoResult(resultCode, data)
            REQ_CODE_SIGN_IN -> handleSignInResult(resultCode, data)
            else -> finishWithGetException(GetCredentialUnknownException("Unexpected requestCode=$requestCode"))
        }
    }

    private fun handleFidoResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "handleFidoResult: data: $data")
        if (resultCode != RESULT_OK || data == null) {
            return if (isCreatePath) finishWithCreateException(CreateCredentialUnknownException("Passkey flow canceled"))
            else finishWithGetException(GetCredentialCancellationException("Passkey flow canceled"))
        }
        runCatching {
            val credentialBytes = data.getByteArrayExtra(FIDO2_KEY_CREDENTIAL_EXTRA)
                ?: throw IllegalStateException("FIDO returned no credential")
            val publicKeyCredential = com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
                .deserializeFromBytes(credentialBytes)
            (publicKeyCredential.response as? AuthenticatorErrorResponse)?.let { err ->
                throw IllegalStateException(err.errorMessage ?: err.errorCode.toString())
            }
            val json = publicKeyCredential.toJson()
            val credData = Bundle().apply {
                putString(if (isCreatePath) PUBKEY_RES_REG_JSON_KEY else PUBKEY_RES_AUTH_JSON_KEY, json)
            }
            Log.d(TAG, "handleFidoResult: $credData")
            finishWithCredential(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL, credData)
        }.onFailure { e ->
            Log.e(TAG, "handleFidoResult failed", e)
            val msg = e.message ?: "FIDO result error"
            if (isCreatePath) finishWithCreateException(CreateCredentialUnknownException(msg))
            else finishWithGetException(GetCredentialUnknownException(msg))
        }
    }

    private fun handleSignInResult(resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            return finishWithGetException(GetCredentialCancellationException("Sign-in canceled"))
        }
        runCatching {
            val bytes = data.getByteArrayExtra(AuthConstants.SIGN_IN_CREDENTIAL)
                ?: throw IllegalStateException("Sign-in result missing credential")
            val credential = SafeParcelableSerializer.deserializeFromBytes(bytes, SignInCredential.CREATOR)
            val credData = Bundle().apply {
                putString(GOOGLE_ID_BUNDLE_KEY_ID, credential.id)
                credential.googleIdToken?.let { putString(GOOGLE_ID_BUNDLE_KEY_ID_TOKEN, it) }
                credential.displayName?.let { putString(GOOGLE_ID_BUNDLE_KEY_DISPLAY_NAME, it) }
                credential.givenName?.let { putString(GOOGLE_ID_BUNDLE_KEY_GIVEN_NAME, it) }
                credential.familyName?.let { putString(GOOGLE_ID_BUNDLE_KEY_FAMILY_NAME, it) }
                credential.profilePictureUri?.let { putString(GOOGLE_ID_BUNDLE_KEY_PROFILE_PICTURE_URI, it.toString()) }
            }
            finishWithCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, credData)
        }.onFailure { e ->
            Log.e(TAG, "handleSignInResult failed", e)
            finishWithGetException(GetCredentialUnknownException(e.message ?: "Sign-in result error"))
        }
    }

    private fun finishWithCredential(type: String, credentialData: Bundle) {
        val responseBundle = Bundle().apply {
            if (isCreatePath) {
                putString(PROVIDER_EXTRA_CREATE_RESPONSE_TYPE, type)
                putBundle(PROVIDER_EXTRA_CREATE_REQUEST_DATA, credentialData)
            } else {
                putString(PROVIDER_EXTRA_CREDENTIAL_TYPE, type)
                putBundle(PROVIDER_EXTRA_CREDENTIAL_DATA, credentialData)
            }
        }
        val extraKey = if (isCreatePath) EXTRA_CREATE_RESPONSE_BUNDLE else EXTRA_GET_RESPONSE_BUNDLE
        Log.d(TAG, "finishWithCredential: extraKey: $extraKey responseBundle: $responseBundle")
        setResult(RESULT_OK, Intent().putExtra(extraKey, responseBundle))
        finish()
    }

    private fun finishWithGetException(e: androidx.credentials.exceptions.GetCredentialException) {
        finishWithExceptionBundle(EXTRA_GET_EXCEPTION_BUNDLE, e.type, e.message ?: e.javaClass.simpleName)
    }

    private fun finishWithCreateException(e: androidx.credentials.exceptions.CreateCredentialException) {
        finishWithExceptionBundle(EXTRA_CREATE_EXCEPTION_BUNDLE, e.type, e.message ?: e.javaClass.simpleName)
    }

    private fun finishWithExceptionBundle(extraKey: String, type: String, message: String) {
        val bundle = Bundle().apply {
            putString(PROVIDER_EXTRA_EXCEPTION_TYPE, type)
            putString(PROVIDER_EXTRA_EXCEPTION_MESSAGE, message)
        }
        setResult(RESULT_OK, Intent().putExtra(extraKey, bundle))
        finish()
    }

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun <T : Parcelable> readParcelableExtra(name: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(name, clazz)
        else intent.getParcelableExtra<Parcelable>(name) as? T

    companion object {
        private const val REQ_CODE_FIDO = 0x1001
        private const val REQ_CODE_SIGN_IN = 0x1002

        private const val EXTRA_GET_RESPONSE_BUNDLE = "android.service.credentials.extra.GET_CREDENTIAL_RESPONSE"
        private const val EXTRA_GET_EXCEPTION_BUNDLE = "android.service.credentials.extra.GET_CREDENTIAL_EXCEPTION"
        private const val EXTRA_CREATE_RESPONSE_BUNDLE = "android.service.credentials.extra.CREATE_CREDENTIAL_RESPONSE"
        private const val EXTRA_CREATE_EXCEPTION_BUNDLE = "android.service.credentials.extra.CREATE_CREDENTIAL_EXCEPTION"
        private const val PROVIDER_EXTRA_CREDENTIAL_TYPE = "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_TYPE"
        private const val PROVIDER_EXTRA_CREDENTIAL_DATA = "androidx.credentials.provider.extra.EXTRA_CREDENTIAL_DATA"
        private const val PROVIDER_EXTRA_CREATE_RESPONSE_TYPE = "androidx.credentials.provider.extra.CREATE_CREDENTIAL_RESPONSE_TYPE"
        private const val PROVIDER_EXTRA_CREATE_REQUEST_DATA = "androidx.credentials.provider.extra.CREATE_CREDENTIAL_REQUEST_DATA"
        private const val PROVIDER_EXTRA_EXCEPTION_TYPE = "androidx.credentials.provider.extra.CREATE_CREDENTIAL_EXCEPTION_TYPE"
        private const val PROVIDER_EXTRA_EXCEPTION_MESSAGE = "androidx.credentials.provider.extra.CREATE_CREDENTIAL_EXCEPTION_MESSAGE"

        private const val PUBKEY_REQ_JSON_KEY = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
        private const val PUBKEY_RES_AUTH_JSON_KEY = "androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON"
        private const val PUBKEY_RES_REG_JSON_KEY = "androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON"
    }
}
