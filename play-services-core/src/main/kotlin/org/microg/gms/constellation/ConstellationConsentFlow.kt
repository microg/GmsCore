/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.util.Log
import google.internal.communications.phonedeviceverification.v1.ConsentValue
import google.internal.communications.phonedeviceverification.v1.Param
import google.internal.communications.phonedeviceverification.v1.StringId

private const val TAG = "GmsConstellationClient"

internal data class ConsentRequestContext(
    val sessionId: String,
    val protoCtx: RequestProtoContext,
    val registeredAppIds: List<StringId>,
    val params: List<Param>,
    val iidToken: String,
)

internal data class ConsentFlowOutcome(
    val consented: Boolean,
    val setConsentAttempted: Boolean,
    val setConsentSucceeded: Boolean,
    val arfbCached: Boolean,
)

internal suspend fun runConsentFlow(
    rpc: ConstellationRpcClient,
    requestContext: ConsentRequestContext,
): ConsentFlowOutcome {
    Log.d(TAG, "Calling GetConsent")

    val getConsentToken = rpc.getDroidGuardToken("getConsent", requestContext.iidToken)
    if (getConsentToken == null) {
        Log.w(TAG, "DroidGuard token for GetConsent failed, proceeding without DG")
    }

    val consentRequest = buildGetConsentRequest(
        sessionId = requestContext.sessionId,
        ctx = requestContext.protoCtx,
        getConsentToken = getConsentToken,
        registeredAppIds = requestContext.registeredAppIds,
        params = requestContext.params,
    )

    try {
        val consentResponse = rpc.getConsent(consentRequest)
        Log.i(TAG, "GetConsent succeeded, consent=${consentResponse.device_consent?.consent}")

        val initialArfbCached = cacheConsentTokenResponse(rpc, requestContext.iidToken, consentResponse.droidguard_token_response)

        if (consentResponse.device_consent?.consent == ConsentValue.CONSENT_VALUE_CONSENTED) {
            Log.i(TAG, "Consent already established")
            return ConsentFlowOutcome(
                consented = true,
                setConsentAttempted = false,
                setConsentSucceeded = false,
                arfbCached = initialArfbCached,
            )
        }

        Log.w(TAG, "Consent not established, calling SetConsent")
        return trySetConsent(rpc, requestContext, initialArfbCached)
    } catch (e: Exception) {
        if (e is com.squareup.wire.GrpcException) {
            Log.e(TAG, "GetConsent gRPC error: code=${e.grpcStatus.code} message=${e.grpcMessage}")
            if (e.grpcStatus.code == 7 || e.grpcStatus.code == 16) {
                rpc.clearDroidGuardTokenCache(rpc.resolveDroidGuardFlow("getConsent"), "Auth error (grpc-status=${e.grpcStatus.code})")
            }
        } else {
            Log.e(TAG, "GetConsent failed: ${e.javaClass.simpleName}: ${e.message}")
        }
        Log.w(TAG, "GetConsent failed, continuing to Sync")
        return ConsentFlowOutcome(
            consented = false,
            setConsentAttempted = false,
            setConsentSucceeded = false,
            arfbCached = false,
        )
    }
}

private suspend fun trySetConsent(
    rpc: ConstellationRpcClient,
    requestContext: ConsentRequestContext,
    initialArfbCached: Boolean,
): ConsentFlowOutcome {
    try {
        var setConsentSucceeded = false
        Log.d(TAG, "SetConsent without DG")
        try {
            val noDgRequest = buildSetConsentRequest(requestContext.sessionId, requestContext.protoCtx, null)
            rpc.setConsent(noDgRequest)
            Log.i(TAG, "SetConsent succeeded (no DG)")
            setConsentSucceeded = true
        } catch (e1: Exception) {
            val code1 = if (e1 is com.squareup.wire.GrpcException) e1.grpcStatus.code else -1
            Log.w(TAG, "SetConsent without DG failed (grpc-status=$code1)")
            if (code1 == 7) {
                Log.d(TAG, "Retrying SetConsent with DG")
                val setConsentToken = rpc.getDroidGuardToken("setConsent", requestContext.iidToken)
                if (setConsentToken != null) {
                    try {
                        val dgRequest = buildSetConsentRequest(requestContext.sessionId, requestContext.protoCtx, setConsentToken)
                        rpc.setConsent(dgRequest)
                        Log.i(TAG, "SetConsent succeeded (with DG)")
                        setConsentSucceeded = true
                    } catch (e2: Exception) {
                        Log.e(TAG, "SetConsent with DG also failed: ${e2.message}")
                    }
                } else {
                    Log.e(TAG, "Failed to get DroidGuard token for SetConsent retry")
                }
            } else {
                Log.e(TAG, "SetConsent failed with non-PERMISSION_DENIED error, not retrying")
            }
        }

        if (setConsentSucceeded) {
            val retryArfbCached = retryGetConsentAfterSetConsent(rpc, requestContext)
            return ConsentFlowOutcome(
                consented = true,
                setConsentAttempted = true,
                setConsentSucceeded = true,
                arfbCached = initialArfbCached || retryArfbCached,
            )
        } else {
            Log.w(TAG, "SetConsent failed, continuing to Sync")
            return ConsentFlowOutcome(
                consented = false,
                setConsentAttempted = true,
                setConsentSucceeded = false,
                arfbCached = initialArfbCached,
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "SetConsent failed: ${e.javaClass.simpleName}: ${e.message}")
        Log.w(TAG, "Continuing to Sync despite SetConsent failure")
        return ConsentFlowOutcome(
            consented = false,
            setConsentAttempted = true,
            setConsentSucceeded = false,
            arfbCached = initialArfbCached,
        )
    }
}

private suspend fun retryGetConsentAfterSetConsent(
    rpc: ConstellationRpcClient,
    requestContext: ConsentRequestContext,
): Boolean {
    Log.d(TAG, "Retrying GetConsent after SetConsent")
    val retryToken = rpc.getDroidGuardToken("getConsent", requestContext.iidToken)
    if (retryToken == null) {
        Log.w(TAG, "Retry GetConsent skipped: no DG token available")
        return false
    }

    val retryRequest = buildGetConsentRequest(
        sessionId = requestContext.sessionId,
        ctx = requestContext.protoCtx,
        getConsentToken = retryToken,
        registeredAppIds = requestContext.registeredAppIds,
        params = requestContext.params,
    )
    val retryResponse = rpc.getConsent(retryRequest)
    Log.i(TAG, "GetConsent retry: consent=${retryResponse.device_consent?.consent}")

    val retryCached = cacheConsentTokenResponse(rpc, requestContext.iidToken, retryResponse.droidguard_token_response)
    if (retryCached) {
        Log.i(TAG, "ARfb cached from retry")
    }
    return retryCached
}

private fun cacheConsentTokenResponse(
    rpc: ConstellationRpcClient,
    iidToken: String,
    dgTokenResponse: google.internal.communications.phonedeviceverification.v1.DroidGuardTokenResponse?,
): Boolean {
    if (dgTokenResponse != null) {
        val serverToken = dgTokenResponse.droidguard_token
        if (!serverToken.isNullOrEmpty()) {
            val ttlMillis = try { dgTokenResponse.droidguard_token_ttl?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L }
            rpc.cacheDroidGuardToken(rpc.resolveDroidGuardFlow("getConsent"), serverToken, ttlMillis, iidToken)
            Log.i(TAG, "Cached ARfb from GetConsent response")
            return true
        } else {
            Log.w(TAG, "GetConsent DG token response present but empty")
        }
    } else {
        Log.w(TAG, "No DG token in GetConsent response - Sync will use raw DG")
    }
    return false
}
