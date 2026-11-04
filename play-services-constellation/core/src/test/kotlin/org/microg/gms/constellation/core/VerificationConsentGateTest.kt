package org.microg.gms.constellation.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.Consent
import org.microg.gms.constellation.core.proto.GaiaConsent
import org.microg.gms.constellation.core.proto.GetConsentResponse
import org.microg.gms.constellation.core.proto.RcsConsent

class VerificationConsentGateTest {
    @Test fun dedicatedRcsConsentAllowsVerificationWhenMatchingGaiaIsAbsent() {
        assertTrue(hasRequiredVerificationConsent(
            GetConsentResponse(rcs_consent = RcsConsent(consent = Consent.CONSENTED)), AsterismClient.RCS))
    }

    @Test fun matchingGaiaConsentAllowsVerification() {
        assertTrue(hasRequiredVerificationConsent(
            GetConsentResponse(gaia_consents = listOf(
                GaiaConsent(asterism_client = AsterismClient.RCS, consent = Consent.CONSENTED))),
            AsterismClient.RCS))
    }

    @Test fun matchingGaiaNoConsentOverridesRcsFallbackConsent() {
        assertFalse(hasRequiredVerificationConsent(
            GetConsentResponse(
                rcs_consent = RcsConsent(consent = Consent.CONSENTED),
                gaia_consents = listOf(
                    GaiaConsent(asterism_client = AsterismClient.RCS, consent = Consent.NO_CONSENT))
            ),
            AsterismClient.RCS))
    }

    @Test fun absentConsentFailsClosed() {
        assertFalse(hasRequiredVerificationConsent(GetConsentResponse(), AsterismClient.RCS))
    }

    @Test fun unrelatedGaiaConsentDoesNotMaskDedicatedRcsConsent() {
        assertTrue(hasRequiredVerificationConsent(
            GetConsentResponse(
                rcs_consent = RcsConsent(consent = Consent.CONSENTED),
                gaia_consents = listOf(
                    GaiaConsent(asterism_client = AsterismClient.CONSTELLATION, consent = Consent.NO_CONSENT))
            ),
            AsterismClient.RCS))
    }

    @Test fun nonRcsClientCannotUseDedicatedRcsConsent() {
        assertFalse(hasRequiredVerificationConsent(
            GetConsentResponse(rcs_consent = RcsConsent(consent = Consent.CONSENTED)),
            AsterismClient.CONSTELLATION))
    }
}
