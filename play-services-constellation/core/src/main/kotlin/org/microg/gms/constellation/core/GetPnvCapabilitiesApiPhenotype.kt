package org.microg.gms.constellation.core

object GetPnvCapabilitiesApiPhenotype {
    /**
     * Carrier IDs allowed for Firebase PNV / TS.43 capability (method 9).
     *
     * Stock GMS populates this from Phenotype. An empty list in microG must not
     * mean "no carriers" — that permanently marks every SIM as
     * [com.google.android.gms.constellation.VerificationStatus.UNSUPPORTED_CARRIER].
     * Empty = unrestricted (any ready SIM may advertise method 9).
     */
    val FPNV_ALLOWED_CARRIER_IDS = ArrayList<Int>()

    fun isCarrierAllowedForFpnv(carrierId: Int): Boolean {
        return FPNV_ALLOWED_CARRIER_IDS.isEmpty() || FPNV_ALLOWED_CARRIER_IDS.contains(carrierId)
    }
}