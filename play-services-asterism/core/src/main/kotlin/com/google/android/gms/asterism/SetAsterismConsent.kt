package com.google.android.gms.asterism

import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.AsterismConsent.DeviceConsentVersion
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentSource
import org.microg.gms.constellation.core.proto.ConsentVersion

enum class SetAsterismConsentRequestStatus(val value: Int) {
    RESOURCE_TOS(0),
    STATIC_STRING(1),
    RCS(2),
    ON_DEMAND(3),
    RESOURCE_TOS_FALLBACK(4);

    companion object {
        fun fromValue(value: Int): SetAsterismConsentRequestStatus =
            entries.find { it.value == value } ?: RESOURCE_TOS_FALLBACK
    }
}

val SetAsterismConsentRequest.asterismClient: AsterismClient
    get() = AsterismClient.fromValue(asterismClientValue) ?: AsterismClient.UNKNOWN_CLIENT

val SetAsterismConsentRequest.consent: Consent
    get() = Consent.fromValue(consentValue) ?: Consent.CONSENT_UNKNOWN

val SetAsterismConsentRequest.consentVersion: ConsentVersion?
    get() = ConsentVersion.fromValue(consentVersionValue)

val SetAsterismConsentRequest.deviceConsentSource: ConsentSource
    get() = ConsentSource.fromValue(deviceConsentSourceValue) ?: ConsentSource.SOURCE_UNSPECIFIED

val SetAsterismConsentRequest.deviceConsentVersion: DeviceConsentVersion
    get() = DeviceConsentVersion.fromValue(deviceConsentVersionValue).let {
        if (it == null || it == DeviceConsentVersion.UNKNOWN) {
            DeviceConsentVersion.PHONE_VERIFICATION_DEFAULT
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
