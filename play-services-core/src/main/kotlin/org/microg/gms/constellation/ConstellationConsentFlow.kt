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
    Log.d(TAG, "Calling GetConsent first...")

    val getConsentToken = rpc.getDroidGuardToken("getConsent", requestContext.iidToken)
    if (getConsentToken == null) {
        Log.w(TAG, "DroidGuard token for GetConsent FAILED - proceeding without DG (no-DG-first strategy)")
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
        Log.i(TAG, "GetConsent SUCCESS: consent=${consentResponse.device_consent?.consent} dg_resp=${consentResponse.droidguard_token_response != null}")

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

        Log.w(TAG, "Consent NOT established (${consentResponse.device_consent?.consent}) - calling SetConsent(CONSENTED)...")
        return trySetConsent(rpc, requestContext, initialArfbCached)
    } catch (e: Exception) {
        if (e is com.squareup.wire.GrpcException) {
            Log.e(TAG, "GetConsent gRPC error: code=${e.grpcStatus.code} name=${e.grpcStatus.name}")
            Log.e(TAG, "GetConsent gRPC message: ${e.grpcMessage}")
            val details = e.grpcStatusDetails
            if (details != null) {
                val b64 = android.util.Base64.encodeToString(details, android.util.Base64.NO_WRAP)
                Log.e(TAG, "GetConsent gRPC statusDetails (${details.size} bytes): $b64")
            } else {
                Log.e(TAG, "GetConsent gRPC statusDetails: null")
            }
            if (e.grpcStatus.code == 7 || e.grpcStatus.code == 16) {
                rpc.clearDroidGuardTokenCache(rpc.resolveDroidGuardFlow("getConsent"), "Auth error (grpc-status=${e.grpcStatus.code})")
            }
        } else {
            Log.e(TAG, "GetConsent failed: ${e.javaClass.simpleName}: ${e.message}")
            if (e.cause != null) {
                Log.e(TAG, "  Cause: ${e.cause?.javaClass?.simpleName}: ${e.cause?.message}")
            }
        }
        Log.d(TAG, "Full exception trace:", e)
        Log.w(TAG, "GetConsent failed; continuing to Sync for experiment")
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
        Log.d(TAG, "SetConsent attempt 1: WITHOUT DroidGuard (API doc: DG not required for SetConsent)")
        try {
            val noDgRequest = buildSetConsentRequest(requestContext.sessionId, requestContext.protoCtx, null)
            Log.d(TAG, "SetConsent request size (no DG): ${noDgRequest.encode().size} bytes")
            rpc.setConsent(noDgRequest)
            Log.i(TAG, "SetConsent SUCCESS (no DG)! Server accepted consent without DroidGuard.")
            setConsentSucceeded = true
        } catch (e1: Exception) {
            val code1 = if (e1 is com.squareup.wire.GrpcException) e1.grpcStatus.code else -1
            Log.w(TAG, "SetConsent without DG failed: grpc-status=$code1 ${e1.message}")
            if (code1 == 7) {
                Log.d(TAG, "SetConsent attempt 2: WITH DroidGuard token")
                val setConsentToken = rpc.getDroidGuardToken("setConsent", requestContext.iidToken)
                if (setConsentToken != null) {
                    try {
                        val dgRequest = buildSetConsentRequest(requestContext.sessionId, requestContext.protoCtx, setConsentToken)
                        Log.d(TAG, "SetConsent request size (with DG): ${dgRequest.encode().size} bytes")
                        rpc.setConsent(dgRequest)
                        Log.i(TAG, "SetConsent SUCCESS (with DG)!")
                        setConsentSucceeded = true
                    } catch (e2: Exception) {
                        Log.e(TAG, "SetConsent with DG also failed: ${e2.message}")
                        if (e2 is com.squareup.wire.GrpcException) {
                            Log.e(TAG, "  gRPC status: code=${e2.grpcStatus.code} name=${e2.grpcStatus.name} msg=${e2.grpcMessage}")
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get DroidGuard token for SetConsent retry")
                }
            } else {
                Log.e(TAG, "SetConsent failed with non-PERMISSION_DENIED error, not retrying")
                Log.d(TAG, "SetConsent exception trace:", e1)
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
            Log.w(TAG, "SetConsent failed - continuing to Sync (may fail)")
            return ConsentFlowOutcome(
                consented = false,
                setConsentAttempted = true,
                setConsentSucceeded = false,
                arfbCached = initialArfbCached,
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "SetConsent block failed: ${e.javaClass.simpleName}: ${e.message}")
        Log.d(TAG, "SetConsent block exception trace:", e)
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
    Log.d(TAG, "Retrying GetConsent to obtain ARfb token...")
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
        Log.i(TAG, "  ARfb cached from retry!")
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
            Log.i(TAG, "Cached ARfb from GetConsent: ${serverToken.length} chars, TTL=${java.util.Date(ttlMillis)}")
            return true
        } else {
            Log.w(TAG, "GetConsent DG token response present but empty")
        }
    } else {
        Log.w(TAG, "No DG token in GetConsent response - Sync will use raw DG")
    }
    return false
}
