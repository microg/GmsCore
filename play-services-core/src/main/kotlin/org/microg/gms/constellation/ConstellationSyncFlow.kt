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
import java.io.File
import kotlinx.coroutines.delay

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
        val requestByteArray = SyncRequest.ADAPTER.encode(currentSyncRequest)
        Log.d(TAG, "SyncRequest size: ${requestByteArray.size} bytes")

        try {
            syncRetryResponse = rpc.sync(currentSyncRequest)
            Log.i(TAG, "Sync SUCCESS on attempt $syncAttempt/$MAX_SYNC_ATTEMPTS")
            break
        } catch (retryEx: Exception) {
            val isPermissionDenied = retryEx is com.squareup.wire.GrpcException && retryEx.grpcStatus.code == 7

            if (isPermissionDenied && syncAttempt < MAX_SYNC_ATTEMPTS) {
                if (syncAttempt == 1) {
                    Log.w(TAG, "Sync PERMISSION_DENIED (attempt 1/$MAX_SYNC_ATTEMPTS), retrying WITHOUT DG...")
                    currentSyncRequest = rebuildSyncRequestWithDg(currentSyncRequest, null)
                    continue
                }

                Log.w(TAG, "Sync PERMISSION_DENIED (attempt $syncAttempt/$MAX_SYNC_ATTEMPTS), retrying with fresh DG in ${SYNC_RETRY_DELAY_MS / 1000}s...")
                rpc.clearDroidGuardTokenCache(rpc.resolveDroidGuardFlow("sync"), "Sync PERMISSION_DENIED retry $syncAttempt")
                requestContext.keyPrefs.edit().putBoolean("is_public_key_acked", false).apply()

                delay(SYNC_RETRY_DELAY_MS)

                val freshSyncToken = rpc.getDroidGuardToken("sync", requestContext.iidToken)
                if (freshSyncToken == null) {
                    Log.e(TAG, "Failed to get fresh DroidGuard token for Sync retry")
                    throw retryEx
                }
                Log.d(TAG, "Got fresh DG token for retry (${freshSyncToken.length} chars)")
                currentSyncRequest = rebuildSyncRequestWithDg(currentSyncRequest, freshSyncToken)
                continue
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
    try {
        val respStr = response.toString()
        val dir = File(context.filesDir, "constellation_logs")
        dir.mkdirs()
        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val respFile = File(dir, "SyncResponse_${ts}.txt")
        respFile.writeText(respStr)
        val verStates = response.responses.map { it.verification?.state?.name ?: "null" }
        Log.d(TAG, "Received Sync response: ${response.responses.size} verifications, states=$verStates, next_sync=${response.next_sync_time?.timestamp}, file=${respFile.absolutePath}")
    } catch (e: Exception) {
        Log.d(TAG, "Received Sync response: ${response.responses.size} verifications (file write failed: ${e.message})")
    }

    val publicKeyStatus = response.header_?.client_info_update?.public_key_status
    if (publicKeyStatus == PublicKeyStatus.CLIENT_KEY_UPDATED) {
        Log.i(TAG, "Public key acknowledged by server (status=$publicKeyStatus)")
        keyPrefs.edit().putBoolean("is_public_key_acked", true).apply()
    } else if (publicKeyStatus == PublicKeyStatus.PUBLIC_KEY_STATUS_NO_STATUS) {
        Log.d(TAG, "No public key status update from server")
    } else {
        Log.d(TAG, "Public key status: $publicKeyStatus")
    }

    val responseVerificationTokens = response.verification_tokens
    if (responseVerificationTokens.isNotEmpty()) {
        try {
            val tokenBytes = responseVerificationTokens.map { android.util.Base64.encodeToString(it.encode(), android.util.Base64.NO_WRAP) }
            keyPrefs.edit().putStringSet("verification_tokens", tokenBytes.toSet()).apply()
            Log.i(TAG, "Stored ${responseVerificationTokens.size} verification_tokens from SyncResponse")
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
            Log.i(TAG, "Cached DroidGuard token from SyncResponse (${serverToken.length} chars, TTL: ${java.util.Date(ttlMillis)})")
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

    Log.d(TAG, "Processing ${responses.size} verification responses")
    var hasVerified = false
    var pendingVerification: Verification? = null
    var noneReason: Int? = null

    for (verificationResponse in responses) {
        val responseVerification = verificationResponse.verification
        val state = responseVerification?.state
        val responseSimInfo = responseVerification?.association?.sim?.sim_info
        val responseImsi = responseSimInfo?.imsi?.firstOrNull() ?: ""
        val responseMsisdn = responseSimInfo?.sim_readable_number ?: ""

        Log.d(TAG, "VerificationResponse: state=$state, imsi=${if (responseImsi.isNotEmpty()) "***${responseImsi.takeLast(4)}" else "empty"}, msisdn=${if (responseMsisdn.isNotEmpty()) "***${responseMsisdn.takeLast(4)}" else "empty"}")

        when (state) {
            VerificationState.VERIFICATION_STATE_VERIFIED -> {
                Log.i(TAG, "Phone number VERIFIED!")
                Log.i("MicroGRcs", "constellation sync result=VERIFIED reason=0")
                hasVerified = true
            }
            VerificationState.VERIFICATION_STATE_PENDING -> {
                Log.w(TAG, "PENDING state - requires Proceed RPC with OTP")
                Log.i("MicroGRcs", "constellation sync result=PENDING reason=0")
            }
            VerificationState.VERIFICATION_STATE_NONE -> {
                Log.w(TAG, "NONE state - no verification exists for this SIM. IMSI was: ${if (imsi.isNotEmpty()) "present" else "EMPTY (likely cause)"}")
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
                Log.i(TAG, "NONE state UnverifiedInfo: reason=$noneReason")
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
