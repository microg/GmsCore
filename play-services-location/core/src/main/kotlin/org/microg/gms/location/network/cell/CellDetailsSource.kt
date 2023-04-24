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
import androidx.core.content.getSystemService
import org.microg.gms.location.network.LocationCacheDatabase

class CellDetailsSource(private val context: Context, private val callback: CellDetailsCallback) {
    fun enable() = Unit
    fun disable() = Unit

    @SuppressLint("MissingPermission")
    fun startScan(workSource: WorkSource?) {
        val telephonyManager = context.getSystemService<TelephonyManager>() ?: return
        if (SDK_INT >= 29) {
            telephonyManager.requestCellInfoUpdate(context.mainExecutor, object : TelephonyManager.CellInfoCallback() {
                override fun onCellInfo(cells: MutableList<CellInfo>) {
                    val details = cells.map(CellInfo::toCellDetails).map { it.repair(context) }.filter(CellDetails::isValid)
                    if (details.isNotEmpty()) callback.onCellDetailsAvailable(details)
                }
            })
        } else if (SDK_INT >= 17) {
            val details = telephonyManager.allCellInfo.map(CellInfo::toCellDetails).map { it.repair(context) }.filter(CellDetails::isValid)
            if (details.isNotEmpty()) callback.onCellDetailsAvailable(details)
        } else {
            val networkOperator = telephonyManager.networkOperator
            var mcc: Int? = null
            var mnc: Int? = null
            if (networkOperator != null && networkOperator.length > 4) {
                mcc = networkOperator.substring(0, 3).toIntOrNull()
                mnc = networkOperator.substring(3).toIntOrNull()
            }
            val detail = telephonyManager.cellLocation.toCellDetails(mcc, mnc)
            if (detail.isValid) callback.onCellDetailsAvailable(listOf(detail))
        }
    }

    companion object {
        fun create(context: Context, callback: CellDetailsCallback): CellDetailsSource = CellDetailsSource(context, callback)
    }
}