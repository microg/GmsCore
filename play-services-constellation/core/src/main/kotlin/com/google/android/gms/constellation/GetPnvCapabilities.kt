package com.google.android.gms.constellation

import com.google.android.gms.constellation.GetPnvCapabilitiesResponse.VerificationCapability

enum class VerificationStatus(val value: Int) {
    SUPPORTED(1),
    UNSUPPORTED_CARRIER(2),
    UNSUPPORTED_API_VERSION(3),
    UNSUPPORTED_SIM_NOT_READY(4);

    companion object {
        fun fromInt(value: Int): VerificationStatus = when (value) {
            2 -> UNSUPPORTED_CARRIER
            3 -> UNSUPPORTED_API_VERSION
            4 -> UNSUPPORTED_SIM_NOT_READY
            else -> SUPPORTED
        }
    }
}

fun verificationCapability(
    verificationMethod: Int,
    status: VerificationStatus
): VerificationCapability {
    return VerificationCapability(verificationMethod, status.value)
}

val VerificationCapability.status: VerificationStatus
    get() = VerificationStatus.fromInt(statusValue)