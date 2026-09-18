/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationProviderFlagsTest {
    @Test
    fun exposesExistingImsConfigurationToProviderReader() {
        assertEquals(
            mapOf("RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met" to "true"),
            providerFlagValues("com.google.android.ims.library")
        )
    }

    @Test
    fun preservesEmptyProviderForOtherNamespaces() {
        for (namespace in listOf(null, "", "unknown", "com.google.android.apps.messaging#com.google.android.apps.messaging",
                "com.google.android.ims.library#com.google.android.apps.messaging")) {
            assertTrue(providerFlagValues(namespace).isEmpty())
        }
    }
}
