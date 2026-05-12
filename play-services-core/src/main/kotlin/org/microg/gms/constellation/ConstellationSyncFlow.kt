/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import google.internal.communications.phonedeviceverification.v1.SyncRequest
import google.internal.communications.phonedeviceverification.v1.SyncResponse
import google.internal.communications.phonedeviceverification.v1.Verification
import google.internal.communications.phonedeviceverification.v1.VerificationState
import google.internal.communications.phonedeviceverification.v1.PublicKeyStatus

private const val TAG = "GmsConstellationClient"
private const val MAX_SYNC_ATTEMPTS = 3
private const val SYNC_RETRY_DELAY_MS = 45_000L

internal data class SyncRequestContext(
    val context: Context,
    val keyPrefs: SharedPreferences,
    val initialRequest: SyncRequest,
    val iidToken: String,
    val imsi: String,
    val phoneNumber: String,
)

internal data class SyncFlowOutcome(
    val response: SyncResponse,
    val hasVerified: Boolean,
    val noneReason: Int?,
    val pendingVerification: Verification?,
)

internal suspend fun runSyncFlow(
    rpc: ConstellationRpcClient,
    requestContext: SyncRequestContext,
): SyncFlowOutcome {
    val response = executeSyncWithRetry(rpc, requestContext)
    persistSyncArtifacts(requestContext.context, requestContext.keyPrefs, rpc, requestContext.iidToken, response)
    return analyzeSyncResponse(response, requestContext.phoneNumber, requestContext.imsi)
}

private suspend fun executeSyncWithRetry(
    rpc: ConstellationRpcClient,
    requestContext: SyncRequestContext,
): SyncResponse {
    var currentSyncRequest = requestContext.initialRequest
    var syncRetryResponse: SyncResponse? = null

    for (syncAttempt in 1..MAX_SYNC_ATTEMPTS) {
        Log.d(TAG, "Sending Sync request (attempt $syncAttempt/$MAX_SYNC_ATTEMPTS)...")

        try {
            syncRetryResponse = rpc.sync(currentSyncRequest)
            Log.i(TAG, "Sync succeeded")
            break
        } catch (retryEx: Exception) {
            val isPermissionDenied = retryEx is com.squareup.wire.GrpcException && retryEx.grpcStatus.code == 7

            if (isPermissionDenied && syncAttempt == 1) {
                Log.w(TAG, "Sync PERMISSION_DENIED, retrying without DG")
                rpc.clearDroidGuardTokenCache(rpc.resolveDroidGuardFlow("sync"), "Sync PERMISSION_DENIED")
                val noDgRequest = currentSyncRequest.copy(
                    header_ = currentSyncRequest.header_?.copy(
                        client_info = currentSyncRequest.header_?.client_info?.copy(
                            device_signals = null
                        )
                    )
                )
                try {
                    syncRetryResponse = rpc.sync(noDgRequest)
                    Log.i(TAG, "Sync succeeded without DG")
                    break
                } catch (noDgEx: Exception) {
                    Log.e(TAG, "Sync no-DG fallback also failed: ${noDgEx.message}")
                    throw retryEx
                }
            }

            throw retryEx
        }
    }

    return syncRetryResponse ?: throw Exception("Sync failed: no response after $MAX_SYNC_ATTEMPTS attempts")
}

private fun persistSyncArtifacts(
    context: Context,
    keyPrefs: SharedPreferences,
    rpc: ConstellationRpcClient,
    iidToken: String,
    response: SyncResponse,
) {
    Log.d(TAG, "Sync response: ${response.responses.size} verifications")

    val publicKeyStatus = response.header_?.client_info_update?.public_key_status
    if (publicKeyStatus == PublicKeyStatus.CLIENT_KEY_UPDATED) {
        Log.i(TAG, "Public key acknowledged by server")
        keyPrefs.edit().putBoolean("is_public_key_acked", true).apply()
    } else {}

    val responseVerificationTokens = response.verification_tokens
    if (responseVerificationTokens.isNotEmpty()) {
        try {
            val tokenBytes = responseVerificationTokens.map { android.util.Base64.encodeToString(it.encode(), android.util.Base64.NO_WRAP) }
            keyPrefs.edit().putStringSet("verification_tokens", tokenBytes.toSet()).apply()
            Log.d(TAG, "Stored ${responseVerificationTokens.size} verification tokens")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to store verification_tokens: ${e.message}")
        }
    }

    val syncDgTokenResponse = response.droidguard_token_response
    if (syncDgTokenResponse != null) {
        val serverToken = syncDgTokenResponse.droidguard_token
        val serverTtl = syncDgTokenResponse.droidguard_token_ttl
        if (!serverToken.isNullOrEmpty()) {
            val ttlMillis = serverTtl?.toEpochMilli() ?: 0L
            rpc.cacheDroidGuardToken(rpc.resolveDroidGuardFlow("sync"), serverToken, ttlMillis, iidToken)
            Log.d(TAG, "Cached DroidGuard token from SyncResponse")
        }
    }
}

private fun analyzeSyncResponse(
    response: SyncResponse,
    phoneNumber: String,
    imsi: String,
): SyncFlowOutcome {
    val responses = response.responses
    if (responses.isEmpty()) {
        Log.w(TAG, "No verification responses in SyncResponse")
        throw SyncNoResponsesException()
    }

    var hasVerified = false
    var pendingVerification: Verification? = null
    var noneReason: Int? = null

    for (verificationResponse in responses) {
        val responseVerification = verificationResponse.verification
        val state = responseVerification?.state
        val responseSimInfo = responseVerification?.association?.sim?.sim_info
        val responseImsi = responseSimInfo?.imsi?.firstOrNull() ?: ""
        val responseMsisdn = responseSimInfo?.sim_readable_number ?: ""


        when (state) {
            VerificationState.VERIFICATION_STATE_VERIFIED -> {
                Log.i(TAG, "Verification state: VERIFIED")
                Log.i("MicroGRcs", "constellation sync result=VERIFIED reason=0")
                hasVerified = true
            }
            VerificationState.VERIFICATION_STATE_PENDING -> {
                Log.i(TAG, "Verification state: PENDING")
                Log.i("MicroGRcs", "constellation sync result=PENDING reason=0")
            }
            VerificationState.VERIFICATION_STATE_NONE -> {
                Log.w(TAG, "Verification state: NONE")
            }
            else -> {
                Log.w(TAG, "Unexpected state: $state")
            }
        }
    }

    if (!hasVerified) {
        pendingVerification = selectPendingVerification(responses, phoneNumber)
        if (pendingVerification == null) {
            noneReason = responses.firstOrNull {
                it.verification?.state == VerificationState.VERIFICATION_STATE_NONE
            }?.verification?.unverified_info?.reason_enum_1 ?: if (responses.any { it.verification?.state == VerificationState.VERIFICATION_STATE_NONE }) 0 else null
            if (noneReason != null) {
                Log.i(TAG, "NONE state reason=$noneReason")
                Log.i("MicroGRcs", "constellation sync result=NONE reason=$noneReason")
            }
        }
    }

    return SyncFlowOutcome(
        response = response,
        hasVerified = hasVerified,
        noneReason = noneReason,
        pendingVerification = pendingVerification,
    )
}

private fun selectPendingVerification(
    responses: List<google.internal.communications.phonedeviceverification.v1.VerificationResponse>,
    phoneNumber: String,
): Verification? {
    val pendingResponses = responses.filter {
        it.verification?.state == VerificationState.VERIFICATION_STATE_PENDING
    }
    if (pendingResponses.isEmpty()) return null

    return if (phoneNumber.isNotEmpty()) {
        pendingResponses.firstOrNull {
            it.verification?.association?.sim?.sim_info?.sim_readable_number == phoneNumber
        }?.verification ?: pendingResponses.firstOrNull()?.verification
    } else {
        pendingResponses.firstOrNull()?.verification
    }
}

internal class SyncNoResponsesException : IllegalStateException("sync-no-responses")
