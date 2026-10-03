/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import java.security.interfaces.ECPublicKey
import java.io.IOException
import okio.ByteString.Companion.decodeHex
import org.junit.Assert.*
import org.junit.Test
import org.microg.gms.cryptauth.proto.BootstrapAssertionBody
import org.microg.gms.cryptauth.proto.BootstrapScreenLockSignals
import org.microg.gms.cryptauth.proto.BootstrapSourceSignals

class CryptAuthBootstrapAssertionTest {
    private val challenge = "001122ff".decodeHex()
    private val signals = BootstrapSourceSignals(android_id = "0123456789abcdef", device = "emulator", model = "sdk-test",
        sdk_version = "34", gms_version = "123", droidguard_result = "synthetic-attestation",
        screen_lock = BootstrapScreenLockSignals(secure_screen_lock = true, screen_lock_confirmed = true,
            screen_lock_state = 2, seconds_since_unlock = -1, seconds_since_screen_lock_setup = -1))
    // Independent Python stdlib oracle: manual protobuf varints, json.dumps(separators=(',',':')),
    // base64.urlsafe_b64encode(...).rstrip('='), hashlib.sha256. Synthetic values only.
    private val signalsHex = "0a10303132333435363738396162636465661208656d756c61746f721a0873646b2d74657374220233342a03313233321573796e7468657469632d6174746573746174696f6e3a1c08011001180220ffffffffffffffffff0128ffffffffffffffffff01"
    private val clientJson = "{\"typ\":\"navigator.id.getAssertion\",\"challenge\":\"ABEi_w\",\"source_device_signals\":\"ChAwMTIzNDU2Nzg5YWJjZGVmEghlbXVsYXRvchoIc2RrLXRlc3QiAjM0KgMxMjMyFXN5bnRoZXRpYy1hdHRlc3RhdGlvbjocCAEQARgCIP___________wEo____________AQ\",\"account_identifier\":\"test@example.invalid\"}"

    @Test fun deviceSignalsAndClientDataMatchIndependentFixture() {
        assertEquals(signalsHex, signals.encodeByteString().hex())
        val client = CryptAuthBootstrapAssertions.clientData("test@example.invalid", challenge, signals.encodeByteString())
        assertEquals(clientJson, client.utf8())
        assertEquals("a745f33e696d9198a2160199860504000bc6c6fbe4be3ef29c83f211ba30ff43", client.sha256().hex())
    }

    @Test fun assertionBodyMatchesIndependentProtobufFixture() {
        val client = CryptAuthBootstrapAssertions.clientData("test@example.invalid", challenge, signals.encodeByteString())
        val body = BootstrapAssertionBody(client_data_hash = client.sha256(), counter = 318368841243183923L, version = 1)
        assertEquals("0a20a745f33e696d9198a2160199860504000bc6c6fbe4be3ef29c83f211ba30ff4310b396e9cee3d8c4b5041801",
            body.encodeByteString().hex())
    }

    @Test fun clientDataEscapesAccountAndRejectsInvalidUnicode() {
        val json = CryptAuthBootstrapAssertions.clientData("a\"b\\c@example.invalid", challenge, ByteString.EMPTY).utf8()
        assertTrue(json.endsWith("\"account_identifier\":\"a\\\"b\\\\c@example.invalid\"}"))
        try {
            CryptAuthBootstrapAssertions.clientData("bad\ud800", challenge, ByteString.EMPTY)
            fail("Expected invalid Unicode rejection")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun rejectsEmptyOrOversizedChallengesAndRedactsAssertion() {
        for (invalid in listOf(ByteString.EMPTY, ByteString.of(*ByteArray(4097)))) {
            try { CryptAuthBootstrapAssertions.validateChallenge(invalid); fail("Expected challenge rejection") }
            catch (_: IllegalArgumentException) { }
        }
        val assertion = CryptAuthBootstrapAssertion(challenge, "abcdef".decodeHex())
        assertEquals("CryptAuthBootstrapAssertion(redacted)", assertion.toString())
    }

    @Test fun advertisedIdentityMustRemainTheConfirmedActiveIdentity() {
        val first = syntheticIdentity()
        val state = CryptAuthState("identity-fixture", keys = listOf(first))
        assertEquals(first.publicKey, CryptAuthBootstrapAssertions.identityPublicKey(state))
        CryptAuthBootstrapAssertions.requireIdentity(state, first.publicKey)
        val replacement = syntheticIdentity()
        try {
            CryptAuthBootstrapAssertions.requireIdentity(state.copy(keys = listOf(replacement)), first.publicKey)
            fail("Expected identity rotation rejection")
        } catch (_: IllegalArgumentException) { }
        for (invalid in listOf(state.copy(keys = emptyList()),
            state.copy(keys = listOf(first.copy(active = false))),
            state.copy(pendingKeys = listOf(first)))) {
            try {
                CryptAuthBootstrapAssertions.requireIdentity(invalid, first.publicKey)
                fail("Expected unconfirmed or revoked identity rejection")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun rejectsMalformedAdvertisedIdentityAndRedactsInfo() {
        val identity = syntheticIdentity()
        for (invalid in listOf(identity.copy(type = KeyType.RAW256),
            identity.copy(handle = ByteString.EMPTY), identity.copy(publicKey = ByteString.EMPTY))) {
            try {
                CryptAuthBootstrapAssertions.identityPublicKey(CryptAuthState("identity-fixture", keys = listOf(invalid)))
                fail("Expected malformed identity rejection")
            } catch (_: Exception) { }
        }
        assertEquals("CryptAuthBootstrapInfo(redacted)",
            CryptAuthBootstrapInfo("test@example.invalid", identity.publicKey).toString())
    }

    @Test fun confirmedKeySelectionOnlyReadsStateUnderTheEnrollmentMutex() = runBlocking {
        val identity = syntheticIdentity()
        val key = syntheticAuthzen()
        val state = CryptAuthState("selection-fixture", keys = listOf(identity, key),
            otpCounters = mapOf(key.handle to 123L))
        var reads = 0
        val store = object : CryptAuthStateStore {
            override fun read(): CryptAuthState {
                assertTrue(cryptAuthStateMutex.isLocked)
                reads++
                return state
            }
            override fun write(state: CryptAuthState) { fail("Selecting confirmed keys must not mutate state") }
        }
        assertEquals(key.handle, CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, identity.publicKey))
        assertEquals(1, reads)
        assertEquals(123L, state.otpCounters.getValue(key.handle))
    }

    @Test fun confirmedKeySelectionRejectsUnenrolledRevokedOrChangedIdentity() = runBlocking {
        val identity = syntheticIdentity()
        val key = syntheticAuthzen()
        val state = CryptAuthState("selection-fixture", keys = listOf(identity, key))
        for (invalid in listOf(state.copy(keys = emptyList()), state.copy(pendingKeys = listOf(key)),
            state.copy(keys = listOf(identity.copy(active = false), key)),
            state.copy(keys = listOf(syntheticIdentity(), key)))) {
            rejectedSelection(invalid, identity.publicKey)
        }
    }

    @Test fun confirmedKeySelectionRejectsInvalidAuthzenBeforeDroidGuardOrCounterUse() = runBlocking {
        val identity = syntheticIdentity()
        val key = syntheticAuthzen()
        for (keys in listOf(emptyList(), listOf(key.copy(active = false)), listOf(key, key),
            listOf(key.copy(type = KeyType.RAW128)), listOf(key.copy(secret = ByteString.EMPTY)),
            listOf(key.copy(publicKey = "unexpected".encodeUtf8())), listOf(key.copy(handle = "wrong".encodeUtf8())))) {
            rejectedSelection(CryptAuthState("selection-fixture", keys = listOf(identity) + keys), identity.publicKey)
        }
    }

    @Test fun confirmedKeySelectionPreservesStorageFailure() = runBlocking {
        val original = IOException("synthetic storage failure")
        val store = object : CryptAuthStateStore {
            override fun read(): CryptAuthState = throw original
            override fun write(state: CryptAuthState) { fail("Read failure must not cause a write") }
        }
        try {
            CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, syntheticIdentity().publicKey)
            fail("Expected read failure")
        } catch (error: IOException) { assertSame(original, error) }
    }

    @Test fun cancelledKeySelectionDoesNotReadOrWriteTheStore() = runBlocking {
        val store = object : CryptAuthStateStore {
            override fun read(): CryptAuthState = throw AssertionError("Cancelled selection read state")
            override fun write(state: CryptAuthState) { fail("Cancelled selection wrote state") }
        }
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().cancel()
            CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, ByteString.EMPTY)
        }
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test fun droidGuardNullAndEmptyResultsHaveOnlyTheEmptyDiagnostic() = runBlocking {
        for (result in listOf(null, "")) {
            rejectedDroidGuardResult(result, CryptAuthTransferDiagnosticReason.DROIDGUARD_EMPTY)
        }
    }

    @Test fun droidGuardOversizedResultsAreRejectedBeforeDecoding() = runBlocking {
        rejectedDroidGuardResult("A".repeat(CryptAuthBootstrapAssertions.MAX_DROIDGUARD_BYTES + 1),
            CryptAuthTransferDiagnosticReason.DROIDGUARD_TOO_LARGE)
    }

    @Test fun droidGuardBudgetMeasuresUtf8BytesAndPreservesTheBoundary() = runBlocking {
        val limit = CryptAuthBootstrapAssertions.MAX_DROIDGUARD_BYTES
        val boundary = "\u6f22".repeat(limit / 3) + "A".repeat(limit % 3)
        assertEquals(65536, boundary.toByteArray(Charsets.UTF_8).size)
        val lines = mutableListOf<String>()
        assertSame(boundary, CryptAuthBootstrapAssertions.checkedDroidGuardResult(boundary, CryptAuthTransferDiagnostics(lines::add)))
        assertTrue(lines.isEmpty())
        val tooLarge = boundary + "A"
        assertTrue(tooLarge.length < limit)
        rejectedDroidGuardResult(tooLarge, CryptAuthTransferDiagnosticReason.DROIDGUARD_TOO_LARGE)
    }

    @Test fun droidGuardLocallyEncodedErrorsAreNotTreatedAsAttestation() = runBlocking {
        // Independent JDK encoder; the production classifier uses Okio to decode provider output.
        val bytes = "ERROR : private-synthetic-account-and-challenge".toByteArray(Charsets.UTF_8) + byteArrayOf(-1, -2)
        for (result in listOf(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
            java.util.Base64.getEncoder().encodeToString(bytes))) {
            rejectedDroidGuardResult(result, CryptAuthTransferDiagnosticReason.DROIDGUARD_LOCAL_ERROR)
        }
    }

    @Test fun boundedDroidGuardProviderOutputIsPreservedWithoutLogging() = runBlocking {
        val lines = mutableListOf<String>()
        val diagnostic = CryptAuthTransferDiagnostics(lines::add)
        for (result in listOf("A", "A".repeat(CryptAuthBootstrapAssertions.MAX_DROIDGUARD_BYTES),
            java.util.Base64.getEncoder().encodeToString("opaque ERROR : embedded bytes".toByteArray(Charsets.UTF_8)))) {
            assertNull(CryptAuthBootstrapAssertions.droidGuardFailureReason(result))
            assertSame(result, CryptAuthBootstrapAssertions.checkedDroidGuardResult(result, diagnostic))
        }
        assertTrue(lines.isEmpty())
    }

    @Test fun signalValidationAlsoRejectsLocalDroidGuardErrors() {
        val localError = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("ERROR : synthetic local failure".toByteArray(Charsets.UTF_8))
        for (result in listOf(null, "", "A".repeat(CryptAuthBootstrapAssertions.MAX_DROIDGUARD_BYTES + 1), localError)) {
            try {
                CryptAuthBootstrapAssertions.validateSignals(signals.copy(droidguard_result = result))
                fail("Expected unavailable DroidGuard result rejection")
            } catch (error: IllegalArgumentException) {
                assertEquals("Invalid bootstrap device signals", error.message)
            }
        }
        CryptAuthBootstrapAssertions.validateSignals(signals)
    }

    private suspend fun rejectedDroidGuardResult(result: String?, reason: CryptAuthTransferDiagnosticReason) {
        assertEquals(reason, CryptAuthBootstrapAssertions.droidGuardFailureReason(result))
        val lines = mutableListOf<String>()
        try {
            CryptAuthBootstrapAssertions.checkedDroidGuardResult(result, CryptAuthTransferDiagnostics(lines::add))
            fail("Expected unavailable DroidGuard result rejection")
        } catch (error: IllegalStateException) {
            assertEquals("Bootstrap device verification unavailable", error.message)
        }
        assertEquals(listOf("${reason.name} failed (IllegalStateException)"), lines)
    }

    private suspend fun rejectedSelection(state: CryptAuthState, publicKey: ByteString) {
        val store = object : CryptAuthStateStore {
            override fun read() = state
            override fun write(state: CryptAuthState) { fail("Rejected selection must not write") }
        }
        try {
            CryptAuthBootstrapAssertions.readConfirmedAuthzenHandle(store, publicKey)
            fail("Expected invalid confirmed state rejection")
        } catch (_: IllegalArgumentException) { }
    }

    private fun syntheticAuthzen() = ByteArray(32) { it.toByte() }.toByteString().let { secret ->
        CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(), KeyType.RAW256, secret = secret, active = true)
    }

    private fun syntheticIdentity(): CryptAuthKey {
        val pair = CryptAuthCrypto.generateKeyPair()
        return CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
            CryptAuthCrypto.publicKey(pair.public as ECPublicKey), pair.private.encoded.toByteString(), true)
    }
}
