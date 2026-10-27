/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.test.AndroidTestCase

@Suppress("DEPRECATION")
class SourceTransferConsentTest : AndroidTestCase() {
    fun testClaimAndCredentialResultAreSingleUse() {
        var accepted = 0
        val request = request({ accepted++ })
        SourceTransferConsent.add(request)
        assertSame(request, SourceTransferConsent.claim(request.token))
        assertNull(SourceTransferConsent.claim(request.token))
        val challenge = request.beginCredentialConfirmation(0)
        request.credentialConfirmed(challenge)
        assertEquals(1, accepted)
        assertNull(SourceTransferConsent.claim(request.token))
        try { request.credentialConfirmed(challenge); fail("Reused credential result") }
        catch (_: IllegalStateException) { }
        assertEquals(1, accepted)
    }

    fun testCancelledSessionRejectsEarlierCredentialResult() {
        var accepted = false
        var cancelled = 0
        val request = request({ accepted = true }, { cancelled++ })
        val challenge = request.beginCredentialConfirmation(0)
        request.cancel()
        try { request.credentialConfirmed(challenge); fail("Accepted cancelled confirmation") }
        catch (_: IllegalStateException) { }
        assertFalse(accepted)
        assertEquals(1, cancelled)
    }

    fun testCredentialChallengeCannotCrossSessions() {
        var accepted = false
        val first = request({ accepted = true })
        val second = request({ accepted = true })
        val firstChallenge = first.beginCredentialConfirmation(0)
        second.beginCredentialConfirmation(0)
        try { second.credentialConfirmed(firstChallenge); fail("Accepted other session result") }
        catch (_: IllegalStateException) { }
        assertFalse(accepted)
        first.cancel()
        second.cancel()
    }

    fun testSelectionCannotChangeAfterCredentialStarted() {
        val request = request({ })
        request.beginCredentialConfirmation(0)
        try { request.beginCredentialConfirmation(0); fail("Replaced pending selection") }
        catch (_: IllegalStateException) { }
        request.cancel()
    }

    fun testExpiredSessionCannotConfirm() {
        var valid = true
        var accepted = false
        val request = SourceTransferConsent.Request(listOf(Account("test@example.invalid", "com.google")),
            { check(valid) }, { _, _ -> accepted = true }, { })
        val challenge = request.beginCredentialConfirmation(0)
        valid = false
        try { request.credentialConfirmed(challenge); fail("Accepted expired confirmation") }
        catch (_: IllegalStateException) { }
        assertFalse(accepted)
        request.cancel()
    }

    fun testConfirmedCredentialKeepsProgressOpenUntilSessionEnds() {
        var accepted = 0
        var terminal = 0
        val request = request({ accepted++ })
        request.observeTerminal { terminal++ }
        val challenge = request.beginCredentialConfirmation(0)
        request.credentialConfirmed(challenge)
        assertEquals(1, accepted)
        assertFalse(request.isTerminal())
        assertEquals(0, terminal)
        try { request.beginCredentialConfirmation(0); fail("Accepted another PIN challenge") }
        catch (_: IllegalStateException) { }
        request.invalidate()
        assertTrue(request.isTerminal())
        assertEquals(1, terminal)
    }

    fun testClosingProgressAfterPinCancelsSessionExactlyOnce() {
        var accepted = 0
        var cancelled = 0
        var terminal = 0
        val request = request({ accepted++ }, { cancelled++ })
        request.observeTerminal { terminal++ }
        val challenge = request.beginCredentialConfirmation(0)
        request.credentialConfirmed(challenge)
        request.cancel()
        request.cancel()
        request.invalidate()
        assertEquals(1, accepted)
        assertEquals(1, cancelled)
        assertEquals(1, terminal)
        assertTrue(request.isTerminal())
        try { request.credentialConfirmed(challenge); fail("Reused PIN after cancellation") }
        catch (_: IllegalStateException) { }
    }

    fun testSessionEndingAfterPinDoesNotInvokeUserCancellation() {
        var cancelled = 0
        var terminal = 0
        val request = request({ }, { cancelled++ })
        request.observeTerminal { terminal++ }
        request.credentialConfirmed(request.beginCredentialConfirmation(0))
        request.invalidate()
        request.cancel() // Activity destruction after a real terminal event.
        assertEquals(0, cancelled)
        assertEquals(1, terminal)
    }

    fun testAlreadyTerminalSessionNotifiesLateObserver() {
        var terminal = 0
        val request = request({ })
        request.invalidate()
        request.observeTerminal { terminal++ }
        assertEquals(1, terminal)
        assertTrue(request.isTerminal())
        assertNull(SourceTransferConsent.claim(request.token))
    }

    fun testDetachedWindowIsNotRetainedForTerminalNotification() {
        var terminal = 0
        val request = request({ })
        val observer: () -> Unit = { terminal++ }
        request.observeTerminal(observer)
        request.removeTerminalObserver(observer)
        request.invalidate()
        assertEquals(0, terminal)
    }

    fun testSessionClosingDuringAuthorizationCannotReopenProgress() {
        var terminal = 0
        lateinit var request: SourceTransferConsent.Request
        request = request({ request.invalidate() })
        request.observeTerminal { terminal++ }
        request.credentialConfirmed(request.beginCredentialConfirmation(0))
        assertTrue(request.isTerminal())
        assertEquals(1, terminal)
        assertNull(SourceTransferConsent.claim(request.token))
    }

    fun testFailedAuthorizationCancelsSessionAndPreservesFailure() {
        val original = SecurityException("synthetic authorization failure")
        var cancelled = 0
        var terminal = 0
        val request = request({ throw original }, { cancelled++ })
        request.observeTerminal { terminal++ }
        try { request.credentialConfirmed(request.beginCredentialConfirmation(0)); fail("Failure swallowed") }
        catch (failure: SecurityException) { assertSame(original, failure) }
        assertEquals(1, cancelled)
        assertEquals(1, terminal)
        assertTrue(request.isTerminal())
    }

    fun testAuthorizationAndTerminalCallbacksRunOutsideRequestMonitor() {
        lateinit var request: SourceTransferConsent.Request
        request = request({ assertFalse(Thread.holdsLock(request)) })
        request.observeTerminal { assertFalse(Thread.holdsLock(request)) }
        request.credentialConfirmed(request.beginCredentialConfirmation(0))
        request.invalidate()
    }

    fun testTerminalRequestCannotBeRegisteredAgain() {
        val request = request({ })
        request.invalidate()
        try { SourceTransferConsent.add(request); fail("Registered closed request") }
        catch (_: IllegalStateException) { }
        assertNull(SourceTransferConsent.claim(request.token))
    }

    fun testBrokenWindowCallbackCannotPreventCancellationCleanup() {
        var cancelled = 0
        val request = request({ }, { cancelled++ })
        SourceTransferConsent.add(request)
        request.observeTerminal { throw IllegalStateException("Window destroyed") }
        request.cancel()
        assertEquals(1, cancelled)
        assertTrue(request.isTerminal())
        assertNull(SourceTransferConsent.claim(request.token))
    }

    fun testVerificationObserverSurvivesPinAndCancellationClosesRequest() {
        val request = request({ })
        val auth = object : org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization {
            override val account = Account("test@example.invalid", "com.google")
            override fun enforceCurrent() { }
        }
        val web = SourceTransferWebRequest(auth, "https://accounts.google.com/fixture")
        var observed = 0
        request.observeVerification { assertSame(web, it); observed++ }
        request.credentialConfirmed(request.beginCredentialConfirmation(0))
        request.showVerification(web)
        assertEquals(1, observed)
        assertFalse(web.result.isCompleted)
        request.cancel()
        assertTrue(web.result.isCancelled)
    }

    fun testVerificationCannotStartBeforePinOrForDifferentAccount() {
        val request = request({ })
        val auth = object : org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization {
            override val account = Account("other@example.invalid", "com.google")
            override fun enforceCurrent() { }
        }
        var observed = 0
        request.observeVerification { observed++ }
        val web = SourceTransferWebRequest(auth, "https://accounts.google.com/fixture")
        try { request.showVerification(web); fail("Verification before PIN") } catch (_: IllegalStateException) { }
        request.credentialConfirmed(request.beginCredentialConfirmation(0))
        try { request.showVerification(web); fail("Verification changed account") } catch (_: IllegalStateException) { }
        assertEquals(0, observed)
        request.cancel()
        web.cancel()
    }

    fun testWebCheckpointAfterCancellationOrRevocationIsRejected() {
        var valid = true
        val auth = object : org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization {
            override val account = Account("test@example.invalid", "com.google")
            override fun enforceCurrent() { check(valid) }
        }
        val cancelled = SourceTransferWebRequest(auth, "https://accounts.google.com/fixture")
        cancelled.cancel()
        try { cancelled.complete("synthetic"); fail("Cancelled checkpoint") } catch (_: IllegalStateException) { }
        val revoked = SourceTransferWebRequest(auth, "https://accounts.google.com/fixture")
        valid = false
        try { revoked.complete("synthetic"); fail("Revoked checkpoint") } catch (_: IllegalStateException) { }
        assertFalse(revoked.result.isCompleted)
        revoked.cancel()
    }

    private fun request(accepted: () -> Unit, rejected: () -> Unit = {}) = SourceTransferConsent.Request(
        listOf(Account("test@example.invalid", "com.google")), { }, { _, _ -> accepted() }, rejected)
}
