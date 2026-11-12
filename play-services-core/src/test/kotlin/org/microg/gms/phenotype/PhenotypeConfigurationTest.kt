package org.microg.gms.phenotype

import org.junit.Assert.assertEquals
import org.junit.Test

class PhenotypeConfigurationTest {
    @Test
    fun messagesConfigurationKeepsAllRcsAndPenpalFlags() {
        val flags = requireNotNull(
            CONFIGURATION_OPTIONS["com.google.android.apps.messaging#com.google.android.apps.messaging"]
        )
        assertEquals(4, flags.size)
    }
}
