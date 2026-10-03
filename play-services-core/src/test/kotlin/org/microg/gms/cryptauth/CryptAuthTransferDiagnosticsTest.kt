/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CryptAuthTransferDiagnosticsTest {
    @Test fun everyCheckpointHasAClosedAndBoundedOutput() {
        val kinds = listOf(
            CancellationException() to "CancellationException",
            SecurityException() to "SecurityException",
            IllegalArgumentException() to "IllegalArgumentException",
            IllegalStateException() to "IllegalStateException",
            IOException() to "IOException",
            Exception() to "OtherException"
        )
        for (reason in CryptAuthTransferDiagnosticReason.values()) {
            assertTrue(reason.name.matches(Regex("[A-Z_]+")))
            for ((error, kind) in kinds) {
                val line = cryptAuthTransferFailureSummary(reason, error)
                assertEquals("${reason.name} failed ($kind)", line)
                assertTrue(line.length <= 80)
            }
        }
    }

    @Test fun messagesCausesAndCustomClassNamesCannotEnterOutput() {
        class SecretAccountAndTokenException : Exception("private-account private-token", IOException("private-key")) {
            override fun toString(): String = error("Diagnostic must not stringify an exception")
        }
        val line = cryptAuthTransferFailureSummary(CryptAuthTransferDiagnosticReason.OAUTH_AUTHENTICATION,
            SecretAccountAndTokenException())
        assertEquals("OAUTH_AUTHENTICATION failed (OtherException)", line)
    }

    @Test fun assertionAndDroidGuardFailuresUseOnlyFixedReasonsAndPreserveTheException() = runBlocking {
        val reasons = listOf(CryptAuthTransferDiagnosticReason.ASSERTION_CHALLENGE,
            CryptAuthTransferDiagnosticReason.ASSERTION_IDENTITY, CryptAuthTransferDiagnosticReason.ASSERTION_KEYS,
            CryptAuthTransferDiagnosticReason.ASSERTION_SCREEN_LOCK, CryptAuthTransferDiagnosticReason.ASSERTION_CHECKIN,
            CryptAuthTransferDiagnosticReason.DROIDGUARD_REQUEST, CryptAuthTransferDiagnosticReason.DROIDGUARD_RESULT,
            CryptAuthTransferDiagnosticReason.ASSERTION_SIGNALS, CryptAuthTransferDiagnosticReason.ASSERTION_COUNTER_AND_SIGN,
            CryptAuthTransferDiagnosticReason.DROIDGUARD_EMPTY, CryptAuthTransferDiagnosticReason.DROIDGUARD_TOO_LARGE,
            CryptAuthTransferDiagnosticReason.DROIDGUARD_LOCAL_ERROR)
        for (reason in reasons) {
            val original = IOException("private account challenge and attestation")
            val lines = mutableListOf<String>()
            try {
                CryptAuthTransferDiagnostics(lines::add).at(reason) { throw original }
                fail("Expected original operation failure")
            } catch (error: IOException) { assertSame(original, error) }
            assertEquals(listOf("${reason.name} failed (IOException)"), lines)
        }
    }

    @Test fun successfulOperationReturnsTheSameValueWithoutLogging() = runBlocking {
        val lines = mutableListOf<String>()
        val diagnostic = CryptAuthTransferDiagnostics(lines::add)
        val result = Any()
        assertSame(result, diagnostic.at(CryptAuthTransferDiagnosticReason.TRANSFER_KEYS) { result })
        assertTrue(lines.isEmpty())
    }

    @Test fun nestedCheckpointsRethrowTheOriginalAndRecordOnlyTheInnermostFailure() = runBlocking {
        val lines = mutableListOf<String>()
        val diagnostic = CryptAuthTransferDiagnostics(lines::add)
        val original = IllegalStateException("private-state")
        try {
            diagnostic.at(CryptAuthTransferDiagnosticReason.KEY_ENROLLMENT) {
                diagnostic.at(CryptAuthTransferDiagnosticReason.ENROLL_KEYS) { throw original }
            }
            fail("Failure must be propagated")
        } catch (error: Exception) { assertSame(original, error) }
        assertEquals(listOf("ENROLL_KEYS failed (IllegalStateException)"), lines)
    }

    @Test fun cancellationIsPreservedWithoutWrappingOrRetrying() = runBlocking {
        val lines = mutableListOf<String>()
        val diagnostic = CryptAuthTransferDiagnostics(lines::add)
        val original = CancellationException("private-session")
        var attempts = 0
        try {
            diagnostic.at(CryptAuthTransferDiagnosticReason.SYNC_KEYS) { attempts++; throw original }
            fail("Cancellation must be propagated")
        } catch (error: CancellationException) { assertSame(original, error) }
        assertEquals(1, attempts)
        assertEquals(listOf("SYNC_KEYS failed (CancellationException)"), lines)
    }

    @Test fun brokenDiagnosticSinkCannotReplaceAnOperationFailure() = runBlocking {
        val diagnostic = CryptAuthTransferDiagnostics { throw IOException("logging failed") }
        val original = SecurityException("private-caller")
        try {
            diagnostic.at(CryptAuthTransferDiagnosticReason.AUTHORIZATION_INITIAL) { throw original }
            fail("Authorization failure must be propagated")
        } catch (error: Exception) { assertSame(original, error) }
    }

    @Test fun concurrentFailuresProduceOnlyOneDiagnostic() {
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val diagnostic = CryptAuthTransferDiagnostics(lines::add)
        val start = CountDownLatch(1)
        val finished = CountDownLatch(8)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val threads = (1..8).map {
            Thread {
                try {
                    check(start.await(3, TimeUnit.SECONDS))
                    val original = IOException("private-response")
                    try {
                        runBlocking { diagnostic.at(CryptAuthTransferDiagnosticReason.SYNC_KEYS) { throw original } }
                        fail("Operation failure must be propagated")
                    } catch (error: Exception) { assertSame(original, error) }
                } catch (error: Throwable) { failures.add(error) }
                finally { finished.countDown() }
            }.apply { this.start() }
        }
        start.countDown()
        try { assertTrue("Workers must finish", finished.await(5, TimeUnit.SECONDS)) }
        finally { threads.forEach { it.join(1000) } }
        assertTrue(failures.isEmpty())
        assertEquals(listOf("SYNC_KEYS failed (IOException)"), lines)
    }

    @Test fun aSeparateAttemptCanReportItsOwnFailure() = runBlocking {
        val lines = mutableListOf<String>()
        repeat(2) {
            try {
                CryptAuthTransferDiagnostics(lines::add).at(CryptAuthTransferDiagnosticReason.CHECKIN_REQUIRED) {
                    throw IllegalStateException()
                }
            } catch (_: IllegalStateException) { }
        }
        assertEquals(listOf("CHECKIN_REQUIRED failed (IllegalStateException)",
            "CHECKIN_REQUIRED failed (IllegalStateException)"), lines)
    }
}
