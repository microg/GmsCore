/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VerificationSubscriptionsTest {
    @Test fun inaccessibleIdentifiersCannotProduceEmptySuccess() {
        assertThrows(IllegalStateException::class.java) {
            selectVerificationSubscriptions(emptyMap<String, Int>(), emptyList())
        }
    }

    @Test fun inaccessibleTargetCannotSelectAnotherSim() {
        assertThrows(IllegalArgumentException::class.java) {
            selectVerificationSubscriptions(mapOf("sim-a" to 1), listOf("sim-b"))
        }
    }

    @Test fun onlyRequestedSimIsVerified() {
        assertEquals(mapOf("sim-b" to 2), selectVerificationSubscriptions(
            mapOf("sim-a" to 1, "sim-b" to 2), listOf("sim-b")
        ))
    }

    @Test fun untargetedRequestUsesReadableSubscriptions() {
        assertEquals(mapOf("sim-a" to 1), selectVerificationSubscriptions(
            mapOf("sim-a" to 1), emptyList()
        ))
    }
}
