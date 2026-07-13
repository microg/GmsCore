@file:RequiresApi(Build.VERSION_CODES.N)

package org.microg.gms.constellation.core.verification

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import org.microg.gms.constellation.core.proto.CarrierIdChallengeResponse
import org.microg.gms.constellation.core.proto.CarrierIdError
import org.microg.gms.constellation.core.proto.Challenge
import org.microg.gms.constellation.core.proto.ChallengeResponse

private const val TAG = "CarrierIdVerifier"

internal data class CarrierIdSession(
    val challengeId: String,
    val subId: Int,
    var attempts: Int = 0,
) {
    fun matches(challengeId: String, subId: Int): Boolean {
        return this.challengeId == challengeId && this.subId == subId
    }
}

fun Challenge.verifyCarrierId(context: Context, subId: Int): ChallengeResponse {
    val carrierChallenge = carrier_id_challenge ?: return failure(
        CarrierIdError.CARRIER_ID_ERROR_UNKNOWN_ERROR,
        "Carrier challenge data missing"
    )
    val challengeData = carrierChallenge.isim_request.takeIf { it.isNotEmpty() }
        ?: return failure(
            CarrierIdError.CARRIER_ID_ERROR_UNKNOWN_ERROR,
            "Carrier challenge data missing"
        )
    if (subId == -1) return failure(
        CarrierIdError.CARRIER_ID_ERROR_NO_SIM,
        "No active subscription for carrier auth"
    )

    val telephonyManager = context.getSystemService<TelephonyManager>()
        ?: return failure(
            CarrierIdError.CARRIER_ID_ERROR_NOT_SUPPORTED,
            "TelephonyManager unavailable"
        )
    val targetManager =
        telephonyManager.createForSubscriptionId(subId)
    if (challengeData.startsWith("[ts43]")) {
        // Not supported for now, try to get the server to dispatch something different
        return failure(
            CarrierIdError.CARRIER_ID_ERROR_NOT_SUPPORTED,
            "TS43-prefixed Carrier ID challenge not supported"
        )
    }

    val appType = carrierChallenge.app_type.takeIf { it != 0 } ?: TelephonyManager.APPTYPE_USIM

    return try {
        val response =
            targetManager.getIccAuthentication(appType, carrierChallenge.auth_type, challengeData)
        if (response.isNullOrEmpty()) {
            failure(CarrierIdError.CARRIER_ID_ERROR_NULL_RESPONSE, "Null ISIM response")
        } else {
            ChallengeResponse(
                carrier_id_response = CarrierIdChallengeResponse(
                    isim_response = response,
                    carrier_id_error = CarrierIdError.CARRIER_ID_ERROR_NO_ERROR
                )
            )
        }
    } catch (e: SecurityException) {
        Log.w(TAG, "Unable to read subscription for carrier auth", e)
        failure(
            CarrierIdError.CARRIER_ID_ERROR_UNABLE_TO_READ_SUBSCRIPTION,
            e.message ?: "SecurityException"
        )
    } catch (e: UnsupportedOperationException) {
        Log.w(TAG, "Carrier auth API unavailable", e)
        failure(
            CarrierIdError.CARRIER_ID_ERROR_NOT_SUPPORTED,
            e.message ?: "UnsupportedOperationException"
        )
    } catch (e: Exception) {
        Log.e(TAG, "Carrier auth failed", e)
        failure(
            CarrierIdError.CARRIER_ID_ERROR_REFLECTION_ERROR,
            e.message ?: "Reflection or platform error"
        )
    }
}

fun retryExceededCarrierId(): ChallengeResponse {
    return failure(
        CarrierIdError.CARRIER_ID_ERROR_RETRY_ATTEMPT_EXCEEDED,
        "Carrier ID retry attempt exceeded"
    )
}

private fun failure(status: CarrierIdError, message: String): ChallengeResponse {
    Log.w(TAG, message)
    return ChallengeResponse(
        carrier_id_response = CarrierIdChallengeResponse(
            isim_response = "",
            carrier_id_error = status
        )
    )
}
