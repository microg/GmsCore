@file:RequiresApi(Build.VERSION_CODES.O)
@file:SuppressLint("HardwareIds")

package org.microg.gms.constellation.core.proto.builders

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.verificationMethods
import org.microg.gms.constellation.core.ConstellationStateStore
import org.microg.gms.constellation.core.proto.ChallengePreference
import org.microg.gms.constellation.core.proto.ChallengePreferenceMetadata
import org.microg.gms.constellation.core.proto.IdTokenRequest
import org.microg.gms.constellation.core.proto.Param
import org.microg.gms.constellation.core.proto.RequestHeader
import org.microg.gms.constellation.core.proto.RequestTrigger
import org.microg.gms.constellation.core.proto.SIMAssociation
import org.microg.gms.constellation.core.proto.SIMSlotInfo
import org.microg.gms.constellation.core.proto.SyncRequest
import org.microg.gms.constellation.core.proto.TelephonyInfo
import org.microg.gms.constellation.core.proto.TelephonyPhoneNumberType
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.VerificationAssociation
import org.microg.gms.constellation.core.proto.VerificationParam
import org.microg.gms.constellation.core.proto.VerificationPolicy

private const val TAG = "SyncRequestBuilder"

fun buildImsiToSubscriptionInfoMap(context: Context): Map<String, SubscriptionInfo> {
    val subscriptionManager =
        context.getSystemService<SubscriptionManager>() ?: return emptyMap()
    val telephonyManager =
        context.getSystemService<TelephonyManager>() ?: return emptyMap()
    val map = mutableMapOf<String, SubscriptionInfo>()

    try {
        subscriptionManager.activeSubscriptionInfoList?.forEach { info ->
            val subsTelephonyManager =
                telephonyManager.createForSubscriptionId(info.subscriptionId)
            subsTelephonyManager.subscriberId?.let { imsi ->
                map[imsi] = info
            }
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission to read SIM info for SubscriptionInfo mapping", e)
    }
    return map
}

fun getTelephonyPhoneNumbers(
    context: Context,
    subscriptionId: Int
): List<SIMAssociation.TelephonyPhoneNumber> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return emptyList()

    val permissions = listOf(Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE)
    if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED }) return emptyList()

    val subscriptionManager =
        context.getSystemService<SubscriptionManager>() ?: return emptyList()

    val sources = intArrayOf(
        SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER,
        SubscriptionManager.PHONE_NUMBER_SOURCE_UICC,
        SubscriptionManager.PHONE_NUMBER_SOURCE_IMS
    )

    return try {
        sources.map { source ->
            val number = subscriptionManager.getPhoneNumber(subscriptionId, source)
            if (number.isNotEmpty()) {
                SIMAssociation.TelephonyPhoneNumber(
                    phone_number = number,
                    phone_number_type = when (source) {
                        SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER ->
                            TelephonyPhoneNumberType.PHONE_NUMBER_SOURCE_CARRIER

                        SubscriptionManager.PHONE_NUMBER_SOURCE_UICC ->
                            TelephonyPhoneNumberType.PHONE_NUMBER_SOURCE_UICC

                        SubscriptionManager.PHONE_NUMBER_SOURCE_IMS ->
                            TelephonyPhoneNumberType.PHONE_NUMBER_SOURCE_IMS

                        else -> TelephonyPhoneNumberType.PHONE_NUMBER_SOURCE_UNSPECIFIED
                    }
                )
            } else null
        }.filterNotNull()
    } catch (e: Exception) {
        Log.w(TAG, "Error getting telephony phone numbers", e)
        emptyList()
    }
}

suspend operator fun SyncRequest.Companion.invoke(
    context: Context,
    sessionId: String,
    request: VerifyPhoneNumberRequest,
    includeClientAuth: Boolean = false,
    callingPackage: String = context.packageName,
    triggerType: RequestTrigger.Type = RequestTrigger.Type.TRIGGER_API_CALL
): SyncRequest {
    val buildContext = buildRequestContext(context)
    return SyncRequest(
        context = context,
        sessionId = sessionId,
        request = request,
        buildContext = buildContext,
        includeClientAuth = includeClientAuth,
        callingPackage = callingPackage,
        triggerType = triggerType
    )
}

suspend operator fun SyncRequest.Companion.invoke(
    context: Context,
    sessionId: String,
    request: VerifyPhoneNumberRequest,
    buildContext: RequestBuildContext,
    imsiToInfoMap: Map<String, SubscriptionInfo> = buildImsiToSubscriptionInfoMap(context),
    includeClientAuth: Boolean = false,
    callingPackage: String = context.packageName,
    triggerType: RequestTrigger.Type = RequestTrigger.Type.TRIGGER_API_CALL
): SyncRequest {
    val apiParamsList = Param.getList(request.extras)

    val verificationParams = request.targetedSims.map {
        VerificationParam(key = it.imsi, value_ = it.phoneNumberHint)
    }

    val structuredParams = VerificationPolicy(
        policy_id = request.policyId,
        max_verification_age_hours = request.timeout,
        id_token_request = IdTokenRequest(
            certificate_hash = request.idTokenRequest.idToken,
            token_nonce = request.idTokenRequest.subscriberHash
        ),
        calling_package = callingPackage,
        params = verificationParams
    )

    val verifications = imsiToInfoMap.map { (imsi, subscriptionInfo) ->
        val subscriptionId = subscriptionInfo.subscriptionId
        val slotIndex = subscriptionInfo.simSlotIndex
        val phoneNumber = subscriptionInfo.number ?: ""
        val iccid = subscriptionInfo.iccId ?: ""

        Verification(
            status = Verification.Status.STATUS_NONE,
            association = VerificationAssociation(
                sim = SIMAssociation(
                    sim_info = SIMAssociation.SIMInfo(
                        imsi = listOf(imsi),
                        sim_readable_number = phoneNumber,
                        telephony_phone_number = getTelephonyPhoneNumbers(context, subscriptionId),
                        iccid = iccid
                    ),
                    gaia_tokens = buildContext.gaiaTokens,
                    sim_slot = SIMSlotInfo(
                        slot_index = slotIndex,
                        subscription_id = subscriptionId
                    )
                )
            ),
            telephony_info = TelephonyInfo(context, subscriptionId),
            structured_api_params = structuredParams,
            api_params = apiParamsList,
            challenge_preference = ChallengePreference(
                capabilities = request.verificationMethods,
                metadata = ChallengePreferenceMetadata()
            )
        )
    }

    return SyncRequest(
        verifications = verifications,
        header_ = RequestHeader(context, sessionId, buildContext, triggerType, includeClientAuth),
        verification_tokens = ConstellationStateStore.loadVerificationTokens(context)
    )
}
