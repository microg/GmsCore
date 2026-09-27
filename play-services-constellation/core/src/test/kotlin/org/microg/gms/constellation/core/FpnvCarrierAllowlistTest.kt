/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FpnvCarrierAllowlistTest {
    @Test
    fun emptyAllowlistMeansUnrestricted() {
        GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS.clear()
        assertTrue(GetPnvCapabilitiesApiPhenotype.isCarrierAllowedForFpnv(0))
        assertTrue(GetPnvCapabilitiesApiPhenotype.isCarrierAllowedForFpnv(1))
        assertTrue(GetPnvCapabilitiesApiPhenotype.isCarrierAllowedForFpnv(42))
    }

    @Test
    fun populatedAllowlistRestrictsCarriers() {
        GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS.clear()
        GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS.add(1)
        assertTrue(GetPnvCapabilitiesApiPhenotype.isCarrierAllowedForFpnv(1))
        assertFalse(GetPnvCapabilitiesApiPhenotype.isCarrierAllowedForFpnv(2))
        GetPnvCapabilitiesApiPhenotype.FPNV_ALLOWED_CARRIER_IDS.clear()
    }
}
