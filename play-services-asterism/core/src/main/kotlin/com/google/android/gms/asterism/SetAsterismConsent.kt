package com.google.android.gms.asterism

import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentSource
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.FlowContext

enum class SetAsterismConsentRequestStatus(val value: Int) {
    RCS_DEFAULT(0),
    RCS_LEGAL_FYI(1),
    DEVICE_PNVR(2),
    ON_DEMAND(3),
    EXPIRED(4);

    companion object {
        fun fromValue(value: Int): SetAsterismConsentRequestStatus =
            entries.find { it.value == value } ?: EXPIRED
    }
}

val SetAsterismConsentRequest.asterismClient: AsterismClient
    get() = AsterismClient.fromValue(asterismClientValue) ?: AsterismClient.UNKNOWN_CLIENT

val SetAsterismConsentRequest.consent: Consent
    get() = Consent.fromValue(consentValue) ?: Consent.CONSENT_UNKNOWN

val SetAsterismConsentRequest.rcsFlowContext: FlowContext
    get() = FlowContext.fromValue(rcsFlowContextValue) ?: FlowContext.FLOW_CONTEXT_UNSPECIFIED

val SetAsterismConsentRequest.deviceConsentSource: ConsentSource
    get() = ConsentSource.fromValue(deviceConsentSourceValue) ?: ConsentSource.SOURCE_UNSPECIFIED

val SetAsterismConsentRequest.deviceConsentVersion: ConsentVersion
    get() = ConsentVersion.fromValue(deviceConsentVersionValue).let {
        if (it == null || it == ConsentVersion.CONSENT_VERSION_UNSPECIFIED) {
            ConsentVersion.PHONE_VERIFICATION_DEFAULT
        } else {
            it
        }
    }

val SetAsterismConsentRequest.status: SetAsterismConsentRequestStatus
    get() = SetAsterismConsentRequestStatus.fromValue(statusValue)

fun SetAsterismConsentRequest.isDevicePnvrFlow(): Boolean {
    return asterismClient == AsterismClient.CONSTELLATION &&
            deviceConsentSourceValue > 0 &&
            deviceConsentVersionValue > 0
}