/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyShareProtocolTest {
    @Test
    fun testEndpointInfoRoundTrip() {
        val original = NearbyShareProtocol.EndpointInfo(
            version = 0,
            visibility = 2,
            deviceType = 1,
            deviceName = "microG Pixel"
        )
        val encoded = NearbyShareProtocol.encodeEndpointInfo(original)
        val decoded = NearbyShareProtocol.decodeEndpointInfo(encoded)
        assertNotNull(decoded)
        assertEquals(original.version, decoded!!.version)
        assertEquals(original.visibility, decoded.visibility)
        assertEquals(original.deviceType, decoded.deviceType)
        assertEquals(original.deviceName, decoded.deviceName)
    }

    @Test
    fun testRejectsTruncatedEndpointInfo() {
        assertNull(NearbyShareProtocol.decodeEndpointInfo(ByteArray(8)))
    }

    @Test
    fun testDeriveAuthPinIsFourDigits() {
        val pin = NearbyShareProtocol.deriveAuthPin("ukey2-handshake-transcript".toByteArray())
        assertEquals(4, pin.length)
    }
}
