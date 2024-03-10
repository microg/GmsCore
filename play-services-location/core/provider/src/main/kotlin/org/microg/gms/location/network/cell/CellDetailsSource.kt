/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.cell

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.WorkSource
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService

private const val TAG = "CellDetailsSource"

class CellDetailsSource(private val context: Context, private val callback: CellDetailsCallback) {
    fun enable() = Unit
    fun disable() = Unit

    @SuppressLint("MissingPermission")
    fun startScan(workSource: WorkSource?) {
        val telephonyManager = context.getSystemService<TelephonyManager>() ?: return
        if (SDK_INT >= 29) {
            try {
                telephonyManager.requestCellInfoUpdate(context.mainExecutor, object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cells: MutableList<CellInfo>) {
                        val details = cells.map(CellInfo::toCellDetails).map { it.repair(context) }.filter(CellDetails::isValid)
                        if (details.isNotEmpty()) callback.onCellDetailsAvailable(details)
                    }
                })
            } catch (e: SecurityException) {
                // It may trigger a SecurityException if the ACCESS_FINE_LOCATION permission isn't granted
                Log.w(TAG, "requestCellInfoUpdate in startScan failed", e)
            }

            return
        } else if (SDK_INT >= 17) {
            val allCellInfo: List<CellInfo>? = try {
                telephonyManager.allCellInfo
            } catch (e: SecurityException) {
                // It may trigger a SecurityException if the ACCESS_FINE_LOCATION permission isn't granted
                Log.w(TAG, "allCellInfo in startScan failed", e)
                null
            }
            if (allCellInfo != null) {
                val details = allCellInfo.map(CellInfo::toCellDetails).map { it.repair(context) }.filter(CellDetails::isValid)
                if (details.isNotEmpty()) {
                    callback.onCellDetailsAvailable(details)
                    return
                }
            }
        }
        val networkOperator = telephonyManager.networkOperator
        if (networkOperator != null && networkOperator.length > 4) {
            val mcc = networkOperator.substring(0, 3).toIntOrNull()
            val mnc = networkOperator.substring(3).toIntOrNull()
            val detail: CellDetails? = try {
                telephonyManager.cellLocation?.toCellDetails(mcc, mnc)
            } catch (e: SecurityException) {
                // It may trigger a SecurityException if the ACCESS_FINE_LOCATION permission isn't granted
                Log.w(TAG, "cellLocation in startScan failed", e)
                null
            }
            if (detail?.isValid == true) callback.onCellDetailsAvailable(listOf(detail))
        }
    }

    companion object {
        fun create(context: Context, callback: CellDetailsCallback): CellDetailsSource = CellDetailsSource(context, callback)
    }
}
