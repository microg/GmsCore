package org.microg.gms.asterism.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.constellation.core.proto.AsterismClient
import org.microg.gms.constellation.core.proto.ConsentVersion

class RcsConsentFastPathTest {
    @Test fun rcsConsentVersionUsesMinimalRcsFastPath() {
        assertTrue(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.RCS_CONSENT))
    }

    @Test fun existingRcsDefaultVersionsRemainFastPath() {
        assertTrue(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI))
        assertTrue(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX))
        assertTrue(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI_IN_SETTINGS))
    }

    @Test fun everyRcsOnlyConsentVersionIsClassified() {
        listOf(
            ConsentVersion.RCS_CONSENT,
            ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI,
            ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX,
            ConsentVersion.RCS_SAMSUNG_UNFREEZE,
            ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI_IN_SETTINGS
        ).forEach { consentVersion ->
            assertTrue(isRcsSpecificConsentVersion(consentVersion))
        }
    }

    @Test fun samsungUnfreezeIsRcsOnlyButNotGenericFastPath() {
        assertTrue(isRcsSpecificConsentVersion(ConsentVersion.RCS_SAMSUNG_UNFREEZE))
        assertFalse(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.RCS_SAMSUNG_UNFREEZE))
    }

    @Test fun nonRcsClientCannotUseRcsFastPath() {
        assertFalse(isRcsConsentFastPath(AsterismClient.CONSTELLATION, ConsentVersion.RCS_CONSENT))
        assertFalse(
            isRcsConsentFastPath(
                AsterismClient.CONSTELLATION,
                ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI_IN_SETTINGS
            )
        )
    }

    @Test fun unspecifiedConsentVersionIsNotRcsSpecificOrFastPath() {
        assertFalse(isRcsSpecificConsentVersion(ConsentVersion.CONSENT_VERSION_UNSPECIFIED))
        assertFalse(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.CONSENT_VERSION_UNSPECIFIED))
    }
}
