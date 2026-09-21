package org.microg.gms.phenotype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhenotypeConfigurationTest {
    @Test
    fun messagesConfigurationKeepsAllRcsAndPenpalFlags() {
        val flags = requireNotNull(
            CONFIGURATION_OPTIONS["com.google.android.apps.messaging#com.google.android.apps.messaging"]
        )
        assertEquals(4, flags.size)
        val names = flags.map { it.name }.toSet()
        assertTrue(names.contains("bugle_phenotype__enable_penpal_conversation"))
        assertTrue(names.contains("bugle_phenotype__bug_325090692_enable_penpal_dasher_check"))
        assertTrue(names.contains("bugle_phenotype__enableDownloadCertificatesForMlsProvisioning"))
        assertTrue(names.contains("bugle_phenotype__enableUploadKeyPackagesForMlsProvisioning"))
    }

    @Test
    fun imsLibraryExposesUpiWithoutAcsFallbackFlag() {
        val flags = requireNotNull(CONFIGURATION_OPTIONS["com.google.android.ims.library"])
        assertEquals(1, flags.size)
        assertEquals(
            "RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met",
            flags[0].name
        )
        assertTrue(flags[0].bool)
    }
}
