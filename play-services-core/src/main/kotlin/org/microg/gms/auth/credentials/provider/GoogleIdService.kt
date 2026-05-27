/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.accounts.AccountManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialOption
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.CustomCredentialEntry
import com.google.android.gms.R
import org.microg.gms.auth.credentials.provider.GoogleIdRequestParams.Companion.toGoogleIdRequestParams
import org.microg.gms.auth.AuthConstants

private const val TAG = "GoogleIdService"

/**
 * Google ID Credential Provider Service
 * Handles Google Sign-In and Google ID Token credentials
 * Note: This service only handles GET operations (sign-in), not CREATE operations
 */
@RequiresApi(34)
open class GoogleIdService : CredentialProviderService() {

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        runCatching {
            val credentialEntries = request.beginGetCredentialOptions
                .flatMap { option ->
                    when (option.type) {
                        TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> handleGoogleIdTokenRequest(option, request)
                        else -> emptyList<CredentialEntry>().also {
                            Log.d(TAG, "Unsupported credential type: ${option.type}")
                        }
                    }
                }

            callback.onResult(BeginGetCredentialResponse.Builder()
                .setCredentialEntries(credentialEntries)
                .build()
                .also { Log.d(TAG, "Returning ${credentialEntries.size} credential entries") })

        }.onFailure { e ->
            Log.e(TAG, "Error in onBeginGetCredential", e)
            callback.onError(GetCredentialUnknownException(e.message))
        }
    }

    private fun handleGoogleIdTokenRequest(
        option: BeginGetCredentialOption,
        request: BeginGetCredentialRequest
    ): List<CredentialEntry> = option.candidateQueryData.toGoogleIdRequestParams().let { params ->
        val callingPackage = request.callingAppInfo?.packageName.orEmpty()
        val accounts = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            listOf(createCredentialEntry(
                option = option,
                params = params,
                callingPackage = callingPackage,
                title = getString(R.string.credentials_service_sign_in_with_google_label),
                requestCode = TAG.hashCode(),
            ))
        } else {
            accounts.map { account ->
                createCredentialEntry(
                    option = option,
                    params = params,
                    callingPackage = callingPackage,
                    accountName = account.name,
                    title = account.name,
                    requestCode = account.name.hashCode()
                )
            }
        }
    }

    private fun createCredentialEntry(
        option: BeginGetCredentialOption,
        params: GoogleIdRequestParams,
        callingPackage: String,
        accountName: String? = null,
        title: String,
        requestCode: Int
    ): CredentialEntry {
        val intent = createGoogleIdIntent(params, callingPackage, accountName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return CustomCredentialEntry(
            this,
            title,
            pendingIntent,
            option,
            accountName?.takeIf { it != title },
            null,
            null,
            Icon.createWithResource(this, R.drawable.ic_google_logo)
        )
    }

    private fun createGoogleIdIntent(
        params: GoogleIdRequestParams,
        callingPackage: String,
        accountName: String?
    ): Intent = Intent(this, SignInProxyActivity::class.java).apply {
        accountName?.let { putExtra(GOOGLE_ID_SIWG_ACCOUNT_NAME, it) }
        putExtra(GOOGLE_ID_SIWG_SERVER_CLIENT_ID, params.serverClientId ?: "")
        putExtra(GOOGLE_ID_SIWG_NONCE, params.nonce)
        putExtra(GOOGLE_ID_SIWG_CALLER_PACKAGE, callingPackage)
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        // Google ID Service only handles GET (sign-in), not CREATE
        callback.onResult(BeginCreateCredentialResponse.Builder().build())
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        callback.onResult(null)
    }
}

const val GOOGLE_ID_SUBTYPE = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_GOOGLE_ID_TOKEN_SUBTYPE"
const val GOOGLE_ID_TYPE_SIWG = "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL"
const val GOOGLE_ID_SERVER_CLIENT_ID = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_SERVER_CLIENT_ID"
const val GOOGLE_ID_NONCE = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_NONCE"
const val GOOGLE_ID_LINKED_SERVICE_ID = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_LINKED_SERVICE_ID"
const val GOOGLE_ID_REQUEST_VERIFIED_PHONE = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_REQUEST_VERIFIED_PHONE_NUMBER"
const val GOOGLE_ID_SIWG_CALLER_PACKAGE = "com.google.android.libraries.identity.googleid.siwg.BUNDLE_KEY_CALLER_PACKAGE"
const val GOOGLE_ID_SIWG_ACCOUNT_NAME = "com.google.android.libraries.identity.googleid.siwg.BUNDLE_KEY_ACCOUNT_NAME"
const val GOOGLE_ID_SIWG_SERVER_CLIENT_ID = "com.google.android.libraries.identity.googleid.siwg.BUNDLE_KEY_SERVER_CLIENT_ID"
const val GOOGLE_ID_SIWG_NONCE = "com.google.android.libraries.identity.googleid.siwg.BUNDLE_KEY_NONCE"
const val GOOGLE_ID_SIWG_HOSTED_DOMAIN = "com.google.android.libraries.identity.googleid.siwg.BUNDLE_KEY_HOSTED_DOMAIN_FILTER"
const val GOOGLE_ID_ANDROIDX_AUTO_SELECT = "androidx.credentials.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED"
const val GOOGLE_ID_BUNDLE_KEY_ID = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID"
const val GOOGLE_ID_BUNDLE_KEY_ID_TOKEN = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN"
const val GOOGLE_ID_BUNDLE_KEY_DISPLAY_NAME = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME"
const val GOOGLE_ID_BUNDLE_KEY_PROFILE_PICTURE_URI = "com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI"

// Credential types
const val TYPE_GOOGLE_ID_TOKEN_CREDENTIAL = "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL"

data class GoogleIdRequestParams(
    val isSignInWithGoogle: Boolean,
    val serverClientId: String?,
    val nonce: String?,
    val hostedDomainFilter: String?,
    val isAutoSelectAllowed: Boolean = false,
    val filterByAuthorized: Boolean = false,
    val linkedServiceId: String?,
    val requestVerifiedPhoneNumber: Boolean = false
) {
    companion object {
        fun Bundle.toGoogleIdRequestParams(): GoogleIdRequestParams {
            val isSignInWithGoogle = getString(GOOGLE_ID_SUBTYPE) == GOOGLE_ID_TYPE_SIWG
            return GoogleIdRequestParams(
                isSignInWithGoogle = isSignInWithGoogle,
                serverClientId = getString(if (isSignInWithGoogle) GOOGLE_ID_SIWG_SERVER_CLIENT_ID else GOOGLE_ID_SERVER_CLIENT_ID),
                nonce = getString(if (isSignInWithGoogle) GOOGLE_ID_SIWG_NONCE else GOOGLE_ID_NONCE),
                hostedDomainFilter = getString(GOOGLE_ID_SIWG_HOSTED_DOMAIN),
                isAutoSelectAllowed = getBoolean(GOOGLE_ID_ANDROIDX_AUTO_SELECT, false),
                linkedServiceId = getString(GOOGLE_ID_LINKED_SERVICE_ID),
                requestVerifiedPhoneNumber = getBoolean(GOOGLE_ID_REQUEST_VERIFIED_PHONE, false)
            )
        }
    }
}
