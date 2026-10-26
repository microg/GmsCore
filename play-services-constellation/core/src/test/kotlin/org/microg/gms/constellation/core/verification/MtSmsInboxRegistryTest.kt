package org.microg.gms.constellation.core.verification

import org.junit.Assert.assertEquals
import org.junit.Test

class MtSmsInboxRegistryTest {
    @Test fun activeSubscriptionsAlsoPrepareGlobalFallback() {
        assertEquals(listOf(1, 2, -1), effectiveMtSmsInboxSubIds(listOf(1, 2)))
    }

    @Test fun duplicateAndInvalidIdsAreNormalized() {
        assertEquals(listOf(3, -1), effectiveMtSmsInboxSubIds(listOf(3, 3, -1, -5)))
    }

    @Test fun noActiveSubscriptionStillPreparesGlobalFallback() {
        assertEquals(listOf(-1), effectiveMtSmsInboxSubIds(emptyList()))
    }
}
