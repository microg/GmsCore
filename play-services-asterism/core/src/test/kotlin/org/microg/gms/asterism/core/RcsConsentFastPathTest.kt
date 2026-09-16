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

    @Test fun nonRcsClientCannotUseRcsFastPath() {
        assertFalse(isRcsConsentFastPath(AsterismClient.CONSTELLATION, ConsentVersion.RCS_CONSENT))
    }

    @Test fun unspecifiedConsentVersionIsNotFastPath() {
        assertFalse(isRcsConsentFastPath(AsterismClient.RCS, ConsentVersion.CONSENT_VERSION_UNSPECIFIED))
    }
}
