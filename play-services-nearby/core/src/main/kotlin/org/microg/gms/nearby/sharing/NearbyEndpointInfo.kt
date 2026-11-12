/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.security.SecureRandom

class NearbyEndpointInfo(
    val name: String,
    val deviceType: NearbyDeviceType = NearbyDeviceType.PHONE,
    val isVisible: Boolean = true,
    val salt: ByteArray = ByteArray(16).apply { SecureRandom().nextBytes(this) }
) {
    fun serialize(): ByteArray {
        val nameBytes = name.toByteArray(Charsets.UTF_8).let {
            if (it.size > 255) it.copyOf(255) else it
        }

        // Byte 0: version (0) | visibility bit | device type bits
        val visibilityBit = if (isVisible) 0 else 0x10
        val typeBits = (deviceType.value and 0x07) shl 1
        val byte0 = (visibilityBit or typeBits).toByte()

        val output = ByteArray(1 + 16 + (if (isVisible) 1 + nameBytes.size else 0))
        output[0] = byte0
        System.arraycopy(salt, 0, output, 1, 16)

        if (isVisible) {
            output[17] = nameBytes.size.toByte()
            System.arraycopy(nameBytes, 0, output, 18, nameBytes.size)
        }

        return output
    }

    fun toBase64(): String = serialize().toByteString().base64Url().replace("=", "")
    companion object {
        fun parse(data: ByteArray): NearbyEndpointInfo? {
            if (data.size < 17) return null
            val byte0 = data[0].toInt() and 0xFF
            val isVisible = (byte0 and 0x10) == 0
            val rawType = (byte0 and 0x0E) shr 1
            val deviceType = NearbyDeviceType.fromValue(rawType)

            val salt = ByteArray(16)
            System.arraycopy(data, 1, salt, 0, 16)

            val name = if (isVisible && data.size >= 18) {
                val nameLength = data[17].toInt() and 0xFF
                if (data.size >= 18 + nameLength) {
                    String(data, 18, nameLength, Charsets.UTF_8)
                } else {
                    "Nearby Device"
                }
            } else {
                "Nearby Device"
            }

            return NearbyEndpointInfo(name, deviceType, isVisible, salt)
        }

        fun parseBase64(base64: String): NearbyEndpointInfo? {
            return try {
                val decoded = base64.decodeBase64()?.toByteArray()
                    ?: base64.replace("-", "+").replace("_", "/").decodeBase64()?.toByteArray()
                if (decoded != null) parse(decoded) else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
