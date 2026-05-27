/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.CustomCredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import androidx.credentials.provider.RemoteEntry
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.squareup.wire.Instant
import org.json.JSONObject

private const val TAG = "RemoteCredentialService"

/**
 * RemoteService - Provides cross-device passkey functionality
 *
 * RemoteChimeraService corresponding to GMS, realizing cross-device authentication function:
 * - Connect other devices via QR code or Bluetooth
 * - Authenticate with a passkey on another device
 * - Create passkeys on other devices
 */
@RequiresApi(34)
open class RemoteService : CredentialProviderService() {

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        runCatching {
            val option = request.beginGetCredentialOptions.firstOrNull { it.type == TYPE_PUBLIC_KEY_CREDENTIAL }
                    as? BeginGetPublicKeyCredentialOption?
                ?: return callback.onError(NoCredentialException())

            // TODO: Don't offer when allowedCredentials set and all of them local

            val responseBuilder = BeginGetCredentialResponse.Builder()
            val pendingIntent = createPendingIntent()

            responseBuilder.setRemoteEntry(RemoteEntry(pendingIntent))

            callback.onResult(responseBuilder.build())
        }.onFailure { e ->
            Log.e(TAG, "Error in onBeginGetCredential", e)
            callback.onError(GetCredentialUnknownException(e.message))
        }
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        runCatching {
            if (request.type != TYPE_PUBLIC_KEY_CREDENTIAL) {
                return callback.onError(CreateCredentialNoCreateOptionException())
                    .also { Log.d(TAG, "Not a PublicKeyCredential create request") }
            }

            val publicKeyRequest = request as BeginCreatePublicKeyCredentialRequest
            val options = JSONObject(publicKeyRequest.requestJson).parsePublicKeyCredentialCreationOptions()

            if (options.authenticatorSelection?.attachment == Attachment.PLATFORM) {
                return callback.onError(CreateCredentialNoCreateOptionException())
                    .also { Log.d(TAG, "Platform attachment required, remote not supported") }
            }

            val responseBuilder = BeginCreateCredentialResponse.Builder()
            val pendingIntent = createPendingIntent()

            responseBuilder.setRemoteEntry(RemoteEntry(pendingIntent))

            callback.onResult(responseBuilder.build())

        }.onFailure { e ->
            Log.e(TAG, "Error in onBeginCreateCredential", e)
            callback.onError(CreateCredentialUnknownException(e.message))
        }
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        Log.d(TAG, "onClearCredentialState: No-op for remote service")
        callback.onResult(null)
    }

    private fun createPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            TAG.hashCode(),
            Intent(this, PublicKeyProxyActivity::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        fun hasPermissionForRemoteEntry(context: Context): Boolean {
            return context.checkSelfPermission("android.permission.PROVIDE_DEFAULT_ENABLED_CREDENTIAL_SERVICE") == PackageManager.PERMISSION_GRANTED
        }
    }
}
