/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A concatenated MT SMS arrives as one SmsMessage per PDU part. Buffering each part separately
 * loses any challenge string that spans a part boundary, so the parts of one broadcast are joined
 * into a single received message before matching.
 */
class MtSmsMultipartJoinTest {
    @Test
    fun `parts of one message are joined in order`() {
        val joined = joinParts(listOf("+15550100" to "abc", null to "def"))

        assertEquals("abcdef", joined?.body)
        assertEquals("+15550100", joined?.sender)
    }

    @Test
    fun `challenge spanning a part boundary matches after joining`() {
        val joined = joinParts(listOf("+15550100" to "your code is 74", null to "21, do not share"))

        assertNotNull(joined)
        assertTrue(
            "joined body should contain the challenge split across the PDU boundary",
            joined!!.body.contains("your code is 7421")
        )
    }

    @Test
    fun `sender is taken from the first part that carries one`() {
        val joined = joinParts(listOf(null to "abc", "+15550100" to "def"))

        assertEquals("+15550100", joined?.sender)
    }

    @Test
    fun `mixed originating senders cannot be joined into one challenge`() {
        assertNull(
            joinParts(
                listOf("+15550100" to "your code is 74", "+15550200" to "21")
            )
        )
    }

    @Test
    fun `missing multipart content cannot manufacture a matching challenge`() {
        assertNull(
            joinParts(
                listOf("+15550100" to "your code is 74", null to null, "+15550100" to "21")
            )
        )
    }

    @Test
    fun `an intent without any body yields no message`() {
        assertNull(joinParts(listOf(null to null)))
        assertNull(joinParts(emptyList<Pair<String?, String?>>()))
    }
}
