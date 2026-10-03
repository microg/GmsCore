/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.ClientDirective
import cryptauthv2.EnrollKeysRequest
import cryptauthv2.EnrollKeysResponse
import cryptauthv2.KeyDirective
import cryptauthv2.KeyType
import cryptauthv2.PolicyReference
import cryptauthv2.SyncKeysRequest
import cryptauthv2.SyncKeysResponse
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyAction
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyCreation
import cryptauthv2.SyncKeysResponse.SyncSingleKeyResponse.KeyStorageLevel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECPrivateKeySpec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CryptAuthEnrollmentTest {
    // RFC 5869, Appendix A.1. This is also used by Chromium's public key-proof tests.
    @Test fun hkdfMatchesRfc5869() {
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
            CryptAuthCrypto.hkdf(ByteArray(22) { 0x0b }, "000102030405060708090a0b0c".decodeHex().toByteArray(),
                "f0f1f2f3f4f5f6f7f8f9".decodeHex().toByteArray(), 42).toByteString().hex())
    }

    // SEC 2 secp256r1 generator (private scalar 1); Google's SecureMessage wire format.
    private val generator = ("080112440a20" +
        "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296" + "1220" +
        "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5").decodeHex()

    @Test fun p256EncodingMatchesPublicWireFormat() {
        assertEquals(generator, CryptAuthCrypto.publicKey(CryptAuthCrypto.parsePublicKey(generator)))
    }

    @Test fun ecdhUsesSha256OfFixedWidthSharedSecret() {
        val publicKey = CryptAuthCrypto.parsePublicKey(generator)
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger.ONE, publicKey.params))
        // ECDH(1, G) has x coordinate G.x, padded to 32 bytes before hashing.
        val expected = "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296".decodeHex().sha256()
        assertEquals(expected, CryptAuthCrypto.agreement(KeyPair(publicKey, privateKey), generator).toByteString())
    }

    @Test fun rejectsInvalidCurvePointsAndWrongKeyType() {
        assertRejected { CryptAuthCrypto.parsePublicKey("080112060a0100120100".decodeHex()) }
        assertRejected { CryptAuthCrypto.parsePublicKey("080212060a0100120100".decodeHex()) }
        assertRejected { CryptAuthCrypto.parsePublicKey("080112060a01ff120100".decodeHex()) }
    }

    @Test fun initialEnrollmentSendsProofsAndPersistsOnlyAfterServerSuccess() = runBlocking {
        val store = MemoryStore()
        val server = Server(store)
        CryptAuthEnrollment(server, store).enroll("screenlock-metadata".encodeUtf8())
        assertEquals(2, store.writes)
        assertEquals(2, store.state.keys.size)
        assertEquals(0, server.request!!.sync_single_key_requests[0].key_handles.size)
        assertEquals("screenlock-metadata".encodeUtf8(), server.request!!.client_app_metadata)
        val enrollment = server.enrollment!!
        assertEquals(server.response.random_session_id, enrollment.random_session_id)
        val identity = enrollment.enroll_single_key_requests.single { it.key_name == "PublicKey" }
        assertEquals("device_key".encodeUtf8(), identity.new_key_handle)
        assertTrue(verifyIdentityProof(identity, enrollment.random_session_id))
        assertFalse(verifyIdentityProof(identity, "different-session".encodeUtf8()))
        val symmetric = enrollment.enroll_single_key_requests.single { it.key_name == "authzen" }
        assertEquals(ByteString.EMPTY, symmetric.key_material)
        val shared = CryptAuthCrypto.agreement(server.pair, enrollment.client_ephemeral_dh)
        val secret = CryptAuthCrypto.derive(shared, "authzen", 32)
        assertEquals(secret, store.state.keys.single { it.name == "authzen" }.secret)
        assertEquals(secret.sha256().base64Url().encodeUtf8(), symmetric.new_key_handle)
        val proofKey = CryptAuthCrypto.hkdf(secret.toByteArray(), "CryptAuth Key Proof".toByteArray(), "authzen".toByteArray(), 32)
        assertEquals(Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(proofKey, "HmacSHA256")); doFinal(enrollment.random_session_id.toByteArray())
        }.toByteString(), symmetric.key_proof)
    }

    @Test fun networkFailureKeepsRevocationsAndPreservesUnconfirmedMaterial() = runBlocking {
        val store = enrolledStore()
        store.state = store.state.copy(otpCounters = mapOf(store.state.keys.last().handle to 123L))
        val before = store.state
        val server = Server(store).apply { failEnrollment = true; response = rotationResponse(store.state) }
        try { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY); fail("Expected network failure") }
        catch (expected: IOException) { }
        assertEquals(listOf(before.keys.first()), store.state.keys)
        assertTrue(store.state.otpCounters.isEmpty())
        assertEquals(2, store.state.pendingKeys.size)
        assertEquals(before.keys.first().secret, store.state.pendingKeys.first().secret)
    }

    @Test fun repeatedIdentityEnrollmentKeepsTheSamePrivateKey() = runBlocking {
        val store = enrolledStore()
        val identity = store.state.keys.single { it.name == "PublicKey" }
        val server = Server(store).apply { response = rotationResponse(store.state) }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertEquals(identity, store.state.keys.single { it.name == "PublicKey" })
        assertEquals(identity.publicKey, server.enrollment!!.enroll_single_key_requests[0].key_material)
        assertEquals(store.state.directives["PublicKey"]!!.policy_reference,
            server.request!!.sync_single_key_requests[0].policy_reference)
    }

    @Test fun noCreationReportsMetadataAndFinishesWithoutEmptyEnrollment() = runBlocking {
        val store = enrolledStore()
        val authzen = store.state.keys.single { it.name == "authzen" }
        store.state = store.state.copy(otpCounters = mapOf(authzen.handle to 321L))
        val keys = store.state.keys
        val server = Server(store).apply {
            failEnrollment = true // Any additional EnrollKeys POST would fail this test.
            response = response.copy(sync_single_key_responses = keys.map { SyncSingleKeyResponse(
                key_actions = listOf(KeyAction.ACTIVATE), key_creation = KeyCreation.NONE) },
                client_directive = response.client_directive!!.copy(checkin_delay_millis = 7200000))
        }
        CryptAuthEnrollment(server, store).enroll("updated-metadata".encodeUtf8())
        assertEquals(keys, store.state.keys)
        assertNull(server.enrollment)
        assertEquals("updated-metadata".encodeUtf8(), server.request!!.client_app_metadata)
        assertEquals(server.response.client_directive, store.state.clientDirective)
        assertEquals(321L, store.state.otpCounters.getValue(authzen.handle))
    }

    @Test fun explicitIdentityRevocationDoesNotReuseDeletedPrivateKey() = runBlocking {
        val store = enrolledStore()
        val previous = store.state.keys.first().secret
        val server = Server(store).apply {
            response = rotationResponse(store.state).let { response -> response.copy(
                sync_single_key_responses = response.sync_single_key_responses.map { it.copy(key_actions = listOf(KeyAction.DELETE)) }
            ) }
            failEnrollment = true
        }
        try { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY); fail("Expected lost response") }
        catch (expected: IOException) { }
        assertTrue(store.state.keys.isEmpty())
        assertNotEquals(previous, store.state.pendingKeys.first().secret)
    }

    @Test fun activationImplicitlyDeactivatesThePreviousKeyWithNoopAction() = runBlocking {
        val store = enrolledStore()
        val secret = ByteArray(32) { 7 }.toByteString()
        val other = CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(), KeyType.RAW256,
            secret = secret, active = false)
        store.state = store.state.copy(keys = store.state.keys + other)
        val server = Server(store).apply { response = response.copy(sync_single_key_responses = listOf(
            SyncSingleKeyResponse(key_actions = listOf(KeyAction.ACTIVATE)),
            SyncSingleKeyResponse(key_actions = listOf(KeyAction.KEY_ACTION_UNSPECIFIED, KeyAction.ACTIVATE))
        )) }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertEquals(other.handle, store.state.keys.single { it.name == "authzen" && it.active }.handle)
    }

    @Test fun malformedSessionAndCountsDoNotReachEnrollment() = runBlocking {
        for (transform in listOf<(SyncKeysResponse) -> SyncKeysResponse>(
            { it.copy(random_session_id = ByteString.EMPTY) },
            { it.copy(sync_single_key_responses = it.sync_single_key_responses.take(1)) },
            { it.copy(client_directive = null) },
            { it.copy(server_status = SyncKeysResponse.ServerStatus.SERVER_OVERLOADED) }
        )) {
            val store = MemoryStore()
            val server = Server(store).apply { response = transform(response) }
            assertRejectedSuspend { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY) }
            assertNull(server.enrollment)
            assertEquals(0, store.writes)
        }
    }

    @Test fun unsupportedSecurityPoliciesFailClosed() = runBlocking {
        for (transform in listOf<(SyncSingleKeyResponse) -> SyncSingleKeyResponse>(
            { it.copy(key_storage_level = KeyStorageLevel.TRUSTED_EXECUTION_ENVIRONMENT) },
            { it.copy(hardware_user_presence_required = true) },
            { it.copy(user_verification_required = true) },
            { it.copy(key_type = KeyType.CURVE25519) },
            { it.copy(key_creation = KeyCreation.INACTIVE) },
            { it.copy(key_directive = KeyDirective(crossproof_key_names = listOf("other"))) },
            { it.copy(unknownFields = "1003".decodeHex()) }
        )) {
            val store = MemoryStore()
            val server = Server(store).apply { response = response.copy(
                sync_single_key_responses = listOf(transform(response.sync_single_key_responses[0]), response.sync_single_key_responses[1])) }
            assertRejectedSuspend { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY) }
            assertNull(server.enrollment)
            assertEquals(0, store.writes)
        }
    }

    @Test fun cancellationAfterServerResponsePreservesPendingKeysWithoutActivating() = runBlocking {
        val store = MemoryStore()
        val server = Server(store).apply { cancelEnrollment = true }
        val job = launch { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY) }
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(1, store.writes)
        assertTrue(store.state.keys.isEmpty())
        assertEquals(2, store.state.pendingKeys.size)
    }

    @Test fun persistenceFailureIsNotReportedAsSuccess() = runBlocking {
        val store = MemoryStore().apply { failWrite = true }
        val server = Server(store)
        try { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY); fail("Expected storage failure") }
        catch (expected: IOException) { }
        assertTrue(store.state.keys.isEmpty())
        assertNull(server.enrollment)
    }

    @Test fun serverSuccessFollowedByStorageFailureKeepsPendingMaterial() = runBlocking {
        val store = MemoryStore().apply { failAfterWrites = 1 }
        val server = Server(store)
        try { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY); fail("Expected storage failure") }
        catch (expected: IOException) { }
        assertNotNull(server.enrollment)
        assertTrue(store.state.keys.isEmpty())
        assertEquals(2, store.state.pendingKeys.size)
        assertEquals(server.enrollment!!.enroll_single_key_requests.first().key_material, store.state.pendingKeys.first().publicKey)
    }

    @Test fun lostEnrollmentResponseIsReconciledWithoutReplacingIdentity() = runBlocking {
        val store = MemoryStore()
        val failedServer = Server(store).apply { failEnrollment = true }
        try { CryptAuthEnrollment(failedServer, store).enroll(ByteString.EMPTY); fail("Expected lost response") }
        catch (expected: IOException) { }
        val pending = store.state.pendingKeys
        assertEquals(2, pending.size)
        // Simulate process death and load only durable state.
        store.state = CryptAuthStateCodec.decode(CryptAuthStateCodec.encode(store.state))
        val server = Server(store).apply {
            failEnrollment = true
            response = response.copy(sync_single_key_responses = pending.map { SyncSingleKeyResponse(
                key_actions = listOf(KeyAction.ACTIVATE), key_creation = KeyCreation.NONE) })
        }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertEquals(pending, store.state.keys)
        assertTrue(store.state.pendingKeys.isEmpty())
        assertEquals(pending.first().handle, server.request!!.sync_single_key_requests.first().key_handles.single())
        assertNull(server.enrollment)
    }

    @Test fun noCreationDoesNotConfirmPendingKeyFromANoop() = runBlocking {
        val store = enrolledStore()
        val secret = ByteArray(32) { 9 }.toByteString()
        val pending = CryptAuthKey("authzen", secret.sha256().base64Url().encodeUtf8(), KeyType.RAW256,
            secret = secret, active = false)
        store.state = store.state.copy(pendingKeys = listOf(pending))
        val server = Server(store).apply {
            failEnrollment = true
            response = response.copy(sync_single_key_responses = listOf(
                SyncSingleKeyResponse(key_actions = listOf(KeyAction.ACTIVATE)),
                SyncSingleKeyResponse(key_actions = listOf(KeyAction.ACTIVATE, KeyAction.KEY_ACTION_UNSPECIFIED))
            ))
        }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertNull(server.enrollment)
        assertEquals(listOf(pending), store.state.pendingKeys)
        assertFalse(store.state.keys.any { it.handle == pending.handle })
        assertRejected { CryptAuthBootstrapAssertions.identityPublicKey(store.state) }
    }

    @Test fun noCreationStillAppliesRevocationAndRemovesItsCounter() = runBlocking {
        val store = enrolledStore()
        val authzen = store.state.keys.single { it.name == "authzen" }
        store.state = store.state.copy(otpCounters = mapOf(authzen.handle to 123L))
        val server = Server(store).apply {
            failEnrollment = true
            response = response.copy(sync_single_key_responses = listOf(
                SyncSingleKeyResponse(key_actions = listOf(KeyAction.ACTIVATE)),
                SyncSingleKeyResponse(key_actions = listOf(KeyAction.DELETE))
            ))
        }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertNull(server.enrollment)
        assertEquals(listOf("PublicKey"), store.state.keys.map { it.name })
        assertTrue(store.state.otpCounters.isEmpty())
    }

    @Test fun noCreationStorageFailureDoesNotClaimRecoveredPendingKeys() = runBlocking {
        val store = MemoryStore()
        try { CryptAuthEnrollment(Server(store).apply { failEnrollment = true }, store).enroll(ByteString.EMPTY) }
        catch (_: IOException) { }
        val pending = store.state.pendingKeys
        store.failWrite = true
        val server = Server(store).apply {
            response = response.copy(sync_single_key_responses = pending.map {
                SyncSingleKeyResponse(key_actions = listOf(KeyAction.ACTIVATE))
            })
        }
        try { CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY); fail("Expected storage failure") }
        catch (_: IOException) { }
        assertNull(server.enrollment)
        assertTrue(store.state.keys.isEmpty())
        assertEquals(pending, store.state.pendingKeys)
    }

    @Test fun serverCanRequestNewProofForThePendingIdentity() = runBlocking {
        val store = MemoryStore()
        try { CryptAuthEnrollment(Server(store).apply { failEnrollment = true }, store).enroll(ByteString.EMPTY) }
        catch (expected: IOException) { }
        val pendingIdentity = store.state.pendingKeys.first()
        val server = Server(store).apply {
            response = response.copy(sync_single_key_responses = response.sync_single_key_responses.mapIndexed { index, single ->
                single.copy(key_actions = listOf(if (index == 0) KeyAction.ACTIVATE else KeyAction.DELETE))
            })
        }
        CryptAuthEnrollment(server, store).enroll(ByteString.EMPTY)
        assertEquals(pendingIdentity, store.state.keys.first())
        assertTrue(verifyIdentityProof(server.enrollment!!.enroll_single_key_requests.first(), server.response.random_session_id))
    }

    @Test fun corruptedStoredStateCannotReplaceValidKeys() = runBlocking {
        val store = enrolledStore()
        val encoded = CryptAuthStateCodec.encode(store.state)
        assertEquals(store.state, CryptAuthStateCodec.decode(encoded))
        assertRejected { CryptAuthStateCodec.decode(encoded + byteArrayOf(0)) }
        assertRejected { CryptAuthStateCodec.decode(encoded.copyOf(8)) }
        val identity = store.state.keys.first()
        assertRejected { CryptAuthStateCodec.encode(store.state.copy(keys = listOf(identity.copy(secret = ByteString.EMPTY)))) }
        val unrelated = CryptAuthCrypto.generateKeyPair()
        assertRejected { CryptAuthStateCodec.encode(store.state.copy(keys = listOf(identity.copy(secret = unrelated.private.encoded.toByteString())))) }
        assertEquals("CryptAuthState(redacted)", store.state.toString())
        assertEquals("CryptAuthKey(redacted)", identity.toString())
    }

    @Test fun separateStoresDoNotShareAnIdentity() = runBlocking {
        val first = enrolledStore()
        val second = enrolledStore()
        assertNotEquals(first.state.keys.first().publicKey, second.state.keys.first().publicKey)
    }

    private fun verifyIdentityProof(key: EnrollKeysRequest.EnrollSingleKeyRequest, session: ByteString): Boolean =
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(CryptAuthCrypto.parsePublicKey(key.key_material))
            update("CryptAuth Key Proof".toByteArray(Charsets.UTF_8)); update(session.toByteArray())
            verify(key.key_proof.toByteArray())
        }

    private suspend fun enrolledStore(): MemoryStore = MemoryStore().also { CryptAuthEnrollment(Server(it), it).enroll(ByteString.EMPTY) }

    private fun rotationResponse(state: CryptAuthState) = response(CryptAuthCrypto.generateKeyPair()).let { response ->
        response.copy(sync_single_key_responses = response.sync_single_key_responses.mapIndexed { index, single ->
            single.copy(key_actions = state.keys.filter { it.name == CryptAuthEnrollment.KEY_NAMES[index] }.map {
                if (index == 0) KeyAction.ACTIVATE else KeyAction.DELETE
            })
        })
    }

    private class MemoryStore : CryptAuthStateStore {
        var state = CryptAuthState("test-instance")
        var writes = 0
        var failWrite = false
        var failAfterWrites: Int? = null
        override fun read() = state
        override fun write(state: CryptAuthState) {
            if (failWrite || (failAfterWrites?.let { writes >= it } == true)) throw IOException("Storage unavailable")
            this.state = state
            writes++
        }
    }

    private class Server(private val store: MemoryStore) : CryptAuthTransport {
        val pair = CryptAuthCrypto.generateKeyPair()
        var response = response(pair)
        var request: SyncKeysRequest? = null
        var enrollment: EnrollKeysRequest? = null
        var failEnrollment = false
        var cancelEnrollment = false
        override suspend fun sync(request: SyncKeysRequest): SyncKeysResponse { this.request = request; return response }
        override suspend fun enroll(request: EnrollKeysRequest): EnrollKeysResponse {
            enrollment = request
            if (store.writes == 0) assertTrue(store.state.keys.isEmpty())
            if (failEnrollment) throw IOException("Server unavailable")
            if (cancelEnrollment) currentCoroutineContext().cancel()
            return EnrollKeysResponse()
        }
    }

    companion object {
        private fun response(pair: KeyPair) = SyncKeysResponse(
            random_session_id = "server-issued-session".encodeUtf8(),
            server_ephemeral_dh = CryptAuthCrypto.publicKey(pair.public as ECPublicKey),
            client_directive = ClientDirective(checkin_delay_millis = 3600000, retry_attempts = 1, retry_period_millis = 60000),
            sync_single_key_responses = listOf(KeyType.P256, KeyType.RAW256).map { type -> SyncSingleKeyResponse(
                key_creation = KeyCreation.ACTIVE, key_type = type,
                key_directive = KeyDirective(policy_reference = PolicyReference(name = "public-test-policy", version = 1))
            ) }
        )

        private fun assertRejected(block: () -> Unit) {
            try { block(); fail("Expected invalid input to be rejected") }
            catch (expected: Exception) { }
        }

        private suspend fun assertRejectedSuspend(block: suspend () -> Unit) {
            try { block(); fail("Expected invalid input to be rejected") }
            catch (expected: IllegalArgumentException) { }
        }
    }
}
