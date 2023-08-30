/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.cell

import android.location.Location

data class CellDetails(
    val type: Type,
    val mcc: Int? = null,
    val mnc: Int? = null,
    val lac: Int? = null,
    val tac: Int? = null,
    val cid: Long? = null,
    val sid: Int? = null,
    val nid: Int? = null,
    val bsid: Int? = null,
    val timestamp: Long? = null,
    val psc: Int? = null,
    val signalStrength: Int? = null,
    val location: Location? = null
) {
    companion object {
        enum class Type {
            CDMA, GSM, WCDMA, LTE, TDSCDMA, NR
        }
    }
}