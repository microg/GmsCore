/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.cell

import android.location.Location
import org.microg.gms.location.network.NetworkDetails

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
    val pscOrPci: Int? = null,
    override val timestamp: Long? = null,
    override val signalStrength: Int? = null,
    val location: Location? = null
) : NetworkDetails {
    companion object {
        enum class Type {
            CDMA, GSM, WCDMA, LTE, TDSCDMA, NR
        }
    }
}