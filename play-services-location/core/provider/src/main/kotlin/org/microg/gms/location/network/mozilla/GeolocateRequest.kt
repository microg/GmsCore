/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.mozilla

data class GeolocateRequest(
    /**
     * The clear text name of the cell carrier / operator.
     */
    val carrier: String? = null,
    /**
     * Should the clients IP address be used to locate it; defaults to true.
     */
    val considerIp: Boolean? = null,
    /**
     * The mobile country code stored on the SIM card.
     */
    val homeMobileCountryCode: Int? = null,
    /**
     * The mobile network code stored on the SIM card.
     */
    val homeMobileNetworkCode: Int? = null,
    /**
     * Same as the `radioType` entry in each cell record. If all the cell entries have the same `radioType`, it can be provided at the top level instead.
     */
    val radioType: RadioType? = null,
    val cellTowers: List<CellTower>? = null,
    val wifiAccessPoints: List<WifiAccessPoint>? = null,
    val fallbacks: Fallback? = null,
)