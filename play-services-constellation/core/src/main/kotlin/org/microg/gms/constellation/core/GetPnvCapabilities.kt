package org.microg.gms.constellation.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.SimCapability
import com.google.android.gms.constellation.VerificationCapability
import com.google.android.gms.constellation.VerificationStatus
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.constellation.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val TAG = "GetPnvCapabilities"

@SuppressLint("HardwareIds")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
suspend fun handleGetPnvCapabilities(
    context: Context,
    callbacks: IConstellationCallbacks,
    request: GetPnvCapabilitiesRequest
) = withContext(Dispatchers.IO) {
    try {
        val baseTelephonyManager =
            context.getSystemService<TelephonyManager>()
                ?: throw IllegalStateException("TelephonyManager unavailable")
        val subscriptionManager =
            context.getSystemService<SubscriptionManager>()
                ?: throw IllegalStateException("SubscriptionManager unavailable")
        val simCapabilities = subscriptionManager.activeSubscriptionInfoList
            .orEmpty()
            .filter { request.simSlotIndices.isEmpty() || it.simSlotIndex in request.simSlotIndices }
            .map { info ->
                val telephonyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    baseTelephonyManager.createForSubscriptionId(info.subscriptionId)
                } else {
                    baseTelephonyManager
                }

                val carrierId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telephonyManager.simCarrierId
                } else {
                    0
                }

                // GMS hardcodes public verification method 9 for the Firebase PNV TS43 capability path.
                val verificationCapabilities = if (9 in request.verificationTypes) {
                    listOf(
                        VerificationCapability(
                            9,
                            when {
                                !GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS.contains(
                                    carrierId
                                ) ->
                                    VerificationStatus.UNSUPPORTED_CARRIER

                                telephonyManager.simState != TelephonyManager.SIM_STATE_READY ->
                                    VerificationStatus.UNSUPPORTED_SIM_NOT_READY

                                else -> VerificationStatus.SUPPORTED
                            }
                        )
                    )
                } else {
                    emptyList()
                }

                // TODO: Reflection should be used to call telephonyManager.getSubscriberId(it.subscriptionId) for SDK < N
                val subscriberIdDigest = MessageDigest.getInstance("SHA-256")
                    .digest(telephonyManager.subscriberId.orEmpty().toByteArray())
                val subscriberIdDigestEncoded =
                    Base64.encodeToString(subscriberIdDigest, Base64.NO_WRAP)

                SimCapability(
                    info.simSlotIndex,
                    subscriberIdDigestEncoded,
                    carrierId,
                    // TODO: SDK < N is TelephonyManager.getSimOperatorNameForSubscription
                    telephonyManager.simOperatorName.orEmpty(),
                    verificationCapabilities
                )
            }

        callbacks.onGetPnvCapabilitiesCompleted(
            Status.SUCCESS,
            GetPnvCapabilitiesResponse(simCapabilities),
            ApiMetadata.DEFAULT
        )
    } catch (e: SecurityException) {
        Log.e(TAG, "getPnvCapabilities missing permission", e)
        callbacks.onGetPnvCapabilitiesCompleted(
            Status(5000),
            GetPnvCapabilitiesResponse(emptyList()),
            ApiMetadata.DEFAULT
        )
    } catch (e: Exception) {
        Log.e(TAG, "getPnvCapabilities failed", e)
        callbacks.onGetPnvCapabilitiesCompleted(
            Status.INTERNAL_ERROR,
            GetPnvCapabilitiesResponse(emptyList()),
            ApiMetadata.DEFAULT
        )
    }
}
