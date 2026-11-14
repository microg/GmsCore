/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MtSmsVerifierTest {

    @Test
    fun assemblesMultipartSmsWhenChallengeSpansParts() {
        assertEquals(
            ReceivedSms(
                body = "Your verification code is 123456. Do not share it.",
                sender = "+15551234567"
            ),
            assembleReceivedSms(
                bodyParts = listOf(
                    "Your verification code is 123",
                    "456. Do not share it."
                ),
                sender = "+15551234567"
            )
        )
    }

    @Test
    fun preservesSinglePartSmsAndDefaultsMissingSender() {
        assertEquals(
            ReceivedSms(
                body = "Your verification code is 123456.",
                sender = ""
            ),
            assembleReceivedSms(
                bodyParts = listOf("Your verification code is 123456."),
                sender = null
            )
        )
    }

    @Test
    fun ignoresEmptySmsBroadcast() {
        assertNull(
            assembleReceivedSms(
                bodyParts = emptyList(),
                sender = null
            )
        )
    }

    @Test
    fun ignoresMultipartSmsWithMissingBodyPart() {
        assertNull(
            assembleReceivedSms(
                bodyParts = listOf("verification code: 123", null, "456"),
                sender = "+15551234567"
            )
        )
    }
}
