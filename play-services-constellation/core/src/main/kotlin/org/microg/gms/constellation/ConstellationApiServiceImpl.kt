/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.RemoteException
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import com.android.libraries.entitlement.ServiceEntitlementException
import com.android.libraries.entitlement.Ts43Authentication
import com.android.libraries.entitlement.Ts43Operation
import com.android.libraries.entitlement.odsa.AcquireTemporaryTokenOperation.AcquireTemporaryTokenRequest
import com.android.libraries.entitlement.utils.Ts43Constants
import com.google.android.gms.common.BuildConfig
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.PhoneNumberVerification
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.iid.InstanceID
import com.google.android.gms.tasks.await
import com.google.common.collect.ImmutableList
import com.squareup.wire.GrpcClient
import com.squareup.wire.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.core.VersionUtil
import org.microg.gms.gcm.GcmConstants
import org.microg.gms.phonenumberverification.CarrierIdCapability
import org.microg.gms.phonenumberverification.ChallengePreference
import org.microg.gms.phonenumberverification.ClientInfo
import org.microg.gms.phonenumberverification.ConnectivityAvailability
import org.microg.gms.phonenumberverification.ConnectivityInfo
import org.microg.gms.phonenumberverification.ConnectivityState
import org.microg.gms.phonenumberverification.ConnectivityType
import org.microg.gms.phonenumberverification.CountryInfo
import org.microg.gms.phonenumberverification.DeviceId
import org.microg.gms.phonenumberverification.DeviceSignals
import org.microg.gms.phonenumberverification.DeviceType
import org.microg.gms.phonenumberverification.IMSIRequest
import org.microg.gms.phonenumberverification.IdTokenRequest
import org.microg.gms.phonenumberverification.MTChallengePreference
import org.microg.gms.phonenumberverification.MobileOperatorInfo
import org.microg.gms.phonenumberverification.Param
import org.microg.gms.phonenumberverification.PhoneDeviceVerificationClient
import org.microg.gms.phonenumberverification.PhoneNumberSource
import org.microg.gms.phonenumberverification.PremiumSmsPermission
import org.microg.gms.phonenumberverification.RequestHeader
import org.microg.gms.phonenumberverification.RequestTrigger
import org.microg.gms.phonenumberverification.RoamingState
import org.microg.gms.phonenumberverification.SIMAssociation
import org.microg.gms.phonenumberverification.SIMInfo
import org.microg.gms.phonenumberverification.SIMSlot
import org.microg.gms.phonenumberverification.SIMState
import org.microg.gms.phonenumberverification.SMSCapability
import org.microg.gms.phonenumberverification.ServiceState
import org.microg.gms.phonenumberverification.ServiceStateEvent
import org.microg.gms.phonenumberverification.StructuredAPIParams
import org.microg.gms.phonenumberverification.SyncRequest
import org.microg.gms.phonenumberverification.TelephonyInfo
import org.microg.gms.phonenumberverification.TelephonyPhoneNumber
import org.microg.gms.phonenumberverification.TriggerType
import org.microg.gms.phonenumberverification.ChallengeResponse
import org.microg.gms.phonenumberverification.ProceedRequest
import org.microg.gms.phonenumberverification.ServerChallengeResponse
import org.microg.gms.phonenumberverification.Ts43ChallengeResponse
import org.microg.gms.phonenumberverification.Verification
import org.microg.gms.phonenumberverification.VerificationAssociation
import org.microg.gms.phonenumberverification.VerificationState
import java.net.URL
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Locale
import java.util.UUID

private const val API_KEY = "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk" // TODO: dedup

/*
TODO:
* what do we cache/store?
* key management for ClientAuth - maybe not needed? it's not in the traces i captured
* make SyncRequest not return invalid argument
    * is it the droidguard field names?
* implement getIidToken aidl api
* is devicekey management / AppCert needed or does the local spatula header work?
* allow user to input IMSI via copy/paste instead of requiring privileged permissions
 */

class ConstellationApiServiceImpl(
    private val context: Context,
    private val getSpatulaHeader: suspend (String) -> String?
) : IConstellationApiService.Stub() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val versionUtil = VersionUtil(context)
    private val keypair = genEcP256Keypair("gms-constellation-temp")

    companion object {
        private const val TAG = "ConstellationApi"
        private const val UPI_CARRIER_TOS_TS43 = "upi-carrier-tos-ts43"
        private const val PHONE_VERIFICATION_BASE_URL = "https://phonedeviceverification-pa.googleapis.com"
    }

    @Throws(RemoteException::class)
    override fun verifyPhoneNumber(
        callbacks: IConstellationCallbacks,
        request: VerifyPhoneNumberRequest,
        apiMetadata: ApiMetadata,
    ) {
        Log.d(TAG, "verifyPhoneNumber called: request=$request, apiMetadata=$apiMetadata")

        try {
            if (UPI_CARRIER_TOS_TS43 == request.upiPolicyId) {
                Log.d(TAG, "Processing UPI carrier TOS TS43 verification")
                // TODO: do we do TS43 on every call, or cache?
                scope.launch {
                    handleTs43Verification(callbacks, request, apiMetadata)
                }
            } else {
                Log.d(TAG, "Unknown verification for policy: ${request.upiPolicyId}")
                // Default response for non-TS43 verifications
                val pnv = PhoneNumberVerification().apply {
                    verificationStatus = 9
                }

                val response = VerifyPhoneNumberResponse().apply {
                    verifications = arrayOf(pnv)
                }
                Log.d(TAG, "replying with $response")
                callbacks.onPhoneNumberVerificationsCompleted(Status.SUCCESS, response, apiMetadata)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyPhoneNumber", e)
            callbacks.onPhoneNumberVerificationsCompleted(
                Status.INTERNAL_ERROR,
                VerifyPhoneNumberResponse(),
                apiMetadata
            )
        }
    }

    override fun getIidToken(
        callbacks: IConstellationCallbacks,
        request: GetIidTokenRequest,
        apiMetadata: ApiMetadata,
    ) {
        Log.d(TAG, "getIidToken called: request=$request, apiMetadata=$apiMetadata")

        val instanceId = InstanceID.getInstance(context)

        val iidToken = instanceId.getToken(
            "496232013492",// TODO: double check the authorizedEntity. Search "IidToken__asterism_project_number" in GmsCore
            GcmConstants.INSTANCE_ID_SCOPE_GCM
        )

        val signTimestamp = Instant.now()
        val signature = run {
            val instance = Signature.getInstance("SHA256withECDSA")
            instance.initSign(keypair.private)
            instance.update("${iidToken}:${signTimestamp.epochSecond}:${signTimestamp.nano}".toByteArray())
            instance.sign()
        }

        callbacks.onIidTokenGenerated(Status.SUCCESS, GetIidTokenResponse().apply {
            this.iidToken = iidToken
            this.fid = instanceId.id
            this.currentTimeMs = signTimestamp.toEpochMilli()
            this.clientSignature = signature
        }, apiMetadata)
    }

    @Throws(Exception::class)
    private suspend fun handleTs43Verification(
        callbacks: IConstellationCallbacks,
        request: VerifyPhoneNumberRequest,
        apiMetadata: ApiMetadata
    ) {
        val sessionId = UUID.randomUUID().toString()

        try {
            // Step 1: Send SyncRequest
            Log.d(TAG, "Sending SyncRequest via gRPC")
            val phoneVerificationClient = createGrpcClient()
            val syncRequest = createSyncRequest(sessionId, request)

            Log.d(TAG, "SyncRequest sent: ${syncRequest.toString().chunked(512).joinToString("\n")}")
            val syncResponse = withContext(Dispatchers.IO) {
                phoneVerificationClient.Sync().execute(syncRequest)
            }
            Log.d(TAG, "SyncResponse received: $syncResponse")

            val verificationResponse = syncResponse.responses[0]
            val verification = verificationResponse.verification
                ?: throw Exception("No verification in SyncResponse")

            if (verification.state != VerificationState.VERIFICATION_STATE_PENDING) {
                if (verification.state == VerificationState.VERIFICATION_STATE_VERIFIED) {
                    Log.d(TAG, "Already verified!")
                    val pnv = PhoneNumberVerification().apply {
                        verificationStatus = 1
                    }
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status.SUCCESS,
                        VerifyPhoneNumberResponse().apply { verifications = arrayOf(pnv) },
                        apiMetadata
                    )
                    return
                }
                throw Exception("Unexpected verification state: ${verification.state}")
            }

            // Step 2: Extract TS43 challenge from the pending verification
            val pendingInfo = verification.pending_verification_info
                ?: throw Exception("No pending_verification_info in PENDING verification")
            val challenge = pendingInfo.challenge
                ?: throw Exception("No challenge in pending_verification_info")
            val ts43Challenge = challenge.ts43_challenge

            Log.d(TAG, "Received challenge type: ${challenge.type}, ts43=${ts43Challenge != null}")

            if (ts43Challenge != null) {
                // Step 3: Perform TS43 auth flow using server-provided entitlement URL
                val entitlementUrl = ts43Challenge.entitlement_url
                Log.d(TAG, "TS43 entitlement URL from server: $entitlementUrl")

                val serverChallenge = ts43Challenge.server_challenge
                Log.d(TAG, "TS43 server challenge: $serverChallenge")

                val temporaryToken = performTs43AuthFlow(
                    entitlementUrl = entitlementUrl,
                    appId = ts43Challenge.app_id,
                    eapAkaRealm = ts43Challenge.eap_aka_realm,
                    serverChallenge = serverChallenge,
                    serviceEntitlementRequest = ts43Challenge.service_entitlement_request
                )

                // Step 4: Send ProceedRequest with the temporary token
                Log.d(TAG, "Sending ProceedRequest via gRPC")
                val proceedRequest = createProceedRequest(
                    verification = verification,
                    temporaryToken = temporaryToken,
                    ts43Type = ts43Challenge.ts43_type,
                    sessionId = sessionId
                )

                val proceedResponse = withContext(Dispatchers.IO) {
                    phoneVerificationClient.Proceed().execute(proceedRequest)
                }
                Log.d(TAG, "ProceedResponse received: $proceedResponse")

                val resultVerification = proceedResponse.verification
                if (resultVerification?.state == VerificationState.VERIFICATION_STATE_VERIFIED) {
                    Log.d(TAG, "Phone number verified successfully!")
                    val phoneNumber = resultVerification.verification_info?.phone_number
                    Log.d(TAG, "Verified phone number: $phoneNumber")

                    val pnv = PhoneNumberVerification().apply {
                        verificationStatus = 1
                    }
                    val response = VerifyPhoneNumberResponse().apply {
                        verifications = arrayOf(pnv)
                    }
                    callbacks.onPhoneNumberVerificationsCompleted(Status.SUCCESS, response, apiMetadata)
                } else {
                    Log.w(TAG, "Verification not completed: state=${resultVerification?.state}")
                    callbacks.onPhoneNumberVerificationsCompleted(
                        Status.INTERNAL_ERROR,
                        VerifyPhoneNumberResponse(),
                        apiMetadata
                    )
                }
            } else {
                // Non-TS43 challenge (MT SMS, MO SMS, etc.) - not yet supported
                Log.w(TAG, "Non-TS43 challenge type: ${challenge.type} - not yet implemented")
                callbacks.onPhoneNumberVerificationsCompleted(
                    Status.INTERNAL_ERROR,
                    VerifyPhoneNumberResponse(),
                    apiMetadata
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in TS43 verification", e)
            callbacks.onPhoneNumberVerificationsCompleted(
                Status.INTERNAL_ERROR,
                VerifyPhoneNumberResponse(),
                apiMetadata
            )
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private suspend fun createSyncRequest(ourSessionId: String, request: VerifyPhoneNumberRequest): SyncRequest {
        val subs = subscriptionManager.activeSubscriptionInfoList

        val settings = request.settings.keySet()
            .mapNotNull { k -> request.settings.getString(k)?.let { v -> k to v } }
            .toMap()

        return SyncRequest.build {
            verifications(
                subs?.map { sub ->
                    val tm = telephonyManager.createForSubscriptionId(sub.subscriptionId)

                    Verification.build {
                        state = VerificationState.VERIFICATION_STATE_NONE
                        association = VerificationAssociation.build {
                            sim = createSimAssociation(sub, tm)
                        }
                        telephony_info = createTelephonyInfo(sub, tm)
                        api_params = settings.map { (key, value) ->
                            Param.build {
                                name = key
                                value_ = value
                            }
                        }
                        challenge_preference = ChallengePreference.build {
                            mt_preference = MTChallengePreference.build {
                                val bytes = ByteArray(8)
                                SecureRandom().nextBytes(bytes)

                                localized_message_template = Base64.encodeToString(bytes, Base64.DEFAULT)
                            }
                        }
                        structured_api_params = StructuredAPIParams.build {
                            policy_id = request.upiPolicyId
                            id_token_request = IdTokenRequest.build {
                                certificate_hash = request.idTokenRequest.certificateHash
                                token_nonce = request.idTokenRequest.tokenNonce
                            }
                            PackageUtils.getCallingPackage(context)?.let { packageName -> // TODO: this is showing gms, not apps.messaging - get from verifyPhoneNumber params
                                Log.w(TAG, "Overriding calling package from $packageName to bugle")
//                                calling_package = packageName
                                calling_package = "com.google.android.apps.messaging"
                            }
                            imsi_requests = request.imsis.map {
                                IMSIRequest.build {
                                    imsi = it.imsi
                                    phone_number_hint = it.msisdn
                                }
                            }
                        }
                    }
                } ?: listOf()
            )
            header_(createRequestHeader(ourSessionId))
            verification_tokens = listOf()
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun createSimAssociation(sub: SubscriptionInfo, tm: TelephonyManager): SIMAssociation {
        val subId = sub.subscriptionId

        return SIMAssociation.build {
            sim_info = SIMInfo.build {
                imsi = listOf(tm.subscriberId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    sim_readable_number = "+" + subscriptionManager.getPhoneNumber(subId)
                    telephony_phone_number = listOf(
                        SubscriptionManager.PHONE_NUMBER_SOURCE_UICC,
                        SubscriptionManager.PHONE_NUMBER_SOURCE_IMS,
                        SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER,
                    )
                        .map { source -> source to subscriptionManager.getPhoneNumber(subId, source) }
                        .filter { it.second.isNotEmpty() }
                        .map { (source, number) ->
                            TelephonyPhoneNumber.build {
                                this.number = number
                                this.source = when (source) {
                                    SubscriptionManager.PHONE_NUMBER_SOURCE_UICC -> PhoneNumberSource.PHONE_NUMBER_SOURCE_IUCC
                                    SubscriptionManager.PHONE_NUMBER_SOURCE_IMS -> PhoneNumberSource.PHONE_NUMBER_SOURCE_IMS
                                    SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER -> PhoneNumberSource.PHONE_NUMBER_SOURCE_CARRIER
                                    else -> PhoneNumberSource.PHONE_NUMBER_SOURCE_UNKNOWN
                                }
                            }
                        }
                }
                iccid = sub.iccId
            }
            sim_slot = SIMSlot.build {
                sub_id = subId
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createTelephonyInfo(sub: SubscriptionInfo, tm: TelephonyManager): TelephonyInfo {
        return TelephonyInfo.build {
            sim_state = SIMState.SIM_NOT_READY // just based on what I saw from GMS
            group_id_level1 = tm.groupIdLevel1
            sim_operator = MobileOperatorInfo.build {
                country_code = tm.simCountryIso
                mcc_mnc = tm.simOperator
                operator_name = tm.simOperatorName
            }
            network_operator = MobileOperatorInfo.build {
                country_code = tm.networkCountryIso
                mcc_mnc = tm.networkOperator
                operator_name = tm.networkOperatorName
            }
            network_roaming = when (tm.isNetworkRoaming) {
                true -> RoamingState.ROAMING_STATE_ROAMING
                false -> RoamingState.ROAMING_STATE_NOT_ROAMING
            }
            data_roaming = when (tm.isDataRoamingEnabled) {
                true -> RoamingState.ROAMING_STATE_ROAMING
                false -> RoamingState.ROAMING_STATE_NOT_ROAMING
            }
            sms_capability = SMSCapability.SMS_CAPABILITY_USER_RESTRICTED // TODO: fix
//            sms_capability = when (tm.isDeviceSmsCapable) {
//                true -> SMSCapability.SMS_CAPABILITY_CAPABLE
//                false -> SMSCapability.SMS_CAPABILITY_INCAPABLE
//            }
            carrier_id_capability = CarrierIdCapability.CARRIER_ID_INCAPABLE
            premium_sms_permission = PremiumSmsPermission.PREMIUM_SMS_PERMISSION_GRANTED
            subscription_count = subscriptionManager.activeSubscriptionInfoCount
            subscription_count_max = subscriptionManager.activeSubscriptionInfoCountMax
            sim_index = sub.simSlotIndex
            imei = telephonyManager.getImei(sub.simSlotIndex)
            service_state = when (tm.serviceState?.state) {
                android.telephony.ServiceState.STATE_IN_SERVICE -> ServiceState.SERVICE_STATE_IN_SERVICE
                android.telephony.ServiceState.STATE_OUT_OF_SERVICE -> ServiceState.SERVICE_STATE_OUT_OF_SERVICE
                android.telephony.ServiceState.STATE_EMERGENCY_ONLY -> ServiceState.SERVICE_STATE_EMERGENCY_ONLY
                android.telephony.ServiceState.STATE_POWER_OFF -> ServiceState.SERVICE_STATE_POWER_OFF
                else -> ServiceState.SERVICE_STATE_UNKNOWN
            }
            service_state_events = listOf(
                ServiceStateEvent.build {
                    voice_registration_state = 1
                    data_registration_state = 1
                    event_timestamp = Instant.now().minusSeconds(86400)
                }
            )
            sim_carrier_id = tm.simCarrierId
        }
    }

    @Throws(ServiceEntitlementException::class)
    private fun performTs43AuthFlow(
        entitlementUrl: String,
        appId: String?,
        eapAkaRealm: String?,
        serverChallenge: org.microg.gms.phonenumberverification.ServerChallenge?,
        serviceEntitlementRequest: org.microg.gms.phonenumberverification.ServiceEntitlementRequest?
    ): String {
        try {
            val slotIndex = 0 // Default to first slot
            val url = URL(entitlementUrl)
            val entitlementVersion = serviceEntitlementRequest?.entitlement_version ?: "9.0"

            Log.d(TAG, "TS43 auth: url=$url, appId=$appId, realm=$eapAkaRealm, version=$entitlementVersion")

            // Step 1: Perform EAP-AKA authentication with the carrier's entitlement server
            val ts43Auth = Ts43Authentication(context, url, entitlementVersion)
            val resolvedAppId = appId ?: Ts43Constants.APP_PHONE_NUMBER_INFORMATION

            val authToken = ts43Auth.getAuthToken(
                slotIndex,
                resolvedAppId,
                context.packageName,
                "1.0"
            )
            Log.d(TAG, "TS43 auth token acquired successfully")

            // Step 2: Use the auth token to perform ODSA operation
            val ts43Operation = Ts43Operation(
                context,
                slotIndex,
                url,
                entitlementVersion,
                authToken.token(),
                Ts43Operation.TOKEN_TYPE_NORMAL,
                context.packageName
            )

            // Determine operation targets from server challenge
            val operationTargets = serverChallenge?.acquire_temporary_token_operation?.operation_targets
                ?: listOf("GetSubscriberInfo")

            val tokenRequest = AcquireTemporaryTokenRequest.builder()
                .setAppId(resolvedAppId)
                .setOperationTargets(ImmutableList.copyOf(operationTargets))
                .build()

            val tokenResponse = ts43Operation.acquireTemporaryToken(tokenRequest)

            Log.d(TAG, "Acquired TS43 temporary token successfully")
            return tokenResponse.temporaryToken()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform TS43 auth flow", e)
            throw ServiceEntitlementException(
                ServiceEntitlementException.ERROR_TOKEN_NOT_AVAILABLE,
                "TS43 auth failed: ${e.message}",
                e
            )
        }
    }

    private suspend fun createProceedRequest(
        verification: Verification,
        temporaryToken: String,
        ts43Type: org.microg.gms.phonenumberverification.Ts43Type?,
        sessionId: String
    ): ProceedRequest {
        return ProceedRequest.build {
            // Pass back the verification from SyncResponse (in PENDING state)
            this.verification = verification
            // Build the challenge response with the TS43 temporary token
            challenge_response = ChallengeResponse.build {
                ts43_challenge_response = Ts43ChallengeResponse.build {
                    this.ts43_type = ts43Type
                    server_challenge_response = ServerChallengeResponse.build {
                        this.temporary_token = temporaryToken
                    }
                }
            }
            header_ = createRequestHeader(sessionId)
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private suspend fun createRequestHeader(sessionId: String): RequestHeader {
        val instanceId = InstanceID.getInstance(context)

        val iidToken = instanceId.getToken(
            "496232013492",// TODO: double check the authorizedEntity. Search "IidToken__asterism_project_number" in GmsCore
            GcmConstants.INSTANCE_ID_SCOPE_GCM
        )

        val hasher = MessageDigest.getInstance("SHA-256")
        hasher.update(iidToken.toByteArray())
        val iidHash = Base64.encodeToString(hasher.digest(), Base64.NO_PADDING or Base64.NO_WRAP)

        val droidguardResult = DroidGuardClient.getResults(
            context, "constellation_verify", hashMapOf(
                "iidHash" to iidHash,
                "rpc" to "sync"
            )
        ).await()

        val deviceId = DeviceId.build {
            iid_token = iidToken

            val id = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID,
            ).toLong(16)
            device_android_id = id // TODO: get the *primary* user id
//                    device_user_id = ???
            user_android_id = id
        }

        // ECDSA signature of a SHA256 hash of "device_id.iid_token:sign_timestamp.seconds:sign_timestamp.nanos"
        val signTimestamp = Instant.now()
        val signature = run {
            val instance = Signature.getInstance("SHA256withECDSA")
            instance.initSign(keypair.private)
            instance.update("${iidToken}:${signTimestamp.epochSecond}:${signTimestamp.nano}".toByteArray())
            instance.sign()
        }

        return RequestHeader.build {
            client_info = ClientInfo.build {
                device_id = deviceId
                client_public_key = keypair.public.encoded.toByteString()
                locale = Locale.getDefault().toString()
                gmscore_version_number = BuildConfig.VERSION_CODE / 1000
                gmscore_version = versionUtil.versionString
                android_sdk_version = Build.VERSION.SDK_INT
                device_signals = DeviceSignals.build {
                    droidguard_result = droidguardResult
                    droidguard_token = droidguardResult
                }
                has_read_privileged_phone_state_permission = true
                country_info = CountryInfo.build {
                    sim_countries = listOf(telephonyManager.simCountryIso)
                    network_countries = listOf(telephonyManager.networkCountryIso)
                }
                connectivity_infos = connectivityManager.allNetworks // TODO: why do we see 3?
                    .mapNotNull { net ->
                        val cap = connectivityManager.getNetworkCapabilities(net)
                            ?: return@mapNotNull null
                        val info = connectivityManager.getNetworkInfo(net)
                            ?: return@mapNotNull null
                        ConnectivityInfo.build {
                            type =
                                if (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                                    ConnectivityType.CONNECTIVITY_TYPE_WIFI
                                else if (cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                                    ConnectivityType.CONNECTIVITY_TYPE_MOBILE
                                else
                                    ConnectivityType.CONNECTIVITY_TYPE_UNKNOWN
                            state = when (info.state) {
                                NetworkInfo.State.CONNECTING -> ConnectivityState.CONNECTIVITY_STATE_CONNECTING
                                NetworkInfo.State.CONNECTED -> ConnectivityState.CONNECTIVITY_STATE_CONNECTED
                                NetworkInfo.State.SUSPENDED -> ConnectivityState.CONNECTIVITY_STATE_SUSPENDED
                                NetworkInfo.State.DISCONNECTING -> ConnectivityState.CONNECTIVITY_STATE_DISCONNECTING
                                NetworkInfo.State.DISCONNECTED -> ConnectivityState.CONNECTIVITY_STATE_DISCONNECTED
                                NetworkInfo.State.UNKNOWN -> ConnectivityState.CONNECTIVITY_STATE_UNKNOWN
                            }
                            availability = when (info.isAvailable) {
                                true -> ConnectivityAvailability.CONNECTIVITY_AVAILABLE
                                false -> ConnectivityAvailability.CONNECTIVITY_NOT_AVAILABLE
                            }
                        }
                    }
                model = Build.MODEL
                manufacturer = Build.MANUFACTURER
                device_type = DeviceType.DEVICE_TYPE_PHONE // is this always true?
                device_fingerprint = Build.FINGERPRINT
            }
            /*
            client_auth = ClientAuth.build {
                device_id = deviceId
                client_sign = signature.toByteString()
                sign_timestamp = signTimestamp
            }
             */
            session_id = sessionId
            trigger = RequestTrigger.build {
                type = TriggerType.TRIGGER_TYPE_TRIGGER_API_CALL
            }
        }
    }

    private suspend fun createGrpcClient(): PhoneDeviceVerificationClient {
         // Get spatula header for the calling package
         val callingPackage = PackageUtils.getCallingPackage(context)
             ?: throw IllegalStateException("No calling package in AIDL call")

         val spatulaHeader = withContext(Dispatchers.IO) {
             getSpatulaHeader(callingPackage)
                 ?: throw IllegalStateException("Failed to generate spatula header for $callingPackage")
         }

         // Create logging interceptor for debugging
         val loggingInterceptor = HttpLoggingInterceptor { message ->
             Log.d(TAG, "OkHttp: $message")
         }.apply {
             level = HttpLoggingInterceptor.Level.BODY
         }

        val trailerLoggingInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            println("Trailers: ${response.trailers()}")
            response
        }

         val client = OkHttpClient.Builder()
             .addInterceptor { chain ->
                 val originalRequest = chain.request()
                 val builder = originalRequest.newBuilder()
                     .header("x-goog-api-key", API_KEY)
                     .header("x-android-package", Constants.GMS_PACKAGE_NAME)
                     .header("x-android-cert", Constants.GMS_PACKAGE_SIGNATURE_SHA1)
                     .header("x-goog-spatula", spatulaHeader)

                 chain.proceed(builder.build())
             }
             .addInterceptor(loggingInterceptor)
             .addInterceptor(trailerLoggingInterceptor)
             .build()

         val grpcClient = GrpcClient.Builder()
             .client(client)
             .baseUrl(PHONE_VERIFICATION_BASE_URL)
             .build()

         return grpcClient.create(PhoneDeviceVerificationClient::class)
    }

    private fun genEcP256Keypair(alias: String): java.security.KeyPair {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore"
        )
        kpg.initialize(
            KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")) // aka prime256v1
                .setDigests(
                    KeyProperties.DIGEST_SHA256,
                    KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512
                )
                .build()
        )
        return kpg.generateKeyPair()
    }

}