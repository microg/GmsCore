/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRequestPhoneNumberResolverTest {
    @Test
    fun prefersFormattedSimNumber() {
        val resolved = SyncRequestPhoneNumberResolver.resolve(
            simNumber = "0664123456",
            countryIso = "AT",
            hint = "+436641111111"
        ) { number, iso -> if (iso == "AT") "+43$number" else number }
        assertEquals("+430664123456", resolved)
    }

    @Test
    fun fallsBackToHintWhenSimNumberBlank() {
        val resolved = SyncRequestPhoneNumberResolver.resolve(
            simNumber = "",
            countryIso = "AT",
            hint = "+436641111111"
        ) { number, _ -> number }
        assertEquals("+436641111111", resolved)
    }

    @Test
    fun returnsEmptyWhenBothMissing() {
        val resolved = SyncRequestPhoneNumberResolver.resolve(
            simNumber = null,
            countryIso = "AT",
            hint = null
        ) { number, _ -> number }
        assertEquals("", resolved)
    }
}
