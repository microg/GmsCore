package org.microg.gms.constellation.core

import android.telephony.TelephonyManager
import com.google.android.gms.constellation.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPnvCapabilitiesPolicyTest {
    @Test
    fun emptyCarrierAllowlistDoesNotDisableReadySim() {
        assertEquals(
            VerificationStatus.SUPPORTED,
            pnvVerificationStatus(
                carrierId = 123,
                simState = TelephonyManager.SIM_STATE_READY,
                allowedCarrierIds = emptyList()
            )
        )
    }

    @Test
    fun configuredCarrierAllowlistRejectsUnknownCarrier() {
        assertEquals(
            VerificationStatus.UNSUPPORTED_CARRIER,
            pnvVerificationStatus(
                carrierId = 123,
                simState = TelephonyManager.SIM_STATE_READY,
                allowedCarrierIds = listOf(456)
            )
        )
    }

    @Test
    fun configuredCarrierAllowlistPreservesSimReadinessCheck() {
        assertEquals(
            VerificationStatus.UNSUPPORTED_SIM_NOT_READY,
            pnvVerificationStatus(
                carrierId = 123,
                simState = TelephonyManager.SIM_STATE_ABSENT,
                allowedCarrierIds = listOf(123)
            )
        )
    }
}
