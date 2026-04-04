@file:RequiresApi(Build.VERSION_CODES.O)

package org.microg.gms.constellation.core

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberRequest.IdTokenRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.VerifyPhoneNumberResponse.PhoneNumberVerification
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.SyncRequest
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.builder.RequestBuildContext
import org.microg.gms.constellation.core.proto.builder.buildImsiToSubscriptionInfoMap
import org.microg.gms.constellation.core.proto.builder.buildRequestContext
import org.microg.gms.constellation.core.proto.builder.invoke
import org.microg.gms.constellation.core.verification.ChallengeProcessor
import org.microg.gms.constellation.core.verification.MtSmsInboxRegistry
import java.util.UUID

private const val TAG = "VerifyPhoneNumber"

private enum class ReadCallbackMode {
    NONE,
    LEGACY,
    TYPED
}

@Suppress("DEPRECATION")
suspend fun handleVerifyPhoneNumberV1(
    context: Context,
    callbacks: IConstellationCallbacks,
    bundle: Bundle,
    packageName: String?
) {
    val callingPackage =
        packageName ?: bundle.getString("calling_package") ?: Constants.GMS_PACKAGE_NAME
    val extras = Bundle(bundle).apply {
        putString("calling_package", callingPackage)
        putString("calling_api", "verifyPhoneNumber")
    }
    val timeout = when (val timeoutValue = extras.get("timeout")) {
        is Long -> timeoutValue
        is Int -> timeoutValue.toLong()
        else -> 300L
    }
    val request = VerifyPhoneNumberRequest(
        extras.getString("policy_id", ""),
        timeout,
        IdTokenRequest(
            extras.getString("certificate_hash", ""),
            extras.getString("token_nonce", "")
        ),
        extras,
        emptyList(),
        false,
        2,
        emptyList()
    )
    val policyId = bundle.getString("policy_id", "")
    val mode = bundle.getInt("verification_mode", 0)
    val useReadPath = when (mode) {
        0 -> policyId in VerifyPhoneNumberApiPhenotypes.READ_ONLY_POLICY_IDS
        2 -> VerifyPhoneNumberApiPhenotypes.ENABLE_READ_FLOW

        else -> false
    }

    handleVerifyPhoneNumberRequest(
        context,
        callbacks,
        request,
        callingPackage,
        if (useReadPath) ReadCallbackMode.LEGACY else ReadCallbackMode.NONE,
        legacyCallbackOnFullFlow = true
    )
}

@Suppress("DEPRECATION")
suspend fun handleVerifyPhoneNumberSingleUse(
    context: Context,
    callbacks: IConstellationCallbacks,
    bundle: Bundle,
    packageName: String?
) {
    val callingPackage =
        packageName ?: bundle.getString("calling_package") ?: Constants.GMS_PACKAGE_NAME
    val extras = Bundle(bundle).apply {
        putString("calling_package", callingPackage)
        putString("calling_api", "verifyPhoneNumberSingleUse")
        putString("one_time_verification", "True")
    }
    val timeout = when (val timeoutValue = extras.get("timeout")) {
        is Long -> timeoutValue
        is Int -> timeoutValue.toLong()
        else -> 300L
    }
    val request = VerifyPhoneNumberRequest(
        extras.getString("policy_id", ""),
        timeout,
        IdTokenRequest(
            extras.getString("certificate_hash", ""),
            extras.getString("token_nonce", "")
        ),
        extras,
        emptyList(),
        false,
        2,
        emptyList()
    )

    handleVerifyPhoneNumberRequest(
        context,
        callbacks,
        request,
        callingPackage,
        ReadCallbackMode.NONE,
        legacyCallbackOnFullFlow = true
    )
}

suspend fun handleVerifyPhoneNumberRequest(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: VerifyPhoneNumberRequest,
    packageName: String?
) {
    val callingPackage = packageName ?: Constants.GMS_PACKAGE_NAME
    request.extras.putString("calling_api", "verifyPhoneNumber")
    val useReadPath = when (request.apiVersion) {
        0 -> request.policyId in VerifyPhoneNumberApiPhenotypes.READ_ONLY_POLICY_IDS
        2 -> VerifyPhoneNumberApiPhenotypes.ENABLE_READ_FLOW
        3 -> request.policyId in VerifyPhoneNumberApiPhenotypes.POLICY_IDS_ALLOWED_FOR_LOCAL_READ
        else -> false
    }

    handleVerifyPhoneNumberRequest(
        context,
        callbacks,
        request,
        callingPackage,
        if (useReadPath) ReadCallbackMode.TYPED else ReadCallbackMode.NONE,
        localReadFallback = request.apiVersion == 3 && useReadPath,
        legacyCallbackOnFullFlow = false
    )
}

private suspend fun handleVerifyPhoneNumberRequest(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: VerifyPhoneNumberRequest,
    callingPackage: String,
    readCallbackMode: ReadCallbackMode,
    localReadFallback: Boolean = false,
    legacyCallbackOnFullFlow: Boolean = false
) {
    try {
        when (readCallbackMode) {
            ReadCallbackMode.LEGACY -> {
                Log.d(TAG, "Using read-only mode")
                handleGetVerifiedPhoneNumbers(context, callbacks, request.extras)
            }

            ReadCallbackMode.TYPED -> {
                if (localReadFallback) {
                    Log.w(TAG, "Local-read mode not implemented, falling back to read-only RPC")
                } else {
                    Log.d(TAG, "Using typed read-only mode")
                }
                val response = fetchVerifiedPhoneNumbers(context, request.extras, callingPackage)
                    .toVerifyPhoneNumberResponse()
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status.SUCCESS,
                    response,
                    ApiMetadata.DEFAULT
                )
            }

            ReadCallbackMode.NONE -> {
                Log.d(TAG, "Using full verification mode")
                runVerificationFlow(
                    context,
                    request,
                    callingPackage,
                    callbacks,
                    legacyCallback = legacyCallbackOnFullFlow
                )
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "verifyPhoneNumber failed", e)
        val status = if (readCallbackMode == ReadCallbackMode.NONE && e is GrpcException) {
            handleRpcError(e)
        } else {
            Status.INTERNAL_ERROR
        }
        when (readCallbackMode) {
            ReadCallbackMode.LEGACY -> {
                callbacks.onPhoneNumberVerified(
                    status,
                    emptyList(),
                    ApiMetadata.DEFAULT
                )
            }

            ReadCallbackMode.NONE -> {
                if (legacyCallbackOnFullFlow) {
                    callbacks.onPhoneNumberVerified(
                        status,
                        emptyList(),
                        ApiMetadata.DEFAULT
                    )
                } else {
                    callbacks.onPhoneNumberVerificationsCompleted(
                        status,
                        VerifyPhoneNumberResponse(emptyArray(), Bundle()),
                        ApiMetadata.DEFAULT
                    )
                }
            }

            ReadCallbackMode.TYPED -> {
                callbacks.onPhoneNumberVerificationsCompleted(
                    status,
                    VerifyPhoneNumberResponse(emptyArray(), Bundle()),
                    ApiMetadata.DEFAULT
                )
            }
        }
    }
}

private fun handleRpcError(error: GrpcException): Status {
    val statusCode = when (error.grpcStatus) {
        GrpcStatus.RESOURCE_EXHAUSTED -> 5008
        GrpcStatus.DEADLINE_EXCEEDED,
        GrpcStatus.ABORTED,
        GrpcStatus.UNAVAILABLE -> 5007

        GrpcStatus.PERMISSION_DENIED -> 5009
        else -> 5002
    }
    return Status(statusCode, error.message)
}

private suspend fun runVerificationFlow(
    context: Context,
    request: VerifyPhoneNumberRequest,
    callingPackage: String,
    callbacks: IConstellationCallbacks,
    legacyCallback: Boolean
) {
    val sessionId = UUID.randomUUID().toString()

    val imsiToInfoMap = buildImsiToSubscriptionInfoMap(context)
    val buildContext = buildRequestContext(context)
    val syncRequest = SyncRequest(
        context,
        sessionId,
        request,
        buildContext,
        imsiToInfoMap = imsiToInfoMap,
        includeClientAuth = ConstellationStateStore.isPublicKeyAcked(context),
        callingPackage = callingPackage
    )

    MtSmsInboxRegistry.prepare(
        context.applicationContext,
        imsiToInfoMap.values.map { it.subscriptionId })

    val verifications = try {
        executeSyncFlow(
            context,
            sessionId,
            request,
            syncRequest,
            buildContext,
            imsiToInfoMap
        )
    } finally {
        MtSmsInboxRegistry.dispose()
    }

    if (legacyCallback) {
        callbacks.onPhoneNumberVerified(
            Status.SUCCESS,
            verifications.mapNotNull { it.toLegacyPhoneNumberInfoOrNull() },
            ApiMetadata.DEFAULT
        )
    } else {
        callbacks.onPhoneNumberVerificationsCompleted(
            Status.SUCCESS,
            VerifyPhoneNumberResponse(verifications, Bundle()),
            ApiMetadata.DEFAULT
        )
    }
}

private fun PhoneNumberVerification.toLegacyPhoneNumberInfoOrNull(): PhoneNumberInfo? {
    if (
        verificationStatus != Verification.Status.STATUS_VERIFIED.toClientStatus() ||
        phoneNumber.isNullOrEmpty()
    ) {
        return null
    }
    val extras = Bundle(this.extras ?: Bundle.EMPTY).apply {
        verificationToken?.let { putString("id_token", it) }
    }
    return PhoneNumberInfo(1, phoneNumber, timestampMillis, extras)
}

private suspend fun executeSyncFlow(
    context: Context,
    sessionId: String,
    request: VerifyPhoneNumberRequest,
    syncRequest: SyncRequest,
    buildContext: RequestBuildContext,
    imsiToInfoMap: Map<String, SubscriptionInfo>
): Array<PhoneNumberVerification> = withContext(Dispatchers.IO) {
    Log.d(TAG, "Sending Sync request")
    val syncResponse = try {
        RpcClient.phoneDeviceVerificationClient.Sync().execute(syncRequest)
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
    Log.d(TAG, "Sync response: ${syncResponse.responses.size} verifications")
    ConstellationStateStore.storeSyncResponse(context, syncResponse)

    val isPublicKeyAcked = syncResponse.header_?.status?.code == 1
    val imsiToSlotMap = imsiToInfoMap.mapValues { it.value.simSlotIndex }
    val requestedImsis = request.targetedSims.map { it.imsi }.toSet()

    val verifications = syncResponse.responses.mapNotNull { result ->
        val verification = result.verification ?: Verification()
        val verificationImsis = verification.association?.sim?.sim_info?.imsi.orEmpty()
        if (requestedImsis.isNotEmpty() && verificationImsis.none { it in requestedImsis }) {
            Log.w(
                TAG,
                "Skipping verification for IMSIs=$verificationImsis because it does not match requested IMSIs=$requestedImsis"
            )
            return@mapNotNull null
        }

        val finalVerification = if (verification.state == Verification.State.PENDING) {
            ChallengeProcessor.process(
                context,
                sessionId,
                imsiToInfoMap,
                buildContext,
                verification
            )
        } else {
            verification
        }

        if (finalVerification.state != Verification.State.VERIFIED) {
            Log.w(TAG, "Unverified. State: ${finalVerification.state}")
            (finalVerification.pending_verification_info
                ?: finalVerification.unverified_info)?.let { Log.w(TAG, it.toString()) }

            if (!request.includeUnverified) {
                return@mapNotNull null
            }
        }

        finalVerification.toClientVerification(imsiToSlotMap)
    }.toTypedArray()

    if (isPublicKeyAcked) {
        Log.d(TAG, "Server acknowledged client public key")
        ConstellationStateStore.setPublicKeyAcked(context, true)
    }

    verifications
}
