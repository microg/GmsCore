/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

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
    val bluetoothBeacons: List<BluetoothBeacon>? = null,
    val cellTowers: List<CellTower>? = null,
    val wifiAccessPoints: List<WifiAccessPoint>? = null,
    /**
     * By default, both a GeoIP based position fallback and a fallback based on cell location areas (lac’s) are enabled. Omit the fallbacks section if you want to use the defaults. Change the values to false if you want to disable either of the fallbacks.
     */
    val fallbacks: Fallback? = null,
)