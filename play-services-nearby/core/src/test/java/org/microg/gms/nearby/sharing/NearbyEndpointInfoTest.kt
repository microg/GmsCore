/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyEndpointInfoTest {

    @Test
    fun testSerializeAndParseRoundtrip() {
        val original = NearbyEndpointInfo(
            name = "Test Device",
            deviceType = NearbyDeviceType.PHONE,
            isVisible = true
        )

        val serialized = original.serialize()
        assertNotNull(serialized)
        assertTrue(serialized.size > 17)

        val parsed = NearbyEndpointInfo.parse(serialized)
        assertNotNull(parsed)
        assertEquals("Test Device", parsed!!.name)
        assertEquals(NearbyDeviceType.PHONE, parsed.deviceType)
        assertTrue(parsed.isVisible)
        assertArrayEquals(original.salt, parsed.salt)
    }

    @Test
    fun testBase64UrlSafeRoundtrip() {
        val original = NearbyEndpointInfo(
            name = "My Tablet",
            deviceType = NearbyDeviceType.TABLET,
            isVisible = true
        )

        val b64 = original.toBase64()
        assertNotNull(b64)
        assertFalse(b64.contains("="))
        assertFalse(b64.contains("+"))
        assertFalse(b64.contains("/"))

        val parsed = NearbyEndpointInfo.parseBase64(b64)
        assertNotNull(parsed)
        assertEquals("My Tablet", parsed!!.name)
        assertEquals(NearbyDeviceType.TABLET, parsed.deviceType)
        assertTrue(parsed.isVisible)
    }

    @Test
    fun testNameTruncationAt255Bytes() {
        val longName = "A".repeat(300)
        val info = NearbyEndpointInfo(name = longName, deviceType = NearbyDeviceType.COMPUTER)
        val serialized = info.serialize()

        val parsed = NearbyEndpointInfo.parse(serialized)
        assertNotNull(parsed)
        assertEquals(255, parsed!!.name.length)
        assertEquals("A".repeat(255), parsed.name)
        assertEquals(NearbyDeviceType.COMPUTER, parsed.deviceType)
    }

    @Test
    fun testHiddenVisibility() {
        val hidden = NearbyEndpointInfo(
            name = "Incognito Phone",
            deviceType = NearbyDeviceType.PHONE,
            isVisible = false
        )

        val serialized = hidden.serialize()
        assertEquals(17, serialized.size)

        val parsed = NearbyEndpointInfo.parse(serialized)
        assertNotNull(parsed)
        assertFalse(parsed!!.isVisible)
        assertEquals(NearbyDeviceType.PHONE, parsed.deviceType)
    }

    @Test
    fun testDeviceTypes() {
        for (type in NearbyDeviceType.values()) {
            val info = NearbyEndpointInfo(name = "Dev-${type.name}", deviceType = type)
            val parsed = NearbyEndpointInfo.parse(info.serialize())
            assertNotNull(parsed)
            assertEquals(type, parsed!!.deviceType)
        }
    }

    @Test
    fun testInvalidData() {
        assertNull(NearbyEndpointInfo.parse(byteArrayOf()))
        assertNull(NearbyEndpointInfo.parse(ByteArray(16)))
        assertNull(NearbyEndpointInfo.parseBase64("!invalid-base64!"))
    }
}
