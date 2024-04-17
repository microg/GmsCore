/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class CellTower(
    /**
     * The type of radio network.
     */
    val radioType: RadioType? = null,
    /**
     * The mobile country code.
     */
    val mobileCountryCode: Int? = null,
    /**
     * The mobile network code.
     */
    val mobileNetworkCode: Int? = null,
    /**
     * The location area code for GSM and WCDMA networks. The tracking area code for LTE networks.
     */
    val locationAreaCode: Int? = null,
    /**
     * The cell id or cell identity.
     */
    val cellId: Int? = null,
    /**
     * The number of milliseconds since this networks was last detected.
     */
    val age: Long? = null,
    /**
     * The primary scrambling code for WCDMA and physical cell id for LTE.
     */
    val psc: Int? = null,
    /**
     * The signal strength for this cell network, either the RSSI or RSCP.
     */
    val signalStrength: Int? = null,
    /**
     * The timing advance value for this cell network.
     */
    val timingAdvance: Int? = null,
)