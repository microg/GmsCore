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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.microg.gms.common.Constants
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.DeviceID
import org.microg.gms.constellation.core.proto.GetConsentRequest
import org.microg.gms.constellation.core.proto.GetConsentResponse
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.PhoneDeviceVerificationClient
import org.microg.gms.constellation.core.proto.RcsConsent
import org.microg.gms.constellation.core.proto.RcsState
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.SetConsentRequest
import org.microg.gms.constellation.core.proto.SyncRequest
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.VerifiedPhoneNumber
import org.microg.gms.constellation.core.proto.builder.RequestBuildContext
import org.microg.gms.constellation.core.proto.builder.buildImsiToSubscriptionInfoMap
import org.microg.gms.constellation.core.proto.builder.buildRequestContext
import org.microg.gms.constellation.core.proto.builder.getList
import org.microg.gms.constellation.core.proto.builder.invoke
import org.microg.gms.constellation.core.verification.ChallengeProcessor
import org.microg.gms.constellation.core.verification.MtSmsInboxRegistry
import org.microg.gms.constellation.core.verification.mtSmsInboxSubscriptionIds
import java.util.UUID
import kotlin.coroutines.coroutineContext

private const val TAG = "VerifyPhoneNumber"

internal enum class ReadCallbackMode {
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
    val normalizedRequest = VerifyPhoneNumberRequest(
        request.policyId ?: "",
        request.timeout,
        request.idTokenRequest,
        Bundle(request.extras ?: Bundle.EMPTY).apply {
            putString("calling_api", "verifyPhoneNumber")
        },
        request.targetedSims ?: emptyList(),
        request.includeUnverified,
        request.apiVersion,
        request.verificationMethodsValues ?: emptyList()
    )
    val useReadPath = when (normalizedRequest.apiVersion) {
        0 -> normalizedRequest.policyId in VerifyPhoneNumberApiPhenotypes.READ_ONLY_POLICY_IDS
        2 -> VerifyPhoneNumberApiPhenotypes.ENABLE_READ_FLOW
        3 -> normalizedRequest.policyId in VerifyPhoneNumberApiPhenotypes.POLICY_IDS_ALLOWED_FOR_LOCAL_READ
        else -> false
    }

    handleVerifyPhoneNumberRequest(
        context,
        callbacks,
        normalizedRequest,
        callingPackage,
        if (useReadPath) ReadCallbackMode.TYPED else ReadCallbackMode.NONE,
        localReadFallback = normalizedRequest.apiVersion == 3 && useReadPath,
        legacyCallbackOnFullFlow = false
    )
}

internal suspend fun handleVerifyPhoneNumberRequest(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: VerifyPhoneNumberRequest,
    callingPackage: String,
    readCallbackMode: ReadCallbackMode,
    localReadFallback: Boolean = false,
    legacyCallbackOnFullFlow: Boolean = false,
    readRemoteVerifiedNumbers: suspend () -> List<VerifiedPhoneNumber> = {
        fetchVerifiedPhoneNumbers(
            context, request.extras, callingPackage, idTokenRequest = request.idTokenRequest
        )
    }
) {
    var phoneNumbers = emptyList<PhoneNumberInfo>()
    var verifications = emptyArray<PhoneNumberVerification>()
    val status = try {
        if (!ConstellationStateStore.isPhoneNumberVerificationEnabled(context)) {
            throw PhoneNumberVerificationDisabledException()
        }

        when (readCallbackMode) {
            ReadCallbackMode.LEGACY -> {
                Log.d(TAG, "Using read-only mode")
                phoneNumbers = fetchVerifiedPhoneNumbers(context, request.extras)
                    .map { it.toPhoneNumberInfo() }
                Status.SUCCESS
            }

            ReadCallbackMode.TYPED -> {
                val localVerifications =
                    if (localReadFallback) loadLocalVerifications(context, request) else null
                if (localVerifications != null) {
                    Log.d(
                        TAG,
                        "Using local-read mode, answering ${localVerifications.size} verification(s) from local state"
                    )
                    verifications = localVerifications
                } else {
                    if (localReadFallback) {
                        Log.d(TAG, "No valid local state for local-read mode, using read-only RPC")
                    } else {
                        Log.d(TAG, "Using typed read-only mode")
                    }
                    verifications = readRemoteVerifiedNumbers()
                        .map { it.toPhoneNumberVerification() }
                        .toTypedArray()
                }
                Status.SUCCESS
            }

            ReadCallbackMode.NONE -> {
                Log.d(TAG, "Using full verification mode")
                verifications = runVerificationFlow(context, request, callingPackage)
                Status.SUCCESS
            }
        }
    } catch (e: CancellationException) {
        // The request's coroutine was cancelled (e.g. the caller process died). A cancelled request
        // must not be turned into a recorded verification outcome or a callback delivery, and the
        // cancellation must reach the dispatcher. Never let the broad catch below swallow it.
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "verifyPhoneNumber failed", e)
        when {
            e is PhoneNumberVerificationDisabledException -> Status(5000)
            readCallbackMode != ReadCallbackMode.NONE -> Status.INTERNAL_ERROR
            e is GrpcException -> handleRpcError(e)
            else -> Status.INTERNAL_ERROR
        }
    }

    // Synchronous settings calls can return normally after this request was cancelled.
    coroutineContext.ensureActive()
    val successful = status.isSuccess && when (readCallbackMode) {
        ReadCallbackMode.NONE -> verifications.isNotEmpty() &&
                (request.targetedSims.isEmpty() ||
                        verifications.size == request.targetedSims.size) &&
                verifications.all {
                    it.verificationStatus == Verification.Status.STATUS_VERIFIED.toClientStatus()
                }

        else -> true
    }
    ConstellationStateStore.recordPhoneNumberVerification(context, callingPackage, successful)
    coroutineContext.ensureActive()

    if (readCallbackMode == ReadCallbackMode.LEGACY ||
        readCallbackMode == ReadCallbackMode.NONE && legacyCallbackOnFullFlow
    ) {
        if (readCallbackMode != ReadCallbackMode.LEGACY) {
            phoneNumbers = verifications.mapNotNull { it.toLegacyPhoneNumberInfoOrNull() }
        }
        callbacks.onPhoneNumberVerified(status, phoneNumbers, ApiMetadata.DEFAULT)
        return
    }

    callbacks.onPhoneNumberVerificationsCompleted(
        status,
        VerifyPhoneNumberResponse(
            verifications,
            Bundle()
        ),
        ApiMetadata.DEFAULT
    )
}

/**
 * The locally stored verifications that answer [request], or null when local state cannot answer
 * it and the read-only RPC has to run instead. Every SIM the caller targeted must have a stored,
 * unexpired verification: a partial answer would hide SIMs the caller asked about.
 */
private fun loadLocalVerifications(
    context: Context,
    request: VerifyPhoneNumberRequest
): Array<PhoneNumberVerification>? {
    val stored = ConstellationStateStore.loadVerifiedNumbers(context)
    if (stored.isEmpty()) return null

    val requestedImsis = request.targetedSims
        .map { it.imsi?.takeIf(String::isNotEmpty) ?: return null }
        .toSet()
    if (requestedImsis.isEmpty()) {
        return stored.map { it.toPhoneNumberVerification() }.toTypedArray()
    }

    val byImsi = stored.associateBy { it.imsi }
    val matching = requestedImsis.map { imsi -> byImsi[imsi] ?: return null }
    return matching.map { it.toPhoneNumberVerification() }.toTypedArray()
}

/** Mirrors the shape the read-only RPC path returns for a verified number. */
private fun StoredVerifiedNumber.toPhoneNumberVerification(): PhoneNumberVerification {
    val extras = Bundle().apply {
        putInt("rcs_state", rcsState)
    }

    // Like read-only V2, a read answer leaves method/slot unset and reports a verified record.
    return PhoneNumberVerification(
        phoneNumber,
        verificationTimeMillis,
        0,
        -1,
        idToken,
        extras,
        1,
        -1L
    )
}

private class PhoneNumberVerificationDisabledException : Exception("Phone number verification is disabled")

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
    callingPackage: String
): Array<PhoneNumberVerification> {
    val sessionId = UUID.randomUUID().toString()

    val imsiToInfoMap = buildImsiToSubscriptionInfoMap(context)
    val buildContext = buildRequestContext(context)

    val asterismClient = when (request.extras.getString("required_consumer_consent")) {
        "RCS" -> AsterismClient.RCS
        else -> AsterismClient.UNKNOWN_CLIENT
    }
    if (
        request.extras.getString("one_time_verification") != "True" &&
        asterismClient != AsterismClient.UNKNOWN_CLIENT
    ) {
        val consent = getConsent(
            context,
            buildContext,
            asterismClient,
            sessionId,
        )

        val consented = consent.rcs_consent?.consent == Consent.CONSENTED ||
                consent.gaia_consents.any {
                    it.asterism_client == asterismClient && it.consent == Consent.CONSENTED
                }

        if (!consented) {
            Log.e(TAG, "Consent has not been set. Auto-setting consent.")
            val consentType = parseConsentVersion(request.extras)
            val setRequest = SetConsentRequest(
                header_ = RequestHeader(context, sessionId, buildContext, "setConsent"),
                asterism_client = asterismClient,
                rcs_consent = RcsConsent(
                    consent = Consent.CONSENTED,
                    consent_version = consentType
                ),
                consent_version = consentType,
                api_params = Param.getList(request.extras)
            )
            autoSetConsent(context, setRequest)
        }
    }

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
        mtSmsInboxSubscriptionIds(imsiToInfoMap.values.map { it.subscriptionId })
    )

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

    return verifications
}

internal suspend fun autoSetConsent(
    context: Context,
    request: SetConsentRequest,
    client: PhoneDeviceVerificationClient = RpcClient.phoneDeviceVerificationClient
) {
    try {
        client.SetConsent().execute(request)
        Log.i(TAG, "Auto-consented for ${request.asterism_client}")
    } catch (e: CancellationException) {
        // Don't continue the verification flow for a request that was cancelled mid-consent.
        throw e
    } catch (e: GrpcException) {
        if (e.grpcStatus == GrpcStatus.PERMISSION_DENIED ||
            e.grpcStatus == GrpcStatus.UNAUTHENTICATED
        ) {
            // Match GetConsent and Sync: discard the rejected token before the next request.
            Log.w(
                TAG,
                "Suspicious client status ${e.grpcStatus.name}. Clearing DroidGuard cache..."
            )
            ConstellationStateStore.clearDroidGuardToken(context)
            throw e
        }
        // Automatic consent remains best effort for other service/transport failures.
        Log.w(TAG, "Auto-consent failed", e)
    } catch (e: Exception) {
        Log.w(TAG, "Auto-consent failed", e)
    }
}

private fun parseConsentVersion(extras: Bundle): ConsentVersion {
    val value = extras.getString("consent_type")
    val parsed = value?.toIntOrNull()?.let(ConsentVersion::fromValue)
        ?: value?.let { runCatching { ConsentVersion.valueOf(it) }.getOrNull() }
    return parsed?.takeUnless { it == ConsentVersion.CONSENT_VERSION_UNSPECIFIED }
        ?: ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI
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

    val refreshedImsis = mutableSetOf<String>()
    val localRecords = mutableListOf<StoredVerifiedNumber>()
    val localStateDeadline = ConstellationStateStore.nextSyncDeadlineMillis(syncResponse.next_sync_time)

    val verifications = syncResponse.responses.mapNotNull { result ->
        val verification = result.verification ?: Verification()
        val verificationImsis = verification.association?.sim?.sim_info?.imsi.orEmpty()
        // Error-only records have no SIM association and must not be silently filtered.
        // Records for another SIM retain the existing targeted-SIM filtering.
        val errorOnly = result.verification == null &&
                result.error?.code?.let { it != 0 } == true
        if (!errorOnly && requestedImsis.isNotEmpty() &&
            verificationImsis.none { it in requestedImsis }
        ) {
            Log.w(
                TAG,
                "Skipping verification because response SIM associations do not match the requested SIM set"
            )
            return@mapNotNull null
        }
        result.error?.let { error ->
            check(error.code == 0) {
                "Sync verification failed (${error.code}): ${error.message}"
            }
        }
        refreshedImsis += verificationImsis.filter { it.isNotEmpty() }

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

            if (!request.includeUnverified) {
                return@mapNotNull null
            }
        }

        if (localStateDeadline != null && finalVerification.state == Verification.State.VERIFIED) {
            val info = finalVerification.verification_info
            val verifiedNumber = info?.phone_number
            if (!verifiedNumber.isNullOrEmpty()) {
                val idToken = finalVerification.api_params
                    .firstOrNull { it.key == "id_token" }
                    ?.value_
                    ?.takeIf { it.isNotEmpty() }
                for (imsi in verificationImsis) {
                    if (imsi.isEmpty()) continue
                    localRecords += StoredVerifiedNumber(
                        imsi,
                        verifiedNumber,
                        info.verification_time?.toEpochMilli() ?: 0L,
                        idToken,
                        // The sync flow does not report an RCS state for a verification.
                        RcsState.STATE_UNSPECIFIED.value,
                        localStateDeadline
                    )
                }
            }
        }

        finalVerification.toClientVerification(imsiToSlotMap)
    }.toTypedArray()

    ConstellationStateStore.storeVerifiedNumbers(
        context,
        localRecords,
        refreshedImsis = refreshedImsis
    )

    if (isPublicKeyAcked) {
        Log.d(TAG, "Server acknowledged client public key")
        ConstellationStateStore.setPublicKeyAcked(context, true)
    }

    verifications
}

suspend fun getConsent(
    context: Context,
    buildContext: RequestBuildContext,
    asterismClient: AsterismClient,
    sessionId: String = UUID.randomUUID().toString()
): GetConsentResponse {
    val request = GetConsentRequest(
        device_id = DeviceID(context, buildContext.iidToken),
        header_ = RequestHeader(
            context,
            sessionId,
            buildContext,
            "getConsent"
        ),
        asterism_client = asterismClient
    )

    return try {
        RpcClient.phoneDeviceVerificationClient.GetConsent().execute(request)
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
}
