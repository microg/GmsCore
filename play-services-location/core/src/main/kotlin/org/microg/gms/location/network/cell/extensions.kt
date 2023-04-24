/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.cell

import android.content.Context
import android.location.Location
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import android.telephony.CellIdentity
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.CellLocation
import android.telephony.TelephonyManager
import android.telephony.cdma.CdmaCellLocation
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import org.microg.gms.location.network.TAG

private fun locationFromCdma(latitude: Int, longitude: Int) = if (latitude == Int.MAX_VALUE || longitude == Int.MAX_VALUE) null else Location("cdma").also {
    it.latitude = latitude.toDouble() / 14400.0
    it.longitude = longitude.toDouble() / 14400.0
    it.accuracy = 30000f
}

private fun CdmaCellLocation.toCellDetails(timestamp: Long? = null) = CellDetails(
    type = CellDetails.Companion.Type.CDMA,
    sid = systemId,
    nid = networkId,
    bsid = baseStationId,
    location = locationFromCdma(baseStationLatitude, baseStationLongitude),
    timestamp = timestamp
)

private fun GsmCellLocation.toCellDetails(mcc: Int? = null, mnc: Int? = null, timestamp: Long? = null) = CellDetails(
    type = CellDetails.Companion.Type.GSM,
    mcc = mcc,
    mnc = mnc,
    lac = lac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toLong(),
    psc = psc.takeIf { it != Int.MAX_VALUE && it != -1 },
    timestamp = timestamp
)

internal fun CellLocation.toCellDetails(mcc: Int? = null, mnc: Int? = null, timestamp: Long? = null) = when (this) {
    is CdmaCellLocation -> toCellDetails(timestamp)
    is GsmCellLocation -> toCellDetails(mcc, mnc, timestamp)
    else -> throw IllegalArgumentException("Unknown CellLocation type")
}

@RequiresApi(17)
private fun CellIdentityCdma.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.CDMA,
    sid = systemId,
    nid = networkId,
    bsid = basestationId,
    location = locationFromCdma(latitude, longitude)
)

@RequiresApi(17)
private fun CellIdentityGsm.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.GSM,
    mcc = if (SDK_INT >= 28) mccString?.toIntOrNull() else mcc.takeIf { it != Int.MAX_VALUE && it != -1 },
    mnc = if (SDK_INT >= 28) mncString?.toIntOrNull() else mnc.takeIf { it != Int.MAX_VALUE && it != -1 },
    lac = lac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toLong()
)

@RequiresApi(18)
private fun CellIdentityWcdma.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.WCDMA,
    mcc = if (SDK_INT >= 28) mccString?.toIntOrNull() else mcc.takeIf { it != Int.MAX_VALUE && it != -1 },
    mnc = if (SDK_INT >= 28) mncString?.toIntOrNull() else mnc.takeIf { it != Int.MAX_VALUE && it != -1 },
    lac = lac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toLong(),
    psc = psc.takeIf { it != Int.MAX_VALUE && it != -1 }
)

@RequiresApi(17)
private fun CellIdentityLte.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.LTE,
    mcc = if (SDK_INT >= 28) mccString?.toIntOrNull() else mcc.takeIf { it != Int.MAX_VALUE && it != -1 },
    mnc = if (SDK_INT >= 28) mncString?.toIntOrNull() else mnc.takeIf { it != Int.MAX_VALUE && it != -1 },
    tac = tac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = ci.takeIf { it != Int.MAX_VALUE && it != -1 }?.toLong()
)

@RequiresApi(28)
private fun CellIdentityTdscdma.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.TDSCDMA,
    mcc = mccString?.toIntOrNull(),
    mnc = mncString?.toIntOrNull(),
    lac = lac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = cid.takeIf { it != Int.MAX_VALUE && it != -1 }?.toLong()
)

@RequiresApi(29)
private fun CellIdentityNr.toCellDetails() = CellDetails(
    type = CellDetails.Companion.Type.NR,
    mcc = mccString?.toIntOrNull(),
    mnc = mncString?.toIntOrNull(),
    tac = tac.takeIf { it != Int.MAX_VALUE && it != -1 },
    cid = nci.takeIf { it != Long.MAX_VALUE && it != -1L }
)

@RequiresApi(28)
internal fun CellIdentity.toCellDetails() = when {
    this is CellIdentityCdma -> toCellDetails()
    this is CellIdentityGsm -> toCellDetails()
    this is CellIdentityWcdma -> toCellDetails()
    this is CellIdentityLte -> toCellDetails()
    this is CellIdentityTdscdma -> toCellDetails()
    SDK_INT >= 29 && this is CellIdentityNr -> toCellDetails()
    else -> throw IllegalArgumentException("Unknown CellIdentity type")
}

private val CellInfo.epochTimestamp: Long
    @RequiresApi(17)
    get() = if (SDK_INT >= 30) System.currentTimeMillis() - (SystemClock.elapsedRealtime() - timestampMillis)
    else System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - timeStamp)

@RequiresApi(17)
internal fun CellInfo.toCellDetails() = when {
    this is CellInfoCdma -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    this is CellInfoGsm -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    SDK_INT >= 18 && this is CellInfoWcdma -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    this is CellInfoLte -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    SDK_INT >= 29 && this is CellInfoTdscdma -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    SDK_INT >= 30 -> cellIdentity.toCellDetails().copy(timestamp = epochTimestamp, signalStrength = cellSignalStrength.dbm)
    else -> throw IllegalArgumentException("Unknown CellInfo type")
}

/**
 * Fix a few known issues in Android's parsing of MNCs
 */
internal fun CellDetails.repair(context: Context): CellDetails {
    if (type == CellDetails.Companion.Type.CDMA) return this
    val networkOperator = context.getSystemService<TelephonyManager>()?.networkOperator ?: return this
    if (networkOperator.length < 5) return this
    val networkOperatorMnc = networkOperator.substring(3).toInt()
    if (networkOperator[3] == '0' && mnc == null || networkOperator.length == 5 && mnc == networkOperatorMnc * 10 + 15)
        return copy(mnc = networkOperatorMnc)
    return this
}

val CellDetails.isValid: Boolean
    get() = when (type) {
        CellDetails.Companion.Type.CDMA -> sid != null && nid != null && bsid != null
        else -> mcc != null && mnc != null && cid != null && (lac != null || tac != null)
    }