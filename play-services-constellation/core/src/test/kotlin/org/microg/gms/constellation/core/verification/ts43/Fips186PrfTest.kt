/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Fips186PrfTest {
    @Test
    fun deriveKeysHasRfc4187Lengths() {
        val keys = Fips186Prf.deriveKeys(
            identityBytes = "test-identity".toByteArray(),
            ik = ByteArray(16) { 1 },
            ck = ByteArray(16) { 2 }
        )
        assertEquals(16, keys.getValue("K_encr").size)
        assertEquals(16, keys.getValue("K_aut").size)
        assertEquals(64, keys.getValue("MSK").size)
        assertEquals(64, keys.getValue("EMSK").size)
    }

    @Test
    fun deriveKeysIsDeterministic() {
        val first = Fips186Prf.deriveKeys(byteArrayOf(1, 2, 3), ByteArray(16), ByteArray(16))
        val second = Fips186Prf.deriveKeys(byteArrayOf(1, 2, 3), ByteArray(16), ByteArray(16))
        assertTrue(first.keys == second.keys)
        first.forEach { (name, value) ->
            assertTrue(value.contentEquals(second.getValue(name)))
        }
    }
}
