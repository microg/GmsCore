/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.GaiaConsent
import org.microg.gms.constellation.core.proto.GetConsentResponse
import org.microg.gms.constellation.core.proto.RcsConsent

class AsterismConsentResolverTest {

    @Test
    fun matchingGaiaConsentTakesPrecedence() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENTED,
                consent_version = ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX
            ),
            gaia_consents = listOf(
                GaiaConsent(
                    asterism_client = AsterismClient.RCS,
                    consent = Consent.NO_CONSENT,
                    consent_version = ConsentVersion.RCS_CONSENT
                )
            )
        )

        assertEquals(
            Consent.NO_CONSENT to ConsentVersion.RCS_CONSENT,
            resolveAsterismConsent(response, AsterismClient.RCS)
        )
    }

    @Test
    fun rcsConsentIsUsedWhenMatchingGaiaConsentIsAbsent() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENTED,
                consent_version = ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX
            )
        )

        assertEquals(
            Consent.CONSENTED to ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX,
            resolveAsterismConsent(response, AsterismClient.RCS)
        )
    }

    @Test
    fun unrelatedGaiaConsentDoesNotMaskRcsConsent() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENTED,
                consent_version = ConsentVersion.RCS_CONSENT
            ),
            gaia_consents = listOf(
                GaiaConsent(
                    asterism_client = AsterismClient.CONSTELLATION,
                    consent = Consent.NO_CONSENT,
                    consent_version = ConsentVersion.CONSENT_VERSION_UNSPECIFIED
                )
            )
        )

        assertEquals(
            Consent.CONSENTED to ConsentVersion.RCS_CONSENT,
            resolveAsterismConsent(response, AsterismClient.RCS)
        )
    }

    @Test
    fun rcsConsentIsNotAppliedToNonRcsClients() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENTED,
                consent_version = ConsentVersion.RCS_CONSENT
            )
        )

        assertEquals(
            Consent.NO_CONSENT to ConsentVersion.CONSENT_VERSION_UNSPECIFIED,
            resolveAsterismConsent(response, AsterismClient.CONSTELLATION)
        )
    }

    @Test
    fun noConsentDataFallsBackToNoConsent() {
        assertEquals(
            Consent.NO_CONSENT to ConsentVersion.CONSENT_VERSION_UNSPECIFIED,
            resolveAsterismConsent(GetConsentResponse(), AsterismClient.RCS)
        )
    }

    @Test
    fun unknownRcsConsentIsTreatedAsMissing() {
        val response = GetConsentResponse(
            rcs_consent = RcsConsent(
                consent = Consent.CONSENT_UNKNOWN,
                consent_version = ConsentVersion.CONSENT_VERSION_UNSPECIFIED
            )
        )

        assertEquals(
            Consent.NO_CONSENT to ConsentVersion.CONSENT_VERSION_UNSPECIFIED,
            resolveAsterismConsent(response, AsterismClient.RCS)
        )
    }
}
