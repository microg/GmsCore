/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.util.Log
import google.internal.communications.phonedeviceverification.v1.*
import kotlinx.coroutines.delay

private const val TAG = "GmsConstellationClient"

internal data class ProceedRequestContext(
    val context: Context,
    val rpc: ConstellationRpcClient,
    val protoCtx: RequestProtoContext,
    val gpnvRequestContext: GpnvRequestContext,
    val sessionId: String,
    val iidToken: String,
    val subId: Int,
    val phoneNumber: String,
    val deviceAndroidId: Long,
    val userAndroidId: Long,
    val proceedClientCredentials: ClientCredentials?,
    val initialVerification: Verification,
)

internal sealed class ProceedFlowOutcome {
    data class Verified(val jwt: String) : ProceedFlowOutcome()
    data class Error(val reason: String, val cause: Exception? = null) : ProceedFlowOutcome()
    data object Incomplete : ProceedFlowOutcome()
}

internal suspend fun runProceedFlow(requestContext: ProceedRequestContext): ProceedFlowOutcome {
    Log.d(TAG, "Entering challenge dispatch loop")

    var currentVerification = requestContext.initialVerification
    val carrierIdAttempts = mutableMapOf<String, Int>()
    var moSmsSent = false

    for (round in 1..16) {
        val challenge = currentVerification.pending_challenge?.challenge
        if (challenge == null) {
            Log.w(TAG, "Round $round: no challenge in pending verification")
            break
        }
        val challengeType = challenge.type
        val challengeId = challenge.challenge_id?.id ?: "unknown"
        Log.d(TAG, "Challenge round $round: type=$challengeType")

        val expiryTimeMs = challenge.expiry_time?.let { pair ->
            val serverNowMs = pair.start_time?.let { it.epochSecond * 1000L + it.nano / 1_000_000L } ?: 0L
            val serverExpiryMs = pair.end_time?.let { it.epochSecond * 1000L + it.nano / 1_000_000L } ?: 0L
            if (serverNowMs > 0 && serverExpiryMs > serverNowMs) serverExpiryMs - serverNowMs else null
        }

        val challengeResponse = buildChallengeResponse(
            requestContext = requestContext,
            challenge = challenge,
            challengeType = challengeType,
            challengeId = challengeId,
            round = round,
            expiryTimeMs = expiryTimeMs,
            carrierIdAttempts = carrierIdAttempts,
            moSmsSent = moSmsSent,
        )

        if (challengeResponse == null) {
            Log.w(TAG, "Round $round: verifier returned null, exiting loop")
            break
        }

        if (challengeType == ChallengeType.CHALLENGE_TYPE_MO_SMS && challenge.mo_challenge != null && !moSmsSent) {
            moSmsSent = true
        }

        when (val proceedOutcome = executeProceed(
            requestContext = requestContext,
            currentVerification = currentVerification,
            challengeResponse = challengeResponse,
            challengeId = challengeId,
            round = round,
        )) {
            is ProceedExecutionOutcome.Verified -> {
                Log.i(TAG, "VERIFIED after challenge round $round")
                var proceedGpnvCtx = requestContext.gpnvRequestContext
                for (gpnvAttempt in 1..2) {
                    try {
                        val verifiedToken = fetchVerifiedPhoneToken(
                            rpc = requestContext.rpc,
                            requestContext = proceedGpnvCtx,
                            targetPhone = requestContext.phoneNumber,
                            marker = "GPNV_POST_PROCEED",
                        )
                        return if (verifiedToken != null) {
                            ProceedFlowOutcome.Verified(verifiedToken.jwt)
                        } else {
                            Log.e(TAG, "VERIFIED but GPNV returned empty token")
                            ProceedFlowOutcome.Error("proceed-no-token")
                        }
                    } catch (e: Exception) {
                        val msg = e.message ?: ""
                        if (gpnvAttempt == 1 && msg.contains("could not verify iid_token")) {
                            Log.w(TAG, "Post-Proceed GPNV iid_token rejected, re-registering")
                            Log.i("MicroGRcs", "GPNV iid_token rejected, retrying with fresh token")
                            GoogleConstellationClient.invalidateIidToken(requestContext.context, ConstellationConstants.SENDER_READ_ONLY)
                            val (freshToken, freshSource) = GoogleConstellationClient.getOrRegisterIidToken(
                                requestContext.context, "com.google.android.gms", ConstellationConstants.SENDER_READ_ONLY
                            )
                            Log.i("MicroGRcs", "GPNV retry iid=$freshSource")
                            proceedGpnvCtx = proceedGpnvCtx.copy(readOnlyIidToken = freshToken)
                        } else {
                            Log.e(TAG, "Post-Proceed GPNV failed: $msg")
                            Log.i("MicroGRcs", "GPNV failed: $msg")
                            return ProceedFlowOutcome.Error("proceed-gpnv-failed")
                        }
                    }
                }
                return ProceedFlowOutcome.Error("proceed-gpnv-exhausted")
            }
            is ProceedExecutionOutcome.Pending -> {
                Log.d(TAG, "Still PENDING after round $round")
                currentVerification = proceedOutcome.verification
                if (challengeType == ChallengeType.CHALLENGE_TYPE_MO_SMS) {
                    val intervals = challenge.mo_challenge?.polling_intervals?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()
                    val pollDelay = intervals.getOrNull(round - 1) ?: 5000L
                    delay(pollDelay)
                }
            }
            is ProceedExecutionOutcome.Error -> {
                return ProceedFlowOutcome.Error(proceedOutcome.reason, proceedOutcome.cause)
            }
            is ProceedExecutionOutcome.OtherState -> {
                Log.w(TAG, "Unexpected post-Proceed state: ${proceedOutcome.state} (round $round)")
                break
            }
        }
    }

    Log.w(TAG, "Challenge loop exhausted (16 rounds) or exited early")
    return ProceedFlowOutcome.Incomplete
}

private suspend fun buildChallengeResponse(
    requestContext: ProceedRequestContext,
    challenge: Challenge,
    challengeType: ChallengeType?,
    challengeId: String,
    round: Int,
    expiryTimeMs: Long?,
    carrierIdAttempts: MutableMap<String, Int>,
    moSmsSent: Boolean,
): ChallengeResponse? {
    return when (challengeType) {
        ChallengeType.CHALLENGE_TYPE_MT_SMS -> {
            val timeoutSec = (expiryTimeMs?.div(1000) ?: 120L).coerceIn(10, 300)
            Log.d(TAG, "MT_SMS: waiting ${timeoutSec}s for OTP")
            val otpSms = SmsInbox.awaitMatch(timeoutSeconds = timeoutSec)
            if (otpSms != null) {
                Log.i(TAG, "MT_SMS OTP received")
                ChallengeResponse(
                    mt_challenge_response = MTChallengeResponse(
                        sms_body = otpSms.messageBody,
                        originating_address = otpSms.originatingAddress,
                    )
                )
            } else {
                Log.w(TAG, "MT_SMS: OTP timeout after ${timeoutSec}s")
                ChallengeResponse(
                    mt_challenge_response = MTChallengeResponse(
                        sms_body = "",
                        originating_address = "",
                    )
                )
            }
        }
        ChallengeType.CHALLENGE_TYPE_MO_SMS -> {
            val moChallenge = challenge.mo_challenge
            if (moChallenge == null) {
                Log.w(TAG, "  MO_SMS: no mo_challenge data")
                null
            } else if (!moSmsSent) {
                Log.d(TAG, "MO_SMS: sending verification SMS")
                ChallengeProcessor.sendMoSms(requestContext.context, moChallenge, requestContext.subId)
            } else {
                val pollDelays = moChallenge.polling_intervals.split(",").mapNotNull { it.trim().toLongOrNull() }
                val pollIndex = (round - 2).coerceAtLeast(0)
                val pollDelay = pollDelays.getOrElse(pollIndex) { pollDelays.lastOrNull() ?: 5000L }
                Log.d(TAG, "MO_SMS: polling (${pollDelay}ms)")
                delay(pollDelay.coerceIn(1000, 30000))
                ChallengeResponse(
                    mo_challenge_response = MOChallengeResponse(
                        status = MOChallengeStatus.MO_STATUS_COMPLETED,
                    )
                )
            }
        }
        ChallengeType.CHALLENGE_TYPE_CARRIER_ID -> {
            val carrierChallenge = challenge.carrier_id_challenge
            if (carrierChallenge == null) {
                Log.w(TAG, "  CARRIER_ID: no carrier_id_challenge data")
                null
            } else {
                val attempts = carrierIdAttempts.getOrDefault(challengeId, 0) + 1
                carrierIdAttempts[challengeId] = attempts
                if (attempts > 3) {
                    Log.w(TAG, "  CARRIER_ID: retry exceeded ($attempts) for $challengeId")
                    ChallengeResponse(
                        carrier_id_challenge_response = CarrierIDChallengeResponse(
                            carrier_id_error = CarrierIdError.CARRIER_ID_ERROR_RETRY_ATTEMPT_EXCEEDED,
                        )
                    )
                } else {
                    Log.d(TAG, "CARRIER_ID: attempt $attempts/3")
                    ChallengeProcessor.verifyCarrierId(requestContext.context, carrierChallenge, requestContext.subId)
                }
            }
        }
        ChallengeType.CHALLENGE_TYPE_REGISTERED_SMS -> {
            val regChallenge = challenge.registered_sms_challenge
            if (regChallenge == null) {
                Log.w(TAG, "  REGISTERED_SMS: no challenge data")
                null
            } else {
                Log.d(TAG, "REGISTERED_SMS: verifying against SMS inbox")
                ChallengeProcessor.verifyRegisteredSms(requestContext.context, regChallenge, requestContext.subId)
            }
        }
        ChallengeType.CHALLENGE_TYPE_FLASH_CALL -> {
            val flashChallenge = challenge.flash_call_challenge
            if (flashChallenge == null) {
                Log.w(TAG, "  FLASH_CALL: no flash_call_challenge data")
                null
            } else {
                val waitMs = expiryTimeMs ?: (flashChallenge.millis_between_interceptions.takeIf { it > 0 } ?: 30_000L)
                Log.d(TAG, "FLASH_CALL: waiting for incoming call")
                ChallengeProcessor.verifyFlashCall(requestContext.context, flashChallenge, waitMs)
            }
        }
        ChallengeType.CHALLENGE_TYPE_TS43 -> {
            val ts43Challenge = challenge.ts43_challenge
            if (ts43Challenge == null) {
                Log.w(TAG, "  TS43: no ts43_challenge data")
                null
            } else {
                Log.d(TAG, "TS43: processing entitlement challenge")
                ChallengeProcessor.handleTs43Challenge(requestContext.context, ts43Challenge, requestContext.subId, requestContext.phoneNumber)
            }
        }
        else -> {
            Log.w(TAG, "  Unsupported challenge type: $challengeType")
            null
        }
    }
}

private sealed class ProceedExecutionOutcome {
    data class Verified(val verification: Verification) : ProceedExecutionOutcome()
    data class Pending(val verification: Verification) : ProceedExecutionOutcome()
    data class OtherState(val state: VerificationState?) : ProceedExecutionOutcome()
    data class Error(val reason: String, val cause: Exception? = null) : ProceedExecutionOutcome()
}

private suspend fun executeProceed(
    requestContext: ProceedRequestContext,
    currentVerification: Verification,
    challengeResponse: ChallengeResponse,
    challengeId: String,
    round: Int,
): ProceedExecutionOutcome {
    val proceedDgToken = requestContext.rpc.getDroidGuardToken("proceed", requestContext.iidToken)
    val proceedClientInfo = buildClientInfo(
        ctx = requestContext.protoCtx,
        droidGuardToken = proceedDgToken,
    )
    val proceedHeader = buildRequestHeader(
        sessionId = requestContext.sessionId,
        clientInfo = proceedClientInfo,
        clientCredentials = requestContext.proceedClientCredentials,
    )
    val proceedRequest = ProceedRequest(
        verification = currentVerification,
        challenge_response = challengeResponse,
        header_ = proceedHeader,
    )

    Log.d(TAG, "Round $round: calling Proceed")

    val noDgRequest = proceedRequest.copy(
        header_ = proceedHeader.copy(
            client_info = proceedHeader.client_info?.copy(device_signals = DeviceSignals())
        )
    )

    val proceedResponse = try {
        val proceedNoDgResponse = requestContext.rpc.proceed(noDgRequest)
        Log.i(TAG, "Proceed succeeded (round $round)")
        proceedNoDgResponse
    } catch (e: Exception) {
        if (e is com.squareup.wire.GrpcException && proceedDgToken != null) {
            Log.w(TAG, "Proceed without DG failed, retrying with DG (round $round)")
            try {
                val proceedWithDgResponse = requestContext.rpc.proceed(proceedRequest)
                Log.i(TAG, "Proceed succeeded with DG (round $round)")
                proceedWithDgResponse
            } catch (e2: Exception) {
                Log.e(TAG, "Round $round: Proceed with DG also failed: ${e2.message}")
                return ProceedExecutionOutcome.Error("proceed-failed-round-$round", e2)
            }
        } else {
            Log.e(TAG, "Round $round: Proceed failed: ${e.message}")
            return ProceedExecutionOutcome.Error("proceed-failed-round-$round", e)
        }
    }

    proceedResponse.droidguard_token_response?.let { dgResp ->
        if (!dgResp.droidguard_token.isNullOrEmpty()) {
            requestContext.rpc.cacheDroidGuardToken(
                requestContext.rpc.resolveDroidGuardFlow("proceed"),
                dgResp.droidguard_token,
                dgResp.droidguard_token_ttl?.toEpochMilli() ?: 0L,
                requestContext.iidToken,
            )
        }
    }

    val newVerification = proceedResponse.verification
    val newState = newVerification?.state
    Log.d(TAG, "Post-Proceed state=$newState")

    return when (newState) {
        VerificationState.VERIFICATION_STATE_VERIFIED -> ProceedExecutionOutcome.Verified(newVerification)
        VerificationState.VERIFICATION_STATE_PENDING -> ProceedExecutionOutcome.Pending(newVerification)
        else -> ProceedExecutionOutcome.OtherState(newState)
    }
}
