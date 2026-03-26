package org.microg.gms.constellation.core

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.IdTokenRequest
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.PhoneNumberVerification
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.constellation.core.proto.SyncRequest
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.builders.RequestBuildContext
import org.microg.gms.constellation.core.proto.builders.buildImsiToSubscriptionInfoMap
import org.microg.gms.constellation.core.proto.builders.buildRequestContext
import org.microg.gms.constellation.core.proto.builders.invoke
import org.microg.gms.constellation.core.verification.ChallengeProcessor
import java.util.UUID

private const val TAG = "VerifyPhoneNumber"

private enum class ReadCallbackMode {
    NONE,
    LEGACY,
    TYPED
}

suspend fun handleVerifyPhoneNumberV1(
    context: Context,
    callbacks: IConstellationCallbacks,
    bundle: Bundle,
    packageName: String?
) {
    val callingPackage = packageName ?: bundle.getString("calling_package") ?: context.packageName
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
        policyId = extras.getString("policy_id", ""),
        timeout = timeout,
        idTokenRequest = IdTokenRequest(
            extras.getString("certificate_hash", ""),
            extras.getString("token_nonce", "")
        ),
        extras = extras,
        targetedSims = emptyList(),
        silent = false,
        apiVersion = 2,
        verificationMethodsValues = emptyList()
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

suspend fun handleVerifyPhoneNumberSingleUse(
    context: Context,
    callbacks: IConstellationCallbacks,
    bundle: Bundle,
    packageName: String?
) {
    val callingPackage = packageName ?: bundle.getString("calling_package") ?: context.packageName
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
        policyId = extras.getString("policy_id", ""),
        timeout = timeout,
        idTokenRequest = IdTokenRequest(
            extras.getString("certificate_hash", ""),
            extras.getString("token_nonce", "")
        ),
        extras = extras,
        targetedSims = emptyList(),
        silent = false,
        apiVersion = 2,
        verificationMethodsValues = emptyList()
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
    val callingPackage = packageName ?: context.packageName
    val requestWithExtras = request.copy(
        extras = Bundle(request.extras).apply {
            putString("calling_api", "verifyPhoneNumber")
        }
    )
    val useReadPath = when (requestWithExtras.apiVersion) {
        0 -> requestWithExtras.policyId in VerifyPhoneNumberApiPhenotypes.READ_ONLY_POLICY_IDS
        2 -> VerifyPhoneNumberApiPhenotypes.ENABLE_READ_FLOW
        3 -> requestWithExtras.policyId in VerifyPhoneNumberApiPhenotypes.POLICY_IDS_ALLOWED_FOR_LOCAL_READ
        else -> false
    }

    handleVerifyPhoneNumberRequest(
        context,
        callbacks,
        requestWithExtras,
        callingPackage,
        if (useReadPath) ReadCallbackMode.TYPED else ReadCallbackMode.NONE,
        localReadFallback = requestWithExtras.apiVersion == 3 && useReadPath,
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
        when (readCallbackMode) {
            ReadCallbackMode.LEGACY -> {
                callbacks.onPhoneNumberVerified(
                    Status.INTERNAL_ERROR,
                    emptyList(),
                    ApiMetadata.DEFAULT
                )
            }

            ReadCallbackMode.NONE -> {
                if (legacyCallbackOnFullFlow) {
                    callbacks.onPhoneNumberVerified(
                        Status.INTERNAL_ERROR,
                        emptyList(),
                        ApiMetadata.DEFAULT
                    )
                } else {
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status.INTERNAL_ERROR,
                        VerifyPhoneNumberResponse(emptyArray(), Bundle()),
                        ApiMetadata.DEFAULT
                    )
                }
            }

            ReadCallbackMode.TYPED -> {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status.INTERNAL_ERROR,
                    VerifyPhoneNumberResponse(emptyArray(), Bundle()),
                    ApiMetadata.DEFAULT
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
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
    val (verifications, isPublicKeyAcked) = executeSyncFlow(
        context,
        sessionId,
        request,
        syncRequest,
        buildContext,
        imsiToInfoMap
    )
    if (isPublicKeyAcked) {
        ConstellationStateStore.setPublicKeyAcked(context, true)
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
    if (verificationStatus != Verification.Status.STATUS_VERIFIED.value || phoneNumber.isNullOrEmpty()) {
        return null
    }
    val extras = Bundle(this.extras ?: Bundle.EMPTY).apply {
        verificationToken?.let { putString("id_token", it) }
    }
    return PhoneNumberInfo(1, phoneNumber, timestampMillis, extras)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
private suspend fun executeSyncFlow(
    context: Context,
    sessionId: String,
    request: VerifyPhoneNumberRequest,
    syncRequest: SyncRequest,
    buildContext: RequestBuildContext,
    imsiToInfoMap: Map<String, SubscriptionInfo>
): Pair<Array<PhoneNumberVerification>, Boolean> = withContext(Dispatchers.IO) {
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

        val finalVerification = if (verification.getState() == Verification.State.PENDING) {
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

        finalVerification.toClientVerification(imsiToSlotMap)
    }.toTypedArray()

    if (isPublicKeyAcked) {
        Log.d(TAG, "Server acknowledged client public key")
    }

    verifications to isPublicKeyAcked
}
