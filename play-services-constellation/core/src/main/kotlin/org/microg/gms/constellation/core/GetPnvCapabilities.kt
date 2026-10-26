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
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse.SimCapability
import com.google.android.gms.constellation.VerificationStatus
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import com.google.android.gms.constellation.verificationCapability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
                        verificationCapability(
                            9,
                            pnvVerificationStatus(carrierId, telephonyManager.simState)
                        )
                    )
                } else {
                    emptyList()
                }

                val subscriberIdDigest = MessageDigest.getInstance("SHA-256")
                    .digest(
                        telephonyManager
                            .subscriberIdForSubscription(baseTelephonyManager, info.subscriptionId)
                            .orEmpty()
                            .toByteArray()
                    )
                val subscriberIdDigestEncoded =
                    Base64.encodeToString(subscriberIdDigest, Base64.NO_WRAP)

                SimCapability(
                    info.simSlotIndex,
                    subscriberIdDigestEncoded,
                    carrierId,
                    telephonyManager
                        .simOperatorNameForSubscription(baseTelephonyManager, info.subscriptionId)
                        .orEmpty(),
                    verificationCapabilities
                )
            }

        // Telephony queries are synchronous, so cancellation may arrive while they block.
        ensureActive()
        callbacks.onGetPnvCapabilitiesCompleted(
            Status.SUCCESS,
            GetPnvCapabilitiesResponse(simCapabilities),
            ApiMetadata.DEFAULT
        )
    } catch (e: CancellationException) {
        // Cancelled (e.g. caller process died): do not deliver a result to a dead caller.
        throw e
    } catch (e: SecurityException) {
        ensureActive()
        Log.e(TAG, "getPnvCapabilities missing permission", e)
        callbacks.onGetPnvCapabilitiesCompleted(
            Status(5000),
            GetPnvCapabilitiesResponse(emptyList()),
            ApiMetadata.DEFAULT
        )
    } catch (e: Exception) {
        ensureActive()
        Log.e(TAG, "getPnvCapabilities failed", e)
        callbacks.onGetPnvCapabilitiesCompleted(
            Status.INTERNAL_ERROR,
            GetPnvCapabilitiesResponse(emptyList()),
            ApiMetadata.DEFAULT
        )
    }
}


/**
 * Resolves the advertised FPNV capability without treating an unconfigured carrier allowlist as a
 * deny-all policy. The integrated phenotype shim currently has no carrier IDs; when a real
 * allowlist is supplied later it remains authoritative.
 */
internal fun pnvVerificationStatus(
    carrierId: Int,
    simState: Int,
    allowedCarrierIds: Collection<Int> = GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS
): VerificationStatus = when {
    allowedCarrierIds.isNotEmpty() && carrierId !in allowedCarrierIds ->
        VerificationStatus.UNSUPPORTED_CARRIER

    simState != TelephonyManager.SIM_STATE_READY ->
        VerificationStatus.UNSUPPORTED_SIM_NOT_READY

    else -> VerificationStatus.SUPPORTED
}

/**
 * Reads the subscription's IMSI. On N+ this receiver was already created for the subscription via
 * [TelephonyManager.createForSubscriptionId], so [TelephonyManager.getSubscriberId] is per-SIM.
 * Pre-N exposes no public per-subscription accessor, so the hidden `getSubscriberId(int)` overload
 * is invoked reflectively, falling back to the default subscription's value when unavailable.
 */
@SuppressLint("HardwareIds")
private fun TelephonyManager.subscriberIdForSubscription(
    baseTelephonyManager: TelephonyManager,
    subscriptionId: Int
): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    subscriberId
} else {
    runCatching {
        TelephonyManager::class.java
            .getMethod("getSubscriberId", Int::class.javaPrimitiveType)
            .invoke(baseTelephonyManager, subscriptionId) as? String
    }.getOrNull() ?: subscriberId
}

/**
 * Reads the subscription's SIM operator name, mirroring [subscriberIdForSubscription]: per-SIM on
 * N+, and the hidden `getSimOperatorNameForSubscription(int)` overload (with a default-subscription
 * fallback) pre-N.
 */
private fun TelephonyManager.simOperatorNameForSubscription(
    baseTelephonyManager: TelephonyManager,
    subscriptionId: Int
): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    simOperatorName
} else {
    runCatching {
        TelephonyManager::class.java
            .getMethod("getSimOperatorNameForSubscription", Int::class.javaPrimitiveType)
            .invoke(baseTelephonyManager, subscriptionId) as? String
    }.getOrNull() ?: simOperatorName
}
