/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationSimSlotTest {
    @Test
    fun mappedImsiRemainsAuthoritative() {
        assertEquals(2, resolveVerificationSimSlot("known", mapOf("known" to 2), 1))
    }

    @Test
    fun unmappedImsiRetainsAssociatedSlot() {
        assertEquals(1, resolveVerificationSimSlot("missing", emptyMap(), 1))
    }

    @Test
    fun absentImsiRetainsFirstSlot() {
        assertEquals(0, resolveVerificationSimSlot(null, emptyMap(), 0))
    }

    @Test
    fun unrelatedImsiMappingDoesNotSelectAnotherSim() {
        assertEquals(1, resolveVerificationSimSlot("missing", mapOf("other" to 0), 1))
    }

    @Test
    fun unresolvedLocalSlotUsesAssociation() {
        assertEquals(1, resolveVerificationSimSlot("known", mapOf("known" to -1), 1))
    }

    @Test
    fun absentAssociationWithoutImsiRemainsUnknown() {
        assertEquals(-1, resolveVerificationSimSlot(null, emptyMap(), null))
    }

    @Test
    fun negativeAssociationRemainsUnknown() {
        assertEquals(-1, resolveVerificationSimSlot("missing", emptyMap(), -1))
    }

    @Test
    fun resolvedFirstSlotSurvivesMissingAssociation() {
        assertEquals(0, resolveVerificationSimSlot("known", mapOf("known" to 0), null))
    }
}
