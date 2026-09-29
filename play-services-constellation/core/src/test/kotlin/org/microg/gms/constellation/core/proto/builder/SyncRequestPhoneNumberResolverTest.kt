/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRequestPhoneNumberResolverTest {

    @Test
    fun prefersFormattedE164() {
        assertEquals(
            "+15551234567",
            resolveSimReadableNumber(
                formattedE164 = "+15551234567",
                phoneNumberHint = "+15559876543",
                rawSubscriptionNumber = "5551234567"
            )
        )
    }

    @Test
    fun fallsBackToPhoneNumberHintWhenE164Missing() {
        assertEquals(
            "+15559876543",
            resolveSimReadableNumber(
                formattedE164 = null,
                phoneNumberHint = "+15559876543",
                rawSubscriptionNumber = "5551234567"
            )
        )
    }

    @Test
    fun fallsBackToRawNumberWhenHintBlank() {
        assertEquals(
            "5551234567",
            resolveSimReadableNumber(
                formattedE164 = "",
                phoneNumberHint = "   ",
                rawSubscriptionNumber = "5551234567"
            )
        )
    }

    @Test
    fun returnsEmptyWhenAllInputsMissing() {
        assertEquals(
            "",
            resolveSimReadableNumber(
                formattedE164 = null,
                phoneNumberHint = null,
                rawSubscriptionNumber = null
            )
        )
    }

    @Test
    fun skipsBlankE164ForHint() {
        assertEquals(
            "+491701234567",
            resolveSimReadableNumber(
                formattedE164 = "  ",
                phoneNumberHint = "+491701234567",
                rawSubscriptionNumber = null
            )
        )
    }
}
