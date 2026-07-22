/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class BluetoothBeacon(
    /**
     * The address of the Bluetooth Low Energy (BLE) beacon.
     */
    val macAddress: String? = null,
    /**
     * The name of the BLE beacon.
     */
    val name: String? = null,
    /**
     * The number of milliseconds since this BLE beacon was last seen.
     */
    val age: Long? = null,
    /**
     * The measured signal strength of the BLE beacon in dBm.
     */
    val signalStrength: Int? = null,
)