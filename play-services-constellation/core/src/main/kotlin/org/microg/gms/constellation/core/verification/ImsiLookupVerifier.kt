@file:RequiresApi(Build.VERSION_CODES.N)

package org.microg.gms.constellation.core.verification

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import org.microg.gms.constellation.core.proto.Challenge
import org.microg.gms.constellation.core.proto.ChallengeResponse

private const val TAG = "ImsiLookupVerifier"

/**
 * Reads the IMSI of one subscription, or null when it cannot be read.
 *
 * The production reader goes through the same privileged phone-state path as
 * GetPnvCapabilities: a subscription-scoped TelephonyManager and subscriberId.
 */
internal fun interface ImsiReader {
    fun read(context: Context, subId: Int): String?
}

@SuppressLint("HardwareIds")
internal val PrivilegedImsiReader = ImsiReader { context, subId ->
    val telephonyManager = context.getSystemService<TelephonyManager>()
    if (telephonyManager == null) {
        Log.w(TAG, "TelephonyManager unavailable")
        return@ImsiReader null
    }
    try {
        telephonyManager.createForSubscriptionId(subId).subscriberId
    } catch (e: SecurityException) {
        Log.w(TAG, "No permission to read the IMSI for IMSI lookup", e)
        null
    }
}

/**
 * Solves an IMSI_LOOKUP challenge.
 *
 * Unlike the Carrier ID and TS.43 methods, IMSI_LOOKUP carries no challenge or
 * response payload in the Constellation schema: the server already holds the
 * IMSI it put in the verification association and only needs the device to
 * confirm it still owns that SIM. Confirmation is therefore an empty
 * ChallengeResponse, the same shape the MT path proceeds with when it has no
 * payload to add.
 *
 * Returns null when the IMSI cannot be confirmed - a missing privileged
 * permission, no matching active subscription, or an IMSI that no longer
 * matches the association. The caller stops the proceed sequence on null
 * rather than looping, because the schema has no IMSI-lookup error field to
 * report the reason in.
 */
internal fun Challenge.verifyImsiLookup(
    context: Context,
    expectedImsi: String?,
    subId: Int,
    readImsi: ImsiReader = PrivilegedImsiReader,
): ChallengeResponse? {
    if (expectedImsi.isNullOrEmpty()) {
        Log.w(TAG, "IMSI lookup challenge without an association IMSI")
        return null
    }
    if (subId == -1) {
        Log.w(TAG, "No active subscription matching the IMSI lookup challenge")
        return null
    }

    val imsi = readImsi.read(context, subId)
    if (imsi.isNullOrEmpty()) {
        Log.w(TAG, "IMSI unavailable for subscription $subId")
        return null
    }
    if (imsi != expectedImsi) {
        Log.w(TAG, "IMSI for subscription $subId no longer matches the challenge association")
        return null
    }

    Log.d(TAG, "IMSI lookup confirmed for subscription $subId")
    return ChallengeResponse()
}
