/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import java.net.InetAddress

enum class NearbyDeviceType(val value: Int) {
    UNKNOWN(0),
    PHONE(1),
    TABLET(2),
    COMPUTER(3);

    companion object {
        fun fromValue(value: Int): NearbyDeviceType = when (value) {
            1 -> PHONE
            2 -> TABLET
            3 -> COMPUTER
            else -> UNKNOWN
        }
    }
}

data class NearbyDevice(
    val id: String,
    val name: String,
    val type: NearbyDeviceType = NearbyDeviceType.PHONE,
    val host: InetAddress? = null,
    val port: Int = 0,
    val endpointId: String? = null,
    val endpointInfo: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NearbyDevice) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
