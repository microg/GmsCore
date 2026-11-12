/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class SourceTransferDiagnosticsTest {
    @Test fun diagnosticIncludesOnlyFixedPhaseAndClass() {
        val failure = IOException("synthetic-account@example.invalid token=never-log",
            IllegalArgumentException("synthetic secret"))
        assertEquals("ASSERTION failed (IOException)",
            sourceTransferFailureSummary(SourceTransferDiagnosticPhase.ASSERTION, failure.javaClass))
    }

    @Test fun everyPhaseProducesAStableBoundedDiagnostic() {
        for (phase in SourceTransferDiagnosticPhase.values()) {
            assertEquals("${phase.name} failed (SecurityException)",
                sourceTransferFailureSummary(phase, SecurityException::class.java))
        }
    }

    @Test fun everyReasonUsesOnlyAFixedCode() {
        for (reason in SourceTransferDiagnosticReason.values()) {
            val summary = sourceTransferFailureSummary(SourceTransferDiagnosticPhase.CONSENT,
                IllegalStateException::class.java, reason)
            assertEquals("CONSENT failed (IllegalStateException) reason=${reason.name}", summary)
            assertTrue(reason.name.matches(Regex("[A-Z_]{1,40}")))
            assertTrue(summary.length <= 128)
        }
    }

    @Test fun accountFailureDoesNotIncludeTheExceptionMessageOrCause() {
        val failure = IllegalStateException("synthetic-account@example.invalid token=never-log",
            IOException("synthetic peer/session material"))
        assertEquals("CONSENT failed (IllegalStateException) reason=CONSENT_NO_ELIGIBLE_ACCOUNT",
            sourceTransferFailureSummary(SourceTransferDiagnosticPhase.CONSENT, failure.javaClass,
                SourceTransferDiagnosticReason.CONSENT_NO_ELIGIBLE_ACCOUNT))
    }

    @Test fun protocolFailureCarriesOnlyItsOriginalStatusAndClosedReason() {
        val failure = SourceTransferProtocolException(10575, SourceTransferDiagnosticReason.PROTOCOL_RESULT_STATUS)
        assertEquals(10575, failure.statusCode)
        assertEquals("Account transfer failed (10575)", failure.message)
        assertEquals("RESULTS failed (SourceTransferProtocolException) reason=PROTOCOL_RESULT_STATUS",
            sourceTransferFailureSummary(SourceTransferDiagnosticPhase.RESULTS, failure.javaClass, failure.reason))
    }

    @Test fun protocolDiagnosticDoesNotIncludeCauseOrSuppressedMessages() {
        val failure = SourceTransferProtocolException(10589, SourceTransferDiagnosticReason.PROTOCOL_RESULT_ACCOUNT_FORMAT)
        failure.initCause(IOException("synthetic-account@example.invalid token=never-log"))
        failure.addSuppressed(IllegalArgumentException("synthetic peer material"))
        val summary = sourceTransferFailureSummary(SourceTransferDiagnosticPhase.RESULTS, failure.javaClass, failure.reason)
        assertEquals("RESULTS failed (SourceTransferProtocolException) reason=PROTOCOL_RESULT_ACCOUNT_FORMAT", summary)
        assertFalse(summary.contains("synthetic"))
        assertFalse(summary.contains("@"))
    }

}
