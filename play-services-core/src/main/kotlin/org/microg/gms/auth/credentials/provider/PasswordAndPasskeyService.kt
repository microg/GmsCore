/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.PasswordCredential.Companion.TYPE_PASSWORD_CREDENTIAL
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CustomCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.squareup.wire.Instant
import org.json.JSONObject
import org.microg.gms.fido.core.CredentialUserInfo
import org.microg.gms.fido.core.Database
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.ui.AuthenticatorActivity.Companion.KEY_CREDENTIAL_ID

private const val TAG = "PasswordAndPasskey"

/**
 * Password and Passkey Credential Provider Service
 * Handles both password and passkey (FIDO2/WebAuthn) credentials
 */
@RequiresApi(34)
open class PasswordAndPasskeyService : CredentialProviderService() {

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        runCatching {
            val credentialEntries = request.beginGetCredentialOptions
                .flatMap { option ->
                    when (option.type) {
                        TYPE_PUBLIC_KEY_CREDENTIAL -> handlePublicKeyCredentialRequest(option as BeginGetPublicKeyCredentialOption, request)
                        TYPE_PASSWORD_CREDENTIAL -> emptyList() // TODO: handle password credential request
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

    private fun handlePublicKeyCredentialRequest(
        option: BeginGetPublicKeyCredentialOption,
        request: BeginGetCredentialRequest
    ): List<CredentialEntry> = runCatching {
        val options = JSONObject(option.requestJson).parsePublicKeyCredentialRequestOptions()

        var entries = Database(this).getKnownRegistrationInfo(options.rpId)
            .filter { it.transport == Transport.SCREEN_LOCK } // TODO: Also show known remote credentials?
            .filter { info -> options.allowList.isNullOrEmpty() || options.allowList!!.any { it.id.encodeBase64Url() == info.credential } }
            .also { Log.d(TAG, "Found ${it.size} credentials for rpId: ${options.rpId}") }
            .mapNotNull { credentialInfo ->
                createSignInCredentialEntry(credentialInfo, option, !options.allowList.isNullOrEmpty())
            }

        if (!RemoteService.hasPermissionForRemoteEntry(this)) {
            entries += CustomCredentialEntry(
                context = this,
                title = getString(R.string.fido_transport_selection_hybrid),
                pendingIntent = getPendingIntent(0),
                beginGetCredentialOption = option,
                subtitle = getString(com.google.android.gms.R.string.credentials_service_remote_custom_subtitle),
                lastUsedTime = Instant.ofEpochMilli(0)
            )
        }

        entries
    }.getOrElse { e ->
        Log.e(TAG, "Error handling public key credential request", e)
        emptyList()
    }

    private fun createSignInCredentialEntry(
        credentialInfo: CredentialUserInfo,
        option: BeginGetPublicKeyCredentialOption,
        isAutoSelectAllowed: Boolean = false
    ): CredentialEntry? = runCatching {
        val user = PublicKeyCredentialUserEntity.parseJson(credentialInfo.userJson)

        val pendingIntent = getPendingIntent(credentialInfo.credential.hashCode(), credentialInfo.credential)

        PublicKeyCredentialEntry(
            context = this,
            username = user.name,
            pendingIntent = pendingIntent,
            beginGetPublicKeyCredentialOption = option,
            displayName = user.displayName,
            lastUsedTime = Instant.ofEpochMilli(credentialInfo.timestamp),
            isAutoSelectAllowed = isAutoSelectAllowed
        )
    }.getOrElse { e ->
        Log.e(TAG, "Error parsing credential user info", e)
        null
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        runCatching {
            when (request.type) {
                TYPE_PUBLIC_KEY_CREDENTIAL -> handleCreatePublicKeyCredential(request as BeginCreatePublicKeyCredentialRequest, callback)
                TYPE_PASSWORD_CREDENTIAL -> error("Password credential creation not supported")
                else -> callback.onError(CreateCredentialUnknownException())
                    .also { Log.w(TAG, "Unsupported credential type: ${request.type}") }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in onBeginCreateCredential", e)
            callback.onError(CreateCredentialUnknownException(e.message))
        }
    }

    @SuppressLint("MutableImplicitPendingIntent")
    private fun handleCreatePublicKeyCredential(
        request: BeginCreatePublicKeyCredentialRequest,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) = runCatching {
        val options = JSONObject(request.requestJson).parsePublicKeyCredentialCreationOptions()

        val pendingIntent = getPendingIntent(request.requestJson.hashCode())

        callback.onResult(BeginCreateCredentialResponse.Builder()
            .addCreateEntry(CreateEntry(options.user.name, pendingIntent))
            .build()
            .also { Log.d(TAG, "Returning create credential response for passkey") })

    }.onFailure { e ->
        Log.e(TAG, "Error creating public key credential entry", e)
        callback.onError(CreateCredentialUnknownException(e.message))
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        callback.onResult(null)
    }

    private fun getPendingIntent(requestCode: Int, credentialIdString: String? = null) = PendingIntent.getActivity(
        this,
        requestCode,
        Intent(this, PublicKeyProxyActivity::class.java).apply {
            if (credentialIdString != null) putExtra(KEY_CREDENTIAL_ID, credentialIdString)
        },
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
