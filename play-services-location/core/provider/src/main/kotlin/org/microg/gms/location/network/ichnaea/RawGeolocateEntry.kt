/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class RawGeolocateEntry(
    val timestamp: Long? = null,

    val bluetoothBeacon: BluetoothBeacon? = null,
    val cellTower: CellTower? = null,
    val wifiAccessPoint: WifiAccessPoint? = null,

    val location: ResponseLocation? = null,
    val horizontalAccuracy: Double? = null,
    val verticalAccuracy: Double? = null,

    val omit: Boolean = false,
)
