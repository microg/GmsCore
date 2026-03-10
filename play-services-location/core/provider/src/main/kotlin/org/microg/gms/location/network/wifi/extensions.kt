/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.network.wifi

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build.VERSION.SDK_INT
import android.os.SystemClock
import androidx.annotation.RequiresApi

internal fun ScanResult.toWifiDetails(): WifiDetails = WifiDetails(
    macAddress = BSSID,
    ssid = SSID,
    timestamp = if (SDK_INT >= 19) System.currentTimeMillis() - (SystemClock.elapsedRealtime() - (timestamp / 1000)) else null,
    frequency = frequency,
    channel = frequencyToChannel(frequency),
    signalStrength = level,
    open = setOf("WEP", "WPA", "PSK", "EAP", "IEEE8021X", "PEAP", "TLS", "TTLS").none { capabilities.contains(it) }
)

@RequiresApi(31)
internal fun WifiInfo.toWifiDetails(): WifiDetails? {
    return WifiDetails(
        macAddress = bssid ?: return null,
        ssid = ssid?.removeSurrounding("\"")
            ?.takeIf { it != WifiManager.UNKNOWN_SSID },
        timestamp = System.currentTimeMillis(),
        frequency = frequency,
        signalStrength = rssi,
        open = currentSecurityType == WifiInfo.SECURITY_TYPE_OPEN
    )
}

private const val BAND_24_GHZ_FIRST_CH_NUM = 1
private const val BAND_24_GHZ_LAST_CH_NUM = 14
private const val BAND_5_GHZ_FIRST_CH_NUM = 32
private const val BAND_5_GHZ_LAST_CH_NUM = 177
private const val BAND_6_GHZ_FIRST_CH_NUM = 1
private const val BAND_6_GHZ_LAST_CH_NUM = 233
private const val BAND_60_GHZ_FIRST_CH_NUM = 1
private const val BAND_60_GHZ_LAST_CH_NUM = 6
private const val BAND_24_GHZ_START_FREQ_MHZ = 2412
private const val BAND_24_GHZ_END_FREQ_MHZ = 2484
private const val BAND_5_GHZ_START_FREQ_MHZ = 5160
private const val BAND_5_GHZ_END_FREQ_MHZ = 5885
private const val BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ = 5935
private const val BAND_6_GHZ_START_FREQ_MHZ = 5955
private const val BAND_6_GHZ_END_FREQ_MHZ = 7115
private const val BAND_60_GHZ_START_FREQ_MHZ = 58320
private const val BAND_60_GHZ_END_FREQ_MHZ = 70200

internal fun frequencyToChannel(freq: Int): Int? {
    return when (freq) {
        // Special cases
        BAND_24_GHZ_END_FREQ_MHZ -> BAND_24_GHZ_LAST_CH_NUM
        BAND_6_GHZ_OP_CLASS_136_CH_2_FREQ_MHZ -> 2

        in BAND_24_GHZ_START_FREQ_MHZ..BAND_24_GHZ_END_FREQ_MHZ ->
            (freq - BAND_24_GHZ_START_FREQ_MHZ) / 5 + BAND_24_GHZ_FIRST_CH_NUM

        in BAND_5_GHZ_START_FREQ_MHZ..BAND_5_GHZ_END_FREQ_MHZ ->
            (freq - BAND_5_GHZ_START_FREQ_MHZ) / 5 + BAND_5_GHZ_FIRST_CH_NUM

        in BAND_6_GHZ_START_FREQ_MHZ..BAND_6_GHZ_END_FREQ_MHZ ->
            (freq - BAND_6_GHZ_START_FREQ_MHZ) / 5 + BAND_6_GHZ_FIRST_CH_NUM

        in BAND_60_GHZ_START_FREQ_MHZ..BAND_60_GHZ_END_FREQ_MHZ ->
            (freq - BAND_60_GHZ_START_FREQ_MHZ) / 2160 + BAND_60_GHZ_FIRST_CH_NUM

        else -> null
    }
}

val WifiDetails.isNomap: Boolean
    get() = ssid?.endsWith("_nomap") == true

val WifiDetails.isHidden: Boolean
    get() = ssid == ""

val WifiDetails.isRequestable: Boolean
    get() = !isNomap && !isHidden && !isMoving

val WifiDetails.macBytes: ByteArray
    get() {
        val mac = macClean
        return byteArrayOf(
            mac.substring(0, 2).toInt(16).toByte(),
            mac.substring(2, 4).toInt(16).toByte(),
            mac.substring(4, 6).toInt(16).toByte(),
            mac.substring(6, 8).toInt(16).toByte(),
            mac.substring(8, 10).toInt(16).toByte(),
            mac.substring(10, 12).toInt(16).toByte()
        )
    }
val WifiDetails.macClean: String
    get() = macAddress.lowercase().replace(":", "")