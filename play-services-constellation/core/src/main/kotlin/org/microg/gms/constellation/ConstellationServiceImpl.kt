/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberVerification
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.droidguard.DroidGuardClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.tasks.await
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.phonenumberverification.*
import org.microg.gms.phonenumberverification.Error as ProtoError
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.Locale
import java.util.UUID

private const val TAG = "ConstellationImpl"
private const val CONSTELLATION_AUTHORIZED_ENTITY = "496232013492"
private const val GMSCORE_VERSION_NUMBER = 244631038
private const val GMSCORE_VERSION_STRING = "24.46.31 (190408-693505712)"

class ConstellationServiceImpl(
    private val context: Context,
    private val packageName: String
) : IConstellationApiService.Stub() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun verifyPhoneNumber(callbacks: IConstellationCallbacks?, request: VerifyPhoneNumberRequest?, apiMetadata: Bundle?) {
        Log.d(TAG, "verifyPhoneNumber called by $packageName, policyId=${request?.upiPolicyId}")
        if (callbacks == null || request == null) return

        scope.launch {
            try {
                doVerifyPhoneNumber(callbacks, request)
            } catch (e: Exception) {
                Log.e(TAG, "verifyPhoneNumber failed", e)
                runCatching {
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status(CommonStatusCodes.INTERNAL_ERROR, "Verification failed: ${e.message}"),
                        null, null
                    )
                }
            }
        }
    }

    override fun getIidToken(callbacks: IConstellationCallbacks?, request: GetIidTokenRequest?, apiMetadata: Bundle?) {
        Log.d(TAG, "getIidToken called by $packageName, appId=${request?.appId}")
        if (callbacks == null) return

        scope.launch {
            try {
                val iidToken = getOrCreateIidToken()
                val response = GetIidTokenResponse(
                    iidToken, null, null, System.currentTimeMillis()
                )
                runCatching {
                    callbacks.onIidTokenGenerated(Status.SUCCESS, response, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getIidToken failed", e)
                runCatching {
                    callbacks.onIidTokenGenerated(
                        Status(CommonStatusCodes.INTERNAL_ERROR, e.message),
                        null, null
                    )
                }
            }
        }
    }

    override fun getPnvCapabilities(callbacks: IConstellationCallbacks?, request: GetPnvCapabilitiesRequest?, apiMetadata: Bundle?) {
        Log.d(TAG, "getPnvCapabilities called by $packageName, policyId=${request?.policyId}")
        if (callbacks == null) return

        runCatching {
            callbacks.onGetPnvCapabilitiesCompleted(
                Status.SUCCESS,
                GetPnvCapabilitiesResponse(listOf("TS43", "MT_SMS", "MO_SMS")),
                null
            )
        }
    }

    // ---- Core verification flow ----

    private suspend fun doVerifyPhoneNumber(
        callbacks: IConstellationCallbacks,
        request: VerifyPhoneNumberRequest
    ) {
        val sessionId = UUID.randomUUID().toString()
        Log.d(TAG, "Starting verification session=$sessionId")

        // Step 1: Gather device signals
        val iidToken = getOrCreateIidToken()
        val checkinInfo = LastCheckinInfo.read(context)
        val dgResult = getDroidGuardResult(iidToken)
        val spatulaHeader = getSpatulaHeaderSafe() ?: throw IllegalStateException(
            "Unable to obtain x-goog-spatula header for $packageName"
        )
        val ecKeyPair = generateEcKeyPair()

        Log.d(TAG, "Device signals gathered: iid=${iidToken.take(20)}..., androidId=${checkinInfo.androidId}")

        // Step 2: Build SyncRequest
        val syncRequest = buildSyncRequest(
            request, sessionId, iidToken, checkinInfo.androidId, dgResult, ecKeyPair
        )

        // Step 3: Call Sync
        val client = PhoneVerificationClient(context) { spatulaHeader }
        val syncResponse = client.sync(syncRequest)
        Log.d(TAG, "SyncResponse received: ${syncResponse.responses.size} responses")

        // Step 4: Process SyncResponse
        handleSyncResponse(syncResponse, sessionId, iidToken, checkinInfo.androidId,
            dgResult, ecKeyPair, client, callbacks)
    }

    private suspend fun handleSyncResponse(
        syncResponse: SyncResponse,
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair,
        client: PhoneVerificationClient,
        callbacks: IConstellationCallbacks
    ) {
        if (syncResponse.responses.isEmpty()) {
            Log.w(TAG, "Empty SyncResponse")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "Empty server response"),
                    null, null
                )
            }
            return
        }

        val verificationResponse = syncResponse.responses.first()
        val verification = verificationResponse.verification ?: run {
            Log.w(TAG, "No verification in response, error=${verificationResponse.error}")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR,
                        "Server error: ${verificationResponse.error?.message}"),
                    null, null
                )
            }
            return
        }

        when (verification.state) {
            VerificationState.VERIFICATION_STATE_VERIFIED -> {
                handleVerifiedState(verification, callbacks)
            }
            VerificationState.VERIFICATION_STATE_PENDING -> {
                handlePendingState(verification, sessionId, iidToken, androidId,
                    dgResult, ecKeyPair, client, callbacks)
            }
            else -> {
                Log.w(TAG, "Unexpected verification state: ${verification.state}")
                runCatching {
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status(CommonStatusCodes.INTERNAL_ERROR,
                            "Unexpected state: ${verification.state}"),
                        null, null
                    )
                }
            }
        }
    }

    private fun handleVerifiedState(
        verification: Verification,
        callbacks: IConstellationCallbacks
    ) {
        val info = verification.verification_info
        val phoneNumber = info?.phone_number ?: ""
        val timestamp = info?.verification_time?.epochSecond ?: (System.currentTimeMillis() / 1000)
        val msisdnToken = extractMsisdnToken(verification)

        Log.d(TAG, "Phone verified: $phoneNumber")
        if (msisdnToken.isNullOrBlank()) {
            Log.w(TAG, "Verified state missing msisdn token payload")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "Missing msisdn token in verification"),
                    null,
                    null
                )
            }
            return
        }

        val pnv = PhoneNumberVerification(
            phoneNumber,             // field 1: phoneNumber
            timestamp * 1000,        // field 2: timestampMillis
            1,                       // field 3: verificationMethod (1 = server-verified)
            0,                       // field 4: unknownInt
            msisdnToken,             // field 5: msisdnToken (opaque server token)
            null,                    // field 6: extras
            1,                       // field 7: verificationStatus (1 = SUCCESS)
            0                        // field 8: retryAfterSeconds
        )

        val response = VerifyPhoneNumberResponse(
            arrayOf(pnv), null
        )

        runCatching {
            callbacks.onPhoneNumberVerificationsCompleted(Status.SUCCESS, response, null)
        }
    }

    private suspend fun handlePendingState(
        verification: Verification,
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair,
        client: PhoneVerificationClient,
        callbacks: IConstellationCallbacks
    ) {
        val pendingInfo = verification.pending_verification_info
        val challenge = pendingInfo?.challenge

        if (challenge == null) {
            Log.w(TAG, "PENDING state but no challenge provided")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "No challenge in PENDING state"),
                    null, null
                )
            }
            return
        }

        Log.d(TAG, "Challenge type=${challenge.type}, id=${challenge.challenge_id?.id}")

        when (challenge.type) {
            ChallengeType.CHALLENGE_TYPE_TS43 -> {
                handleTs43Challenge(verification, challenge, sessionId, iidToken,
                    androidId, dgResult, ecKeyPair, client, callbacks)
            }
            ChallengeType.CHALLENGE_TYPE_IMSI_LOOKUP -> {
                // IMSI lookup is server-side — just call Proceed immediately
                Log.d(TAG, "IMSI_LOOKUP challenge, calling Proceed")
                callProceedAndRespond(verification, ChallengeResponse(),
                    sessionId, iidToken, androidId, dgResult, ecKeyPair, client, callbacks)
            }
            else -> {
                Log.w(TAG, "Unsupported challenge type: ${challenge.type}")
                runCatching {
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status(CommonStatusCodes.INTERNAL_ERROR,
                            "Unsupported challenge: ${challenge.type}"),
                        null, null
                    )
                }
            }
        }
    }

    private suspend fun handleTs43Challenge(
        verification: Verification,
        challenge: Challenge,
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair,
        client: PhoneVerificationClient,
        callbacks: IConstellationCallbacks
    ) {
        val ts43 = challenge.ts43_challenge ?: run {
            Log.w(TAG, "TS43 challenge type but no ts43_challenge data")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "Missing TS43 challenge data"),
                    null, null
                )
            }
            return
        }

        Log.d(TAG, "TS43 challenge: url=${ts43.entitlement_url}, appId=${ts43.app_id}, " +
                "realm=${ts43.eap_aka_realm}, integrator=${ts43.ts43_type?.integrator}")

        try {
            val challengeResponse = performTs43Authentication(ts43)
            callProceedAndRespond(verification, challengeResponse,
                sessionId, iidToken, androidId, dgResult, ecKeyPair, client, callbacks)
        } catch (e: Exception) {
            Log.e(TAG, "TS43 authentication failed", e)
            // Send error response in Proceed so server knows what happened
            val errorResponse = ChallengeResponse(
                ts43_challenge_response = Ts43ChallengeResponse(
                    ts43_type = ts43.ts43_type,
                    error = ProtoError(
                        error_type = ErrorType.ERROR_TYPE_RUNTIME_ERROR,
                        service_entitlement_error = ServiceEntitlementError(
                            error_code = -1,
                            http_status = 0,
                            api = "acquireTemporaryToken"
                        )
                    )
                )
            )
            callProceedAndRespond(verification, errorResponse,
                sessionId, iidToken, androidId, dgResult, ecKeyPair, client, callbacks)
        }
    }

    private fun performTs43Authentication(ts43: Ts43Challenge): ChallengeResponse {
        val entitlementUrl = ts43.entitlement_url
        val appId = ts43.app_id

        Log.d(TAG, "Performing TS43 auth: url=$entitlementUrl, appId=$appId")

        // Use Android's service entitlement library via reflection (AOSP framework component)
        // Available on Android 12+ as com.android.libraries.entitlement
        val temporaryToken = try {
            performTs43ViaEntitlementLibrary(entitlementUrl, appId, ts43)
        } catch (e: Exception) {
            Log.w(TAG, "TS43 entitlement library not available, trying direct approach", e)
            performTs43Direct(ts43)
        }

        Log.d(TAG, "TS43 temporary token obtained: ${temporaryToken.take(20)}...")

        return ChallengeResponse(
            ts43_challenge_response = Ts43ChallengeResponse(
                ts43_type = ts43.ts43_type,
                server_challenge_response = ServerChallengeResponse(
                    temporary_token = temporaryToken
                )
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun performTs43ViaEntitlementLibrary(
        url: String, appId: String, ts43: Ts43Challenge
    ): String {
        // Attempt to use com.android.libraries.entitlement.Ts43Authentication
        // This is an AOSP framework library available on Android 12+ with carrier support
        val subId = getActiveSubscriptionId()

        val ts43AuthClass = Class.forName("com.android.libraries.entitlement.Ts43Authentication")
        val entitlementVersion = ts43.service_entitlement_request?.entitlement_version ?: "2.0"

        // Ts43Authentication(context, url, entitlementVersion)
        val constructor = ts43AuthClass.getConstructor(
            Context::class.java, String::class.java, String::class.java
        )
        val ts43Auth = constructor.newInstance(context, url, entitlementVersion)

        // getAuthToken(slotIndex, appId, packageName, "1.0")
        val getAuthTokenMethod = ts43AuthClass.getMethod(
            "getAuthToken", Int::class.javaPrimitiveType,
            String::class.java, String::class.java, String::class.java
        )
        val slotIndex = getSlotIndex(subId)
        val authResult = getAuthTokenMethod.invoke(ts43Auth, slotIndex, appId, packageName, "1.0")

        // authResult.token()
        val tokenMethod = authResult.javaClass.getMethod("token")
        val authToken = tokenMethod.invoke(authResult) as String

        Log.d(TAG, "EAP-AKA auth token obtained")

        // Now acquire temporary token
        val ts43OpClass = Class.forName("com.android.libraries.entitlement.Ts43Operation")
        val opConstructor = ts43OpClass.getConstructor(
            Context::class.java, Int::class.javaPrimitiveType, String::class.java,
            String::class.java, String::class.java, Int::class.javaPrimitiveType,
            String::class.java
        )
        val ts43Op = opConstructor.newInstance(
            context, slotIndex, url, entitlementVersion, authToken, 0, packageName
        )

        // Some platform builds expose a no-arg acquireTemporaryToken(). If the signature differs
        // we fall back to the direct path instead of passing null-filled placeholders.
        val acquireMethod = ts43OpClass.methods.first { it.name == "acquireTemporaryToken" }
        if (acquireMethod.parameterCount != 0) {
            throw IllegalStateException(
                "Unsupported acquireTemporaryToken signature with ${acquireMethod.parameterCount} parameters"
            )
        }
        val tokenResponse = acquireMethod.invoke(ts43Op)

        val tempTokenMethod = tokenResponse.javaClass.getMethod("temporaryToken")
        return tempTokenMethod.invoke(tokenResponse) as String
    }

    private fun performTs43Direct(
        ts43: Ts43Challenge
    ): String {
        val entitlementRequest = ts43.service_entitlement_request
        val existingTemporaryToken = entitlementRequest?.temporary_token?.takeIf { it.isNotBlank() }
        if (existingTemporaryToken != null) {
            Log.i(TAG, "Using temporary token from ServiceEntitlementRequest direct fallback")
            return existingTemporaryToken
        }
        throw UnsupportedOperationException(
            "TS43 entitlement library unavailable and no temporary token in challenge payload"
        )
    }

    private suspend fun callProceedAndRespond(
        verification: Verification,
        challengeResponse: ChallengeResponse,
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair,
        client: PhoneVerificationClient,
        callbacks: IConstellationCallbacks
    ) {
        val header = buildRequestHeader(sessionId, iidToken, androidId, dgResult, ecKeyPair)

        val proceedRequest = ProceedRequest(
            verification = verification,
            challenge_response = challengeResponse,
            header_ = header
        )

        Log.d(TAG, "Calling Proceed...")
        val proceedResponse = client.proceed(proceedRequest)

        val resultVerification = proceedResponse.verification
        if (resultVerification == null) {
            Log.w(TAG, "No verification in ProceedResponse")
            runCatching {
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "Empty Proceed response"),
                    null, null
                )
            }
            return
        }

        Log.d(TAG, "Proceed result state=${resultVerification.state}")

        when (resultVerification.state) {
            VerificationState.VERIFICATION_STATE_VERIFIED -> {
                handleVerifiedState(resultVerification, callbacks)
            }
            VerificationState.VERIFICATION_STATE_PENDING -> {
                // Still pending — could be retryable
                val retryChallenge = resultVerification.pending_verification_info?.challenge
                val retryAfterSec = retryChallenge?.expiry_time?.timestamp?.epochSecond?.let {
                    (it - System.currentTimeMillis() / 1000).coerceAtLeast(0)
                } ?: 30L

                Log.w(TAG, "Still PENDING after Proceed, retryAfter=${retryAfterSec}s")

                val pnv = PhoneNumberVerification(
                    null, System.currentTimeMillis(), 0, 0, null, null,
                    2,                   // verificationStatus 2 = FAILED (retryable)
                    retryAfterSec        // retryAfterSeconds
                )
                val response = VerifyPhoneNumberResponse(arrayOf(pnv), null)
                runCatching {
                    callbacks.onPhoneNumberVerificationsCompleted(Status.SUCCESS, response, null)
                }
            }
            else -> {
                Log.w(TAG, "Verification failed: state=${resultVerification.state}")
                val pnv = PhoneNumberVerification(
                    null, System.currentTimeMillis(), 0, 0, null, null,
                    9, 0  // verificationStatus 9 = DENIED
                )
                val response = VerifyPhoneNumberResponse(arrayOf(pnv), null)
                runCatching {
                    callbacks.onPhoneNumberVerificationsCompleted(Status.SUCCESS, response, null)
                }
            }
        }
    }

    // ---- Request building ----

    private fun buildSyncRequest(
        request: VerifyPhoneNumberRequest,
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair
    ): SyncRequest {
        val header = buildRequestHeader(sessionId, iidToken, androidId, dgResult, ecKeyPair)
        val verification = buildVerificationFromRequest(request)

        return SyncRequest(
            header_ = header,
            verifications = listOf(verification)
        )
    }

    private fun buildRequestHeader(
        sessionId: String,
        iidToken: String,
        androidId: Long,
        dgResult: String,
        ecKeyPair: java.security.KeyPair
    ): RequestHeader {
        val deviceId = DeviceId(
            iid_token = iidToken,
            device_android_id = androidId,
            user_android_id = androidId
        )

        val publicKeyBytes = ecKeyPair.public.encoded
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val clientInfo = ClientInfo(
            device_id = deviceId,
            client_public_key = okio.ByteString.of(*publicKeyBytes),
            locale = Locale.getDefault().toString(),
            gmscore_version_number = GMSCORE_VERSION_NUMBER,
            gmscore_version = GMSCORE_VERSION_STRING,
            android_sdk_version = Build.VERSION.SDK_INT,
            device_signals = DeviceSignals(
                droidguard_result = dgResult  // Field 2 — REQUIRED (the PR #3269 fix)
            ),
            country_info = buildCountryInfo(tm),
            connectivity_infos = buildConnectivityInfo(),
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            device_type = DeviceType.DEVICE_TYPE_PHONE,
            device_fingerprint = Build.FINGERPRINT,
            user_profile_type = UserProfileType.REGULAR_USER,
            has_read_privileged_phone_state_permission = false
        )

        return RequestHeader(
            client_info = clientInfo,
            session_id = sessionId,
            trigger = RequestTrigger(type = TriggerType.TRIGGER_TYPE_TRIGGER_API_CALL)
        )
    }

    private fun buildVerificationFromRequest(
        request: VerifyPhoneNumberRequest
    ): Verification {
        val extras = request.extras
        val policyId = request.upiPolicyId ?: ""
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Build SIM association
        val imsi = extras?.getString("IMSI") ?: getImsiSafe(tm)
        val simInfo = SIMInfo(
            imsi = if (imsi.isNotEmpty()) listOf(imsi) else emptyList(),
            iccid = getIccidSafe()
        )

        val subId = getActiveSubscriptionId()
        val simSlot = SIMSlot(
            index = getSlotIndex(subId),
            sub_id = subId
        )

        val association = VerificationAssociation(
            sim = SIMAssociation(sim_info = simInfo, sim_slot = simSlot)
        )

        // Build telephony info
        val telephonyInfo = buildTelephonyInfo(tm)

        // Build structured API params from the AIDL request
        val idTokenReq = request.idTokenRequest
        val structuredParams = StructuredAPIParams(
            policy_id = policyId,
            calling_package = packageName,
            id_token_request = if (idTokenReq != null) {
                org.microg.gms.phonenumberverification.IdTokenRequest(
                    certificate_hash = idTokenReq.hash ?: "",
                    token_nonce = idTokenReq.appId ?: ""
                )
            } else null,
            imsi_requests = request.imsis?.map { imsiReq ->
                IMSIRequest(
                    imsi = imsiReq.imsi ?: "",
                    phone_number_hint = imsiReq.carrierId
                )
            } ?: if (imsi.isNotEmpty()) listOf(IMSIRequest(imsi = imsi)) else emptyList()
        )

        // Build API params from the extras bundle
        val apiParams = mutableListOf<Param>()
        extras?.keySet()?.forEach { key ->
            extras.getString(key)?.let { value ->
                apiParams.add(Param(name = key, value_ = value))
            }
        }

        // Challenge preference with random localized message template for silent MT SMS
        val mtTemplate = Base64.encodeToString(
            ByteArray(8).also { java.security.SecureRandom().nextBytes(it) },
            Base64.NO_WRAP
        )

        return Verification(
            association = association,
            state = VerificationState.VERIFICATION_STATE_NONE,
            telephony_info = telephonyInfo,
            structured_api_params = structuredParams,
            api_params = apiParams,
            challenge_preference = ChallengePreference(
                mt_preference = MTChallengePreference(
                    localized_message_template = mtTemplate
                )
            )
        )
    }

    // ---- Device info helpers ----

    private fun buildTelephonyInfo(tm: TelephonyManager): TelephonyInfo {
        val simOperator = tm.simOperator ?: ""
        val simCountry = tm.simCountryIso ?: ""
        val networkOperator = tm.networkOperator ?: ""
        val networkCountry = tm.networkCountryIso ?: ""

        return TelephonyInfo(
            sim_state = if (tm.simState == TelephonyManager.SIM_STATE_READY)
                SIMState.SIM_READY else SIMState.SIM_NOT_READY,
            sim_operator = MobileOperatorInfo(
                country_code = simCountry,
                mcc_mnc = simOperator,
                operator_name = tm.simOperatorName ?: ""
            ),
            network_operator = MobileOperatorInfo(
                country_code = networkCountry,
                mcc_mnc = networkOperator,
                operator_name = tm.networkOperatorName ?: ""
            ),
            network_roaming = if (tm.isNetworkRoaming)
                RoamingState.ROAMING_STATE_ROAMING else RoamingState.ROAMING_STATE_NOT_ROAMING,
            sms_capability = SMSCapability.SMS_CAPABILITY_CAPABLE,
            service_state = ServiceState.SERVICE_STATE_IN_SERVICE,
            imei = getImeiSafe(tm),
            subscription_count = getActiveSubscriptionCount(),
            subscription_count_max = getMaxSubscriptionCount()
        )
    }

    private fun buildCountryInfo(tm: TelephonyManager): CountryInfo {
        val simCountry = tm.simCountryIso ?: ""
        val networkCountry = tm.networkCountryIso ?: ""
        return CountryInfo(
            sim_countries = if (simCountry.isNotEmpty()) listOf(simCountry) else emptyList(),
            network_countries = if (networkCountry.isNotEmpty()) listOf(networkCountry) else emptyList()
        )
    }

    private fun buildConnectivityInfo(): List<ConnectivityInfo> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return emptyList()

        val infos = mutableListOf<ConnectivityInfo>()
        val activeNetwork = cm.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

        if (caps != null) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                infos.add(ConnectivityInfo(
                    type = ConnectivityType.CONNECTIVITY_TYPE_WIFI,
                    state = ConnectivityState.CONNECTIVITY_STATE_CONNECTED,
                    availability = ConnectivityAvailability.CONNECTIVITY_AVAILABLE
                ))
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                infos.add(ConnectivityInfo(
                    type = ConnectivityType.CONNECTIVITY_TYPE_MOBILE,
                    state = ConnectivityState.CONNECTIVITY_STATE_CONNECTED,
                    availability = ConnectivityAvailability.CONNECTIVITY_AVAILABLE
                ))
            }
        }
        return infos
    }

    // ---- Token / Key helpers ----

    private suspend fun getDroidGuardResult(iidToken: String): String {
        val iidHash = sha256Hex(iidToken)
        val data = mapOf("iidHash" to iidHash, "rpc" to "sync")
        return try {
            DroidGuardClient.getResults(context, "constellation_verify", data).await()
        } catch (e: Exception) {
            Log.w(TAG, "DroidGuard failed, using empty result", e)
            ""
        }
    }

    private suspend fun getOrCreateIidToken(): String {
        // Generate a stable IID-like token for Constellation.
        // Real GMS uses a backend-issued IID/FCM token. We keep this stable per-install and
        // include checkin/android IDs so the value is deterministic across process restarts.
        return try {
            val prefs = context.getSharedPreferences("constellation_prefs", Context.MODE_PRIVATE)
            val cached = prefs.getString("iid_token", null)
            if (cached != null) return cached

            val gcmToken = getCachedGcmRegistrationToken()
            if (!gcmToken.isNullOrBlank()) {
                prefs.edit().putString("iid_token", gcmToken).apply()
                Log.i(TAG, "Using cached GCM registration token as IID")
                return gcmToken
            }

            val checkinInfo = LastCheckinInfo.read(context)
            val installationId = prefs.getString("installation_id", null) ?: UUID.randomUUID().toString().replace("-", "")
            if (prefs.getString("installation_id", null) == null) {
                prefs.edit().putString("installation_id", installationId).apply()
            }
            val raw = "$CONSTELLATION_AUTHORIZED_ENTITY:$installationId:${checkinInfo.androidId}"
            val digest = Base64.encodeToString(
                sha256Bytes(raw),
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
            )
            val token = "APA91b$digest"

            prefs.edit().putString("iid_token", token).apply()
            token
        } catch (e: Exception) {
            Log.w(TAG, "IID token generation fallback", e)
            val checkinInfo = LastCheckinInfo.read(context)
            "APA91b" + Base64.encodeToString(
                sha256Bytes("constellation:${checkinInfo.androidId}"),
                Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
            )
        }
    }

    private fun getCachedGcmRegistrationToken(): String? {
        // Read existing registration IDs from GCM database if available.
        // This is closer to a real IID/FCM identity than a synthetic token.
        return queryGcmRegistrationToken(packageName)
            ?: queryGcmRegistrationToken("com.google.android.gms")
    }

    @Suppress("UNCHECKED_CAST")
    private fun queryGcmRegistrationToken(targetPackage: String): String? {
        return try {
            val dbClass = Class.forName("org.microg.gms.gcm.GcmDatabase")
            val constructor = dbClass.getConstructor(Context::class.java)
            val database = constructor.newInstance(context)
            val getRegistrations = dbClass.getMethod("getRegistrationsByApp", String::class.java)
            val registrations = getRegistrations.invoke(database, targetPackage) as? List<Any>
            val latest = registrations
                ?.maxByOrNull { reg ->
                    runCatching { reg.javaClass.getField("timestamp").getLong(reg) }.getOrDefault(0L)
                }
            val token = latest
                ?.let { reg -> runCatching { reg.javaClass.getField("registerId").get(reg) as? String }.getOrNull() }
                ?.takeIf { it.isNotBlank() }
            runCatching { dbClass.getMethod("close").invoke(database) }
            token
        } catch (e: Exception) {
            Log.d(TAG, "No cached GCM registration token for $targetPackage", e)
            null
        }
    }

    private fun generateEcKeyPair(): java.security.KeyPair {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec("secp256r1"))
        return keyGen.generateKeyPair()
    }

    private fun getSpatulaHeaderSafe(): String? {
        return try {
            // AppCertServiceImpl exposes a non-suspend getSpatulaHeader(String) entrypoint.
            val implClass = Class.forName("org.microg.gms.auth.appcert.AppCertServiceImpl")
            val constructor = implClass.getDeclaredConstructor(Context::class.java)
            constructor.isAccessible = true
            val impl = constructor.newInstance(context)
            val method = implClass.getMethod("getSpatulaHeader", String::class.java)
            val header = method.invoke(impl, packageName) as? String
            header?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Spatula header unavailable", e)
            null
        }
    }

    private fun extractMsisdnToken(verification: Verification): String? {
        val tokenLikeKeys = setOf(
            "rcs_msisdn_token",
            "msisdn_token",
            "msisdntoken",
            "carrier_rcs_msisdn_token"
        )
        val fromApiParams = verification.api_params.firstNotNullOfOrNull { param ->
            val key = param.name.lowercase(Locale.US)
            if (key in tokenLikeKeys || (key.contains("msisdn") && key.contains("token"))) {
                param.value_.takeIf { it.isNotBlank() }
            } else null
        }
        if (!fromApiParams.isNullOrBlank()) return fromApiParams

        // Fallback: use the opaque verification token bytes from VERIFIED state.
        val verificationToken = verification.verification_info
            ?.verification_token
            ?.token
            ?.takeIf { it.size > 0 }
            ?.let {
                Base64.encodeToString(
                    it.toByteArray(),
                    Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
                )
            }
        return verificationToken?.takeIf { it.isNotBlank() }
    }

    // ---- Telephony helpers ----

    @Suppress("MissingPermission")
    private fun getImsiSafe(tm: TelephonyManager): String {
        return try { tm.subscriberId ?: "" } catch (_: Exception) { "" }
    }

    @Suppress("MissingPermission")
    private fun getImeiSafe(tm: TelephonyManager): String {
        return try {
            if (Build.VERSION.SDK_INT >= 26) tm.imei ?: "" else ""
        } catch (_: Exception) { "" }
    }

    @Suppress("MissingPermission")
    private fun getIccidSafe(): String {
        return try {
            if (Build.VERSION.SDK_INT >= 28) {
                val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                sm?.activeSubscriptionInfoList?.firstOrNull()?.iccId ?: ""
            } else ""
        } catch (_: Exception) { "" }
    }

    private fun getActiveSubscriptionId(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= 24) {
                SubscriptionManager.getDefaultSubscriptionId()
            } else 0
        } catch (_: Exception) { 0 }
    }

    private fun getActiveSubscriptionCount(): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            sm?.activeSubscriptionInfoCount ?: 1
        } catch (_: Exception) { 1 }
    }

    private fun getMaxSubscriptionCount(): Int {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            sm?.activeSubscriptionInfoCountMax ?: 1
        } catch (_: Exception) { 1 }
    }

    private fun getSlotIndex(subId: Int): Int {
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                SubscriptionManager.getSlotIndex(subId)
            } else 0
        } catch (_: Exception) { 0 }
    }

    // ---- Crypto helpers ----

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun sha256Bytes(input: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    }

}
