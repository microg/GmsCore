package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.constellation.core.proto.ConsentVersion
import org.microg.gms.constellation.core.proto.RequestTrigger

class RcsAutoConsentRequestSemanticsTest {
    @Test
    fun preservesRequestedConsentVersionButUsesRcsConsentRecordVersion() {
        val semantics = resolveRcsAutoConsentRequestSemantics(
            ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI
        )

        assertEquals(ConsentVersion.RCS_CONSENT, semantics.rcsConsentVersion)
        assertEquals(
            ConsentVersion.RCS_DEFAULT_ON_LEGAL_FYI,
            semantics.requestConsentVersion
        )
    }

    @Test
    fun usesConsentApiTrigger() {
        val semantics = resolveRcsAutoConsentRequestSemantics(
            ConsentVersion.RCS_DEFAULT_ON_OUT_OF_BOX
        )

        assertEquals(
            RequestTrigger.Type.CONSENT_API_TRIGGER,
            semantics.triggerType
        )
    }
}
