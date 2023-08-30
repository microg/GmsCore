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
            return
        } else if (SDK_INT >= 17) {
            val allCellInfo = telephonyManager.allCellInfo
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
            val detail = telephonyManager.cellLocation?.toCellDetails(mcc, mnc)
            if (detail?.isValid == true) callback.onCellDetailsAvailable(listOf(detail))
        }
    }

    companion object {
        fun create(context: Context, callback: CellDetailsCallback): CellDetailsSource = CellDetailsSource(context, callback)
    }
}