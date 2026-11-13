/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Fips186PrfTest {

    @Test
    fun deriveKeys_returnsExpectedKeyLengths() {
        val keys = Fips186Prf.deriveKeys(
            identityBytes = byteArrayOf(0x01, 0x02, 0x03),
            ik = ByteArray(16) { it.toByte() },
            ck = ByteArray(16) { (it + 16).toByte() }
        )

        assertEquals(16, keys["K_encr"]!!.size)
        assertEquals(16, keys["K_aut"]!!.size)
        assertEquals(64, keys["MSK"]!!.size)
        assertEquals(64, keys["EMSK"]!!.size)
    }

    @Test
    fun deriveKeys_isDeterministic() {
        val identity = "310260123456789".toByteArray(Charsets.UTF_8)
        val ik = ByteArray(16) { 0x11 }
        val ck = ByteArray(16) { 0x22 }

        val first = Fips186Prf.deriveKeys(identity, ik, ck)
        val second = Fips186Prf.deriveKeys(identity, ik, ck)

        assertArrayEquals(first["K_encr"], second["K_encr"])
        assertArrayEquals(first["K_aut"], second["K_aut"])
        assertArrayEquals(first["MSK"], second["MSK"])
        assertArrayEquals(first["EMSK"], second["EMSK"])
    }

    @Test
    fun deriveKeys_changesWhenIdentityChanges() {
        val ik = ByteArray(16) { 0x11 }
        val ck = ByteArray(16) { 0x22 }

        val a = Fips186Prf.deriveKeys("imsi-a".toByteArray(), ik, ck)
        val b = Fips186Prf.deriveKeys("imsi-b".toByteArray(), ik, ck)

        assertEquals(false, a["MSK"]!!.contentEquals(b["MSK"]))
    }
}
