/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import google.internal.communications.phonedeviceverification.v1.*
import okio.ByteString

private const val TAG = "GmsConstellationProto"

@Suppress("DEPRECATION")
private fun ConnectivityManager.allNetworksCompat(): Array<android.net.Network> = allNetworks

@Suppress("DEPRECATION")
private fun SubscriptionInfo.mccCompat(): Int = mcc

@Suppress("DEPRECATION")
private fun SubscriptionInfo.mncCompat(): Int = mnc

data class TelephonyData(
    val simCountry: String,
    val networkCountry: String,
    val simOperator: String,
    val networkOperator: String,
    val groupIdLevel1: String,
    val imei: String,
    val iccId: String,
    val phoneTypeInt: Int,
    val dataRoamingInt: Int,
    val networkRoamingInt: Int,
    val smsCapabilityInt: Int,
    val activeSubCount: Int,
    val maxSubCount: Int,
    val simSlotIndex: Int,
    val subId: Int,
    val carrierIdCapabilityInt: Int,
    val smsNoConfirmInt: Int,
    val simStateEnum: Int,
    val serviceStateEnum: Int,
    val isEmbedded: Boolean,
    val carrierId: Long,
    val simOperatorName: String,
    val networkOperatorName: String,
    val subscriptionInfo: SubscriptionInfo?,
    val telephonyManagerSub: TelephonyManager?,
)

data class RequestProtoContext(
    val iidToken: String,
    val deviceAndroidId: Long,
    val userAndroidId: Long,
    val publicKeyBytes: ByteString?,
    val localeStr: String,
    val gmscoreVersionNumber: Int,
    val gmscoreVersion: String,
    val registeredAppIds: List<StringId>,
    val countryInfo: CountryInfo,
    val connectivityInfos: List<ConnectivityInfo>,
    val telephonyInfoContainer: TelephonyInfoContainer?,
)

fun gatherConnectivityInfos(context: Context): List<ConnectivityInfo> {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val result = mutableListOf<ConnectivityInfo>()
    try {
        val bestByType = linkedMapOf<ConnectivityType, ConnectivityInfo>()
        connectivityManager?.allNetworksCompat()?.forEach { network ->
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return@forEach
            val connType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectivityType.CONNECTIVITY_TYPE_WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectivityType.CONNECTIVITY_TYPE_MOBILE
                else -> ConnectivityType.CONNECTIVITY_TYPE_UNKNOWN
            }
            if (connType == ConnectivityType.CONNECTIVITY_TYPE_UNKNOWN) return@forEach

            val connState = when {
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> ConnectivityState.CONNECTIVITY_STATE_CONNECTED
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> ConnectivityState.CONNECTIVITY_STATE_CONNECTING
                else -> ConnectivityState.CONNECTIVITY_STATE_UNKNOWN
            }
            val connAvail = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                ConnectivityAvailability.CONNECTIVITY_AVAILABLE
            } else {
                ConnectivityAvailability.CONNECTIVITY_NOT_AVAILABLE
            }
            val newInfo = ConnectivityInfo(
                type = connType,
                state = connState,
                availability = connAvail
            )
            val oldInfo = bestByType[connType]
            if (oldInfo == null || newInfo.state.value > oldInfo.state.value) {
                bestByType[connType] = newInfo
            }
        }
        result.addAll(bestByType.values)
    } catch (e: SecurityException) {
        Log.w(TAG, "Could not get connectivity info", e)
    }
    Log.d(TAG, "ConnectivityInfos: ${result.size} networks")
    return result
}

fun gatherTelephonyData(
    context: Context,
    targetImsi: String?,
    targetMsisdn: String?
): TelephonyData {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val allSubs = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
    val subscriptionInfo = if (targetImsi != null && allSubs.size > 1) {
        allSubs.find { sub ->
            val subMcc = if (Build.VERSION.SDK_INT >= 29) sub.mccString?.toIntOrNull() else sub.mccCompat()
            val subMnc = if (Build.VERSION.SDK_INT >= 29) sub.mncString?.toIntOrNull() else sub.mncCompat()
            val subMccMnc = "${subMcc ?: -1}${String.format("%02d", subMnc ?: -1)}"
            targetImsi.startsWith(subMccMnc)
        } ?: allSubs.find { sub ->
            @Suppress("DEPRECATION")
            targetMsisdn != null && sub.number != null && sub.number.isNotEmpty() &&
                (targetMsisdn.endsWith(sub.number.takeLast(8)) || sub.number.endsWith(targetMsisdn.takeLast(8)))
        } ?: allSubs.firstOrNull().also {
            Log.w(TAG, "Could not match IMSI $targetImsi to any subscription, using first")
        }
    } else {
        allSubs.firstOrNull()
    }

    val subId = subscriptionInfo?.subscriptionId ?: SubscriptionManager.INVALID_SUBSCRIPTION_ID
    val matchedMcc = if (Build.VERSION.SDK_INT >= 29) subscriptionInfo?.mccString else subscriptionInfo?.mccCompat()?.toString()
    val matchedMnc = if (Build.VERSION.SDK_INT >= 29) subscriptionInfo?.mncString else subscriptionInfo?.mncCompat()?.toString()
    Log.d(TAG, "Subscription match: target IMSI=${targetImsi?.take(5)}***, matched subId=${subscriptionInfo?.subscriptionId}, slot=${subscriptionInfo?.simSlotIndex}, mcc=$matchedMcc, mnc=$matchedMnc")

    val telephonyManagerSub = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        try {
            telephonyManager?.createForSubscriptionId(subId) ?: telephonyManager
        } catch (e: Exception) {
            telephonyManager
        }
    } else {
        telephonyManager
    }

    val simCountry = telephonyManager?.simCountryIso?.lowercase(java.util.Locale.ROOT) ?: ""
    val networkCountry = telephonyManager?.networkCountryIso?.lowercase(java.util.Locale.ROOT) ?: ""
    val simOperatorStr = telephonyManagerSub?.simOperator ?: ""
    val networkOperatorStr = telephonyManagerSub?.networkOperator ?: ""
    val groupIdLevel1 = try {
        telephonyManagerSub?.groupIdLevel1 ?: ""
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission for GroupIdLevel1")
        ""
    }
    val imei = try {
        @Suppress("DEPRECATION")
        telephonyManagerSub?.deviceId ?: ""
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission for IMEI")
        ""
    }
    val iccId = try {
        subscriptionInfo?.iccId
            ?: run {
                @Suppress("DEPRECATION")
                telephonyManagerSub?.simSerialNumber
            }
            ?: ""
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission for ICCID")
        ""
    }

    // TelephonyInfo fields - verified against v26 bfqi.java:d()
    val phoneTypeInt = when (telephonyManagerSub?.phoneType) {
        TelephonyManager.PHONE_TYPE_GSM -> 1
        TelephonyManager.PHONE_TYPE_CDMA -> 2
        TelephonyManager.PHONE_TYPE_SIP -> 3
        else -> 0
    }
    val dataRoamingInt = if (telephonyManagerSub?.isNetworkRoaming == true) 2 else 1

    val activeNetwork = connectivityManager?.activeNetwork
    val activeNetworkCaps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
    val networkRoamingInt = when {
        activeNetworkCaps == null -> 0
        activeNetworkCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) -> 1
        else -> 2
    }

    val hasReadSms = context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasSendSms = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val smsCapabilityInt = if (!hasReadSms || !hasSendSms) {
        3 // DEFAULT_CAPABILITY (no SMS perms, GMS: i2=5, 5-2=3)
    } else {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
        val userRestricted = userManager?.userRestrictions?.getBoolean("no_sms") == true
        @Suppress("DEPRECATION")
        val isSmsCapable = telephonyManagerSub?.isSmsCapable == true
        if (userRestricted) 4 else if (!isSmsCapable) 1 else 2
    }

    val activeSubCount = subscriptionManager?.activeSubscriptionInfoCount ?: 1
    val maxSubCount = subscriptionManager?.activeSubscriptionInfoCountMax ?: 1
    val simSlotIndex = subscriptionInfo?.simSlotIndex ?: 0

    val hasPrivilegedPhoneState = context.checkSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasCarrierIdCapability = if (!hasPrivilegedPhoneState || telephonyManagerSub == null) {
        false
    } else {
        try {
            telephonyManagerSub.javaClass.getMethod("getIccAuthentication", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            true
        } catch (e: Exception) {
            try {
                telephonyManagerSub.javaClass.getMethod("getIccSimChallengeResponse", Int::class.javaPrimitiveType, String::class.java)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
    val carrierIdCapabilityInt = if (hasCarrierIdCapability) 2 else 1

    val smsNoConfirmGranted = context.checkSelfPermission("android.permission.SEND_SMS_NO_CONFIRMATION") == android.content.pm.PackageManager.PERMISSION_GRANTED
    val smsNoConfirmInt = if (smsNoConfirmGranted) 2 else 1

    val simStateEnum = if (telephonyManagerSub?.simState == TelephonyManager.SIM_STATE_READY) 2 else 1

    val serviceStateEnum = try {
        when (telephonyManagerSub?.serviceState?.state) {
            ServiceState.STATE_IN_SERVICE -> 1
            ServiceState.STATE_OUT_OF_SERVICE -> 2
            ServiceState.STATE_EMERGENCY_ONLY -> 3
            ServiceState.STATE_POWER_OFF -> 4
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    val isEmbedded = subscriptionInfo?.isEmbedded ?: false
    val carrierId = try {
        telephonyManagerSub?.simCarrierId?.toLong() ?: -1L
    } catch (e: Exception) {
        -1L
    }

    return TelephonyData(
        simCountry = simCountry,
        networkCountry = networkCountry,
        simOperator = simOperatorStr,
        networkOperator = networkOperatorStr,
        groupIdLevel1 = groupIdLevel1,
        imei = imei,
        iccId = iccId,
        phoneTypeInt = phoneTypeInt,
        dataRoamingInt = dataRoamingInt,
        networkRoamingInt = networkRoamingInt,
        smsCapabilityInt = smsCapabilityInt,
        activeSubCount = activeSubCount,
        maxSubCount = maxSubCount,
        simSlotIndex = simSlotIndex,
        subId = subId,
        carrierIdCapabilityInt = carrierIdCapabilityInt,
        smsNoConfirmInt = smsNoConfirmInt,
        simStateEnum = simStateEnum,
        serviceStateEnum = serviceStateEnum,
        isEmbedded = isEmbedded,
        carrierId = carrierId,
        simOperatorName = telephonyManager?.simOperatorName ?: "",
        networkOperatorName = telephonyManager?.networkOperatorName ?: "",
        subscriptionInfo = subscriptionInfo,
        telephonyManagerSub = telephonyManagerSub,
    )
}

// ---- Proto building functions ----

/**
 * Build CountryInfo from telephony data.
 */
fun buildCountryInfo(td: TelephonyData): CountryInfo {
    return CountryInfo(
        sim_countries = if (td.simCountry.isNotEmpty()) listOf(td.simCountry) else emptyList(),
        network_countries = if (td.networkCountry.isNotEmpty()) listOf(td.networkCountry) else emptyList()
    )
}

/**
 * Build TelephonyInfo proto from gathered telephony data.
 * Verified against GMS v26 bfqi.java:d().
 */
fun buildTelephonyInfo(td: TelephonyData): TelephonyInfo {
    return TelephonyInfo(
        phone_type = td.phoneTypeInt,
        group_id_level1 = td.groupIdLevel1,
        sim_country = MobileOperatorCountry(
            country_iso = td.simCountry,
            mcc_mnc = td.simOperator,
            operator_name = td.simOperatorName,
            nil_since_millis = 0
        ),
        network_country = MobileOperatorCountry(
            country_iso = td.networkCountry,
            mcc_mnc = td.networkOperator,
            operator_name = td.networkOperatorName,
            nil_since_millis = 0
        ),
        data_roaming = td.dataRoamingInt,
        network_roaming = td.networkRoamingInt,
        sms_capability = td.smsCapabilityInt,
        subscription_count = td.activeSubCount,
        subscription_count_max = td.maxSubCount,
        eap_aka_capability = td.carrierIdCapabilityInt,
        sms_no_confirm_capability = td.smsNoConfirmInt,
        sim_state = td.simStateEnum,
        imei = td.imei.takeIf { it.isNotEmpty() },
        service_state = td.serviceStateEnum,
        is_embedded = td.isEmbedded,
        sim_carrier_id = td.carrierId
    )
}

/**
 * Build TelephonyInfoContainer from Gaia IDs.
 * GMS sends this with Gaia IDs (field 20 of ClientInfo).
 */
fun buildTelephonyInfoContainer(gaiaIds: List<String>): TelephonyInfoContainer? {
    if (gaiaIds.isEmpty()) return null
    val nowMillis = System.currentTimeMillis()
    val entries = gaiaIds.map { gaiaId ->
        TelephonyInfoEntry(
            gaia_id = gaiaId,
            state = 1,  // State=1 in all captured GMS traffic
            timestamp = Timestamp(
                seconds = nowMillis / 1000,
                nanos = ((nowMillis % 1000) * 1_000_000).toInt()
            )
        )
    }
    return TelephonyInfoContainer(entries = entries)
}

/**
 * Build SIMInfo for a verification's SIMAssociation.
 *
 * listOf("") encodes as field-present-but-empty on wire,
 * causing server error "imsi[0] empty". Send emptyList() when IMSI is blank.
 */
fun buildSIMInfo(
    imsi: String,
    msisdn: String,
    iccId: String?
): SIMInfo {
    return SIMInfo(
        imsi = listOf(imsi).filter { it.isNotEmpty() },
        sim_readable_number = msisdn,
        iccid = iccId ?: ""
    )
}

/**
 * Build the common ClientInfo used in all request types (GetConsent, SetConsent, Sync, Proceed).
 * All requests use the same structure, differing only in device_signals (DG token) and
 * whether experiment_infos is included.
 *
 * @param droidGuardToken DG token string, null for empty DeviceSignals, absent for null device_signals
 * @param includeDeviceSignals false to set device_signals=null (SetConsent without DG)
 * @param includeExperimentInfos true for Sync requests (stock includes empty list), false otherwise
 */
fun buildClientInfo(
    ctx: RequestProtoContext,
    droidGuardToken: String?,
    includeDeviceSignals: Boolean = true,
    includeExperimentInfos: Boolean = false
): ClientInfo {
    val deviceSignals = if (!includeDeviceSignals) {
        null
    } else if (droidGuardToken != null) {
        DeviceSignals(droidguard_token = droidGuardToken)
    } else {
        DeviceSignals()
    }

    return ClientInfo(
        device_id = DeviceId(
            iid_token = ctx.iidToken,
            device_android_id = ctx.deviceAndroidId,
            user_android_id = ctx.userAndroidId
        ),
        client_public_key = ctx.publicKeyBytes ?: ByteString.EMPTY,
        locale = ctx.localeStr,
        gmscore_version_number = ctx.gmscoreVersionNumber,
        gmscore_version = ctx.gmscoreVersion,
        android_sdk_version = Build.VERSION.SDK_INT,
        device_signals = deviceSignals,
        has_read_privileged_phone_state_permission = 1,
        registered_app_ids = ctx.registeredAppIds,
        country_info = ctx.countryInfo,
        connectivity_infos = ctx.connectivityInfos,
        is_standalone_device = false,
        telephony_info_container = ctx.telephonyInfoContainer,
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        device_fingerprint = Build.FINGERPRINT,
        device_type = DeviceType.DEVICE_TYPE_PHONE,
        experiment_infos = if (includeExperimentInfos) emptyList() else emptyList()
    )
}

/**
 * Build RequestHeader wrapping a ClientInfo.
 */
fun buildRequestHeader(
    sessionId: String,
    clientInfo: ClientInfo,
    clientCredentials: ClientCredentials? = null,
    trigger: RequestTrigger = RequestTrigger(type = TriggerType.TRIGGER_TYPE_TRIGGER_API_CALL)
): RequestHeader {
    return RequestHeader(
        session_id = sessionId,
        client_info = clientInfo,
        client_credentials = clientCredentials,
        trigger = trigger
    )
}

/**
 * Build VerificationMethodInfo for Sync requests.
 * Stock GMS sends EMPTY methods list (Phenotype default=0).
 */
fun buildVerificationMethodInfo(smsToken: String): VerificationMethodInfo {
    return VerificationMethodInfo(
        methods = emptyList(),
        data_ = VerificationMethodData(value_ = smsToken)
    )
}

/**
 * Build the full SyncRequest proto.
 *
 * @param sessionId UUID session identifier
 * @param ctx common request context (IID, keys, version, etc.)
 * @param syncToken DG token for Sync (may be ARfb cached or raw)
 * @param syncClientCredentials ECDSA client credentials (null if key not yet acked)
 * @param verification the Verification message (contains SIM info, telephony, carrier, params)
 * @param verificationTokens loaded from SharedPreferences
 */
fun buildSyncRequest(
    sessionId: String,
    ctx: RequestProtoContext,
    syncToken: String?,
    syncClientCredentials: ClientCredentials?,
    verification: Verification,
    verificationTokens: List<VerificationToken>
): SyncRequest {
    val clientInfo = buildClientInfo(
        ctx = ctx,
        droidGuardToken = syncToken,
        includeExperimentInfos = true
    )
    return SyncRequest(
        verifications = listOf(verification),
        header_ = buildRequestHeader(
            sessionId = sessionId,
            clientInfo = clientInfo,
            clientCredentials = syncClientCredentials
        ),
        verification_tokens = verificationTokens
    )
}

/**
 * Build a Verification message for the SyncRequest.
 */
fun buildVerification(
    simInfo: SIMInfo,
    simAssociationIdentifiers: List<StringId>,
    simSlotIndex: Int,
    subId: Int,
    telephonyInfo: TelephonyInfo,
    params: List<Param>,
    verificationMethodInfo: VerificationMethodInfo,
    carrierInfo: CarrierInfo
): Verification {
    return Verification(
        association = VerificationAssociation(
            sim = SIMAssociation(
                sim_info = simInfo,
                identifiers = simAssociationIdentifiers,
                sim_slot = SIMSlot(
                    index = simSlotIndex,
                    sub_id = subId
                )
            )
        ),
        state = VerificationState.VERIFICATION_STATE_NONE,
        telephony_info = telephonyInfo,
        params = params,
        verification_method_info = verificationMethodInfo,
        carrier_info = carrierInfo
    )
}

/**
 * Build the GetConsentRequest proto.
 *
 * CRITICAL: GMS sets DeviceId at TOP LEVEL (field 1) even though proto says DEPRECATED.
 * bekg.java:492-509: ONLY sets iid_token, omits android_id fields.
 */
fun buildGetConsentRequest(
    sessionId: String,
    ctx: RequestProtoContext,
    getConsentToken: String?,
    registeredAppIds: List<StringId>,
    params: List<Param>
): GetConsentRequest {
    val clientInfo = buildClientInfo(
        ctx = ctx,
        droidGuardToken = getConsentToken
    )
    return GetConsentRequest(
        device_id = DeviceId(iid_token = ctx.iidToken),
        gaia_ids = registeredAppIds,
        header_ = buildRequestHeader(
            sessionId = sessionId,
            clientInfo = clientInfo
        ),
        api_params = params,
        include_asterism_consents = AsterismClient.ASTERISM_CLIENT_UNKNOWN,
        asterism_client_bool = true,
        imei = null
    )
}

/**
 * Build the SetConsentRequest proto.
 * API doc says DG only required for Sync/Proceed, not SetConsent.
 *
 * @param dgToken DG token string, or null for no-DG attempt
 */
fun buildSetConsentRequest(
    sessionId: String,
    ctx: RequestProtoContext,
    dgToken: String?
): SetConsentRequest {
    val clientInfo = buildClientInfo(
        ctx = ctx,
        droidGuardToken = dgToken,
        includeDeviceSignals = dgToken != null
    )
    return SetConsentRequest(
        header_ = buildRequestHeader(
            sessionId = sessionId,
            clientInfo = clientInfo
        ),
        asterism_client = AsterismClient.ASTERISM_CLIENT_RCS,
        device_verification_consent = DeviceVerificationConsent(
            consent_value = ConsentValue.CONSENT_VALUE_CONSENTED,
            consent_source = DeviceVerificationConsentSource.DEVICE_VERIFICATION_CONSENT_SOURCE_ANDROID_DEVICE_SETTINGS,
            consent_version = DeviceVerificationConsentVersion.DEVICE_VERIFICATION_CONSENT_VERSION_PHONE_VERIFICATION_DEFAULT
        )
    )
}

/**
 * Build a CarrierInfo for the Verification message.
 * Stock GMS (bevm.java:1170-1253) populates ALL 5 CarrierInfo fields.
 */
fun buildCarrierInfo(
    phoneNumber: String?,
    subscriptionId: Long,
    idTokenCertificateHash: String,
    idTokenNonce: String,
    callingPackage: String,
    imsiRequests: List<IMSIRequest>
): CarrierInfo {
    return CarrierInfo(
        phone_number = phoneNumber ?: "",
        subscription_id = subscriptionId,
        id_token_request = if (idTokenCertificateHash.isNotEmpty() || idTokenNonce.isNotEmpty()) {
            IdTokenRequest(
                certificate_hash = idTokenCertificateHash,
                token_nonce = idTokenNonce
            )
        } else null,
        calling_package = callingPackage,
        imsi_requests = imsiRequests
    )
}

/**
 * Convert a Bundle of key-value pairs to a list of Param protos.
 * Stock GMS (bevm.k) converts ALL bundle keys to proto Params.
 */
fun bundleToParams(bundle: Bundle?): List<Param> {
    if (bundle == null || bundle.isEmpty) {
        return emptyList()
    }
    val params = ArrayList<Param>(bundle.size())
    for (key in bundle.keySet()) {
        val value = bundle.getString(key) ?: continue
        params.add(Param(name = key, value_ = value))
    }
    return params
}

/**
 * Rebuild a SyncRequest swapping only ClientInfo.device_signals. All other fields preserved
 * via Wire-generated copy() — forward-safe against proto additions.
 */
fun rebuildSyncRequestWithDg(
    original: SyncRequest,
    droidGuardToken: String?
): SyncRequest {
    val deviceSignals = if (droidGuardToken != null) {
        DeviceSignals(droidguard_token = droidGuardToken)
    } else {
        DeviceSignals()
    }
    val newClientInfo = original.header_?.client_info?.copy(device_signals = deviceSignals)
    val newHeader = original.header_?.copy(client_info = newClientInfo)
    return original.copy(header_ = newHeader)
}
