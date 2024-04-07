/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.ichnaea

data class WifiAccessPoint(
    /**
     * The BSSID of the WiFi network.
     */
    val macAddress: String,
    /**
     * The number of milliseconds since this network was last detected.
     */
    val age: Long? = null,
    /**
     * The WiFi channel for networks in the 2.4GHz range. This often ranges from 1 to 13.
     */
    val channel: Int? = null,
    /**
     * The frequency in MHz of the channel over which the client is communicating with the access point.
     */
    val frequency: Int? = null,
    /**
     * The received signal strength (RSSI) in dBm.
     */
    val signalStrength: Int? = null,
    /**
     * The current signal to noise ratio measured in dB.
     */
    val signalToNoiseRatio: Int? = null,
    /**
     * The SSID of the Wifi network.
     */
    val ssid: String? = null
) {
    init {
        if (ssid != null && ssid.endsWith("_nomap")) throw IllegalArgumentException("Wifi networks with a SSID ending in _nomap must not be collected.")
    }
}