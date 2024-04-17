/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class GeosubmitItem(
    /**
     * The time of observation of the data, measured in milliseconds since the UNIX epoch. Can be omitted if the observation time is very recent. The age values in each section are relative to this timestamp.
     */
    val timestamp: Long? = null,
    /**
     * The position block contains information about where and when the data was observed.
     */
    val position: GeosubmitPosition? = null,
    val bluetoothBeacons: List<BluetoothBeacon>? = null,
    val cellTowers: List<CellTower>? = null,
    val wifiAccessPoints: List<WifiAccessPoint>? = null,
)