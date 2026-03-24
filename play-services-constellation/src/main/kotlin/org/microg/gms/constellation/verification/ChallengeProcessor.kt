package org.microg.gms.constellation.verification

import android.content.Context
import android.telephony.SubscriptionInfo
import android.util.Log
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import org.microg.gms.constellation.ConstellationStateStore
import org.microg.gms.constellation.RpcClient
import org.microg.gms.constellation.getState
import org.microg.gms.constellation.proto.ChallengeResponse
import org.microg.gms.constellation.proto.ProceedRequest
import org.microg.gms.constellation.proto.RequestHeader
import org.microg.gms.constellation.proto.RequestTrigger
import org.microg.gms.constellation.proto.Verification
import org.microg.gms.constellation.proto.VerificationMethod
import org.microg.gms.constellation.proto.builders.RequestBuildContext
import org.microg.gms.constellation.proto.builders.invoke

object ChallengeProcessor {
    private const val TAG = "ChallengeProcessor"
    private const val MAX_PROCEED_ROUNDS = 16

    private fun challengeTimeRemainingMillis(verification: Verification): Long? {
        val expiry = verification.pending_verification_info?.challenge?.expiry_time ?: return null
        val targetMillis = expiry.timestamp?.toEpochMilli() ?: return null
        val referenceMillis = expiry.now?.toEpochMilli() ?: return null
        return targetMillis - referenceMillis
    }

    suspend fun process(
        context: Context,
        sessionId: String,
        imsiToInfoMap: Map<String, SubscriptionInfo>,
        buildContext: RequestBuildContext,
        verification: Verification,
    ): Verification {
        var currentVerification = verification

        for (attempt in 1..MAX_PROCEED_ROUNDS) {
            if (currentVerification.getState() != Verification.State.PENDING) {
                Log.d(
                    TAG,
                    "Verification state: ${currentVerification.getState()}. Stopping sequential verification."
                )
                return currentVerification
            }

            val challenge = currentVerification.pending_verification_info?.challenge
            if (challenge == null) {
                Log.w(
                    TAG,
                    "Attempt $attempt: Pending verification but no challenge found. Stopping."
                )
                return currentVerification
            }

            val challengeId = challenge.challenge_id?.id ?: ""
            val remainingMillis = challengeTimeRemainingMillis(currentVerification)
            if (remainingMillis != null && remainingMillis <= 0L) {
                Log.w(TAG, "Attempt $attempt: Challenge $challengeId expired before proceed")
                return currentVerification
            }
            Log.d(
                TAG,
                "Attempt $attempt: Solving challenge ID: $challengeId, Type: ${challenge.type}"
            )

            val challengeImsi = currentVerification.association?.sim?.sim_info?.imsi?.firstOrNull()
            val info = imsiToInfoMap[challengeImsi]
            val subId = info?.subscriptionId ?: -1

            val challengeResponse: ChallengeResponse? = when {
                challenge.type == VerificationMethod.TS43 -> Ts43Verifier(
                    context,
                    subId
                ).verify(challenge.ts43_challenge)

                challenge.type == VerificationMethod.CARRIER_ID && challenge.ts43_challenge != null ->
                    Ts43Verifier(context, subId).verify(challenge.ts43_challenge)

                challenge.type == VerificationMethod.CARRIER_ID ->
                    CarrierIdVerifier(context, subId).verify(challenge)

                challenge.type == VerificationMethod.MT_SMS -> MtSmsVerifier(
                    context,
                    subId
                ).verify(challenge.mt_challenge)

                challenge.type == VerificationMethod.MO_SMS -> MoSmsVerifier(
                    context,
                    subId
                ).verify(challenge.mo_challenge)

                challenge.type == VerificationMethod.REGISTERED_SMS ->
                    RegisteredSmsVerifier(context, subId).verify(challenge.registered_sms_challenge)

                challenge.type == VerificationMethod.FLASH_CALL -> {
                    Log.w(TAG, "Flash call verification is unavailable on this build")
                    null
                }

                else -> {
                    Log.w(TAG, "Unsupported verification method: ${challenge.type}")
                    null
                }
            }

            if (challengeResponse != null) {
                Log.d(TAG, "Attempt $attempt: Challenge successfully solved. Proceeding...")
                val proceedHeader = RequestHeader(
                    context,
                    sessionId,
                    buildContext,
                    RequestTrigger.Type.TRIGGER_API_CALL,
                    includeClientAuth = true
                )
                val proceedRequest = ProceedRequest(
                    verification = currentVerification,
                    challenge_response = challengeResponse,
                    header_ = proceedHeader
                )
                val proceedResponse = try {
                    RpcClient.phoneDeviceVerificationClient.Proceed().execute(proceedRequest)
                } catch (e: GrpcException) {
                    if (e.grpcStatus == GrpcStatus.PERMISSION_DENIED ||
                        e.grpcStatus == GrpcStatus.UNAUTHENTICATED
                    ) {
                        Log.w(
                            TAG,
                            "Suspicious client status ${e.grpcStatus.name}. Clearing DroidGuard cache..."
                        )
                        ConstellationStateStore.clearDroidGuardToken(context)
                    }
                    throw e
                }
                ConstellationStateStore.storeProceedResponse(context, proceedResponse)
                currentVerification = proceedResponse.verification ?: currentVerification
            } else {
                Log.w(
                    TAG,
                    "Attempt $attempt: Challenge verification failed or returned no response."
                )
                // GMS continues looping if IMSI doesn't match or other issues, but here we return to avoid infinite retries if verifier is broken
                return currentVerification
            }
        }

        Log.w(TAG, "Exhausted all $MAX_PROCEED_ROUNDS proceed rounds, record is still pending.")
        return currentVerification
    }
}
