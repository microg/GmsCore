package com.google.android.gms.asterism

import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion

val GetAsterismConsentRequest.asterismClient: AsterismClient
    get() = AsterismClient.fromValue(asterismClientValue) ?: AsterismClient.UNKNOWN_CLIENT

fun getAsterismConsentResponse(
    requestCode: Int,
    consentState: Consent,
    gmscoreIidToken: String?,
    fid: String?,
    consentVersion: ConsentVersion
): GetAsterismConsentResponse {
    val consentStateValue =
        if (consentState == Consent.CONSENTED || consentState == Consent.CONSENT_UNKNOWN) {
            consentState.value
        } else {
            Consent.NO_CONSENT.value
        }

    return GetAsterismConsentResponse(
        requestCode,
        consentStateValue,
        gmscoreIidToken,
        fid,
        consentVersion.value
    )
}