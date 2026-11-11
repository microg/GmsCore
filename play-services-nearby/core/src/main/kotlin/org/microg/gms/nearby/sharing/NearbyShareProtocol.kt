/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Nearby Share / Quick Share wire protocol helpers (#1272):
 * - BLE Fast Advertisement (`0xFE2C`) packet encoding & parsing
 * - mDNS (`_FC9F5ED42C8A._tcp.local.`) endpoint info encoding & parsing
 * - UKEY2 4-digit authentication token derivation
 */
object NearbyShareProtocol {
    const val FAST_ADVERTISEMENT_SERVICE_UUID = 0xFE2C
    const val MDNS_SERVICE_TYPE = "_FC9F5ED42C8A._tcp.local."
    const val PROTOCOL_VERSION = 0

    data class EndpointInfo(
        val version: Int,
        val visibility: Int,
        val deviceType: Int,
        val deviceName: String
    )

    fun encodeEndpointInfo(info: EndpointInfo): ByteArray {
        val nameBytes = info.deviceName.toByteArray(StandardCharsets.UTF_8)
        val clampedName = if (nameBytes.size > 128) nameBytes.copyOf(128) else nameBytes
        val header = ((info.version and 0x07) shl 5) or
            ((info.visibility and 0x03) shl 3) or
            (info.deviceType and 0x07)
        val out = ByteArray(18 + clampedName.size)
        out[0] = header.toByte()
        // 16-byte deterministic salt/metadata header + 1-byte name length
        out[17] = clampedName.size.toByte()
        System.arraycopy(clampedName, 0, out, 18, clampedName.size)
        return out
    }

    fun decodeEndpointInfo(raw: ByteArray): EndpointInfo? {
        if (raw.size < 18) return null
        val header = raw[0].toInt() and 0xFF
        val version = (header ushr 5) and 0x07
        val visibility = (header ushr 3) and 0x03
        val deviceType = header and 0x07
        val nameLen = raw[17].toInt() and 0xFF
        if (raw.size < 18 + nameLen) return null
        val name = String(raw, 18, nameLen, StandardCharsets.UTF_8)
        return EndpointInfo(
            version = version,
            visibility = visibility,
            deviceType = deviceType,
            deviceName = name
        )
    }

    fun deriveAuthPin(sharedSecret: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
        val value = ((digest[0].toInt() and 0xFF) shl 8) or (digest[1].toInt() and 0xFF)
        return String.format("%04d", value % 10000)
    }
}
