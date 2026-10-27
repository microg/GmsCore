/* SPDX-FileCopyrightText: 2020 Google LLC (public test vector)
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.junit.Assert.*
import org.junit.Test
import org.microg.gms.cryptauth.proto.Header
import org.microg.gms.cryptauth.proto.HeaderAndBodyInternal
import org.microg.gms.cryptauth.proto.SecureMessage
import java.security.Signature

class CryptAuthBootstrapCryptoTest {
    // Fixed fixture from Google's Apache-2.0 SecureMessageSimpleTestVectorTest.java.
    // https://github.com/google/ukey2/blob/master/src/main/javatest/com/google/security/cryptauth/lib/securemessage/SecureMessageSimpleTestVectorTest.java
    private val fixturePublic = ("080112460a210093090508a7fdbcaaed1182f5a1236566c7ac8a495342c2cfa547ed347b71772d" +
        "122100bfed53bef43e66bd74402a37ac9b5a9671a7e23990609d820e53295fe88e17fb").decodeHex()
    private val fixturePrivate = ("3041020100301306072a8648ce3d020106082a8648ce3d030107042730250201010420" +
        "1aaec3aac5f802c2efec7a03559ab4513327f70c638b7f13796de1cf6e794c95").decodeHex()
    private val fixtureSecret = "a7693ed7b54e466ec2c6b0af9dc2272625f990ad51177db89c67dee9bc15d298".decodeHex()
    private val fixture = ("0a6b0a37080210021a030000012203ffff002a10aa1037f8abd1b3dc812cf6d4c173911a" +
        "32130a0b0c0d0e0f101112131415161718191a1b1c380512309217bd7a8a60fc208f9895f04c25c3bdc15a2660d19738de32e2521964244532" +
        "443c266094cfb7f6c2b4d397aa5d1c2212463044022100a9990bba2221d75aadb613f37fd58ce058f37d8638eb4f2f6559b0d5665c04f1" +
        "021f6dbb23152ce5b32011a6bc7137e88628513300ace3f4e64969e074e4548c8b").decodeHex()
    private val payload = CryptAuthBootstrapPayload("0063016202610360045f055e065d075c085b095a".decodeHex(),
        "0b16212c37".decodeHex(), "0a0b0c0d0e0f101112131415161718191a1b1c".decodeHex())
    private val identity = CryptAuthKey("PublicKey", CryptAuthEnrollment.DEVICE_KEY_HANDLE, KeyType.P256,
        fixturePublic, fixturePrivate, true)
    private val authzen = CryptAuthKey("authzen", fixtureSecret.sha256().base64Url().encodeUtf8(), KeyType.RAW256,
        secret = fixtureSecret, active = true)
    private val state = CryptAuthState("public-fixture", keys = listOf(identity, authzen))

    @Test fun encryptionMatchesGoogleFixedVectorExactly() {
        val known = SecureMessage.ADAPTER.decode(fixture)
        val header = Header.ADAPTER.decode(HeaderAndBodyInternal.ADAPTER.decode(known.header_and_body).header_)
        // This validates ciphertext and tag against independent published bytes, not our own decryptor.
        val actual = CryptAuthBootstrapCrypto.signCrypt(identity, authzen, payload, header)
        assertEquals(known.header_and_body, actual.headerAndBody)
        assertTrue(verify(known))
        assertTrue(verify(SecureMessage.ADAPTER.decode(actual.secureMessage)))
        // The enrollment proof domain is deliberately different from SecureMessage.
        assertFalse(verify(SecureMessage.ADAPTER.decode(actual.secureMessage), "CryptAuth Key Proof".encodeUtf8()))
    }

    @Test fun bootstrapHeaderUsesTheConfirmedPublicKeyAndFreshIv() {
        val first = CryptAuthBootstrapCrypto.createEnvelope(state, payload)
        val second = CryptAuthBootstrapCrypto.createEnvelope(state, payload)
        val firstHeader = header(first)
        val secondHeader = header(second)
        assertEquals(fixturePublic, firstHeader.verification_key_id)
        assertEquals(payload.publicMetadata, firstHeader.public_metadata)
        assertEquals(payload.associatedData!!.size, firstHeader.associated_data_length)
        assertNull(firstHeader.decryption_key_id)
        assertEquals(16, firstHeader.iv!!.size)
        assertNotEquals(firstHeader.iv, secondHeader.iv)
        assertTrue(verify(SecureMessage.ADAPTER.decode(first.secureMessage)))
        assertEquals(first.headerAndBody, SecureMessage.ADAPTER.decode(first.secureMessage).header_and_body)
    }

    @Test fun nullAndEmptyAssociatedDataPreserveWirePresence() {
        val absent = CryptAuthBootstrapCrypto.createEnvelope(state, CryptAuthBootstrapPayload(payload.body))
        val empty = CryptAuthBootstrapCrypto.createEnvelope(state, CryptAuthBootstrapPayload(payload.body, ByteString.EMPTY))
        assertNull(header(absent).associated_data_length)
        assertEquals(0, header(empty).associated_data_length)
    }

    @Test fun pendingOrInactiveOrMissingKeysCannotSign() {
        for (invalid in listOf(
            state.copy(pendingKeys = listOf(identity)),
            state.copy(keys = listOf(identity.copy(active = false), authzen)),
            state.copy(keys = listOf(identity, authzen.copy(active = false))),
            state.copy(keys = listOf(identity)),
            state.copy(keys = listOf(authzen)),
            state.copy(keys = listOf(identity, identity, authzen))
        )) assertRejected { CryptAuthBootstrapCrypto.createEnvelope(invalid, payload) }
    }

    @Test fun rejectsUnsupportedKeyTypeAndMismatchedIdentity() {
        val otherPrivate = CryptAuthCrypto.generateKeyPair().private.encoded.toByteString()
        for (invalid in listOf(
            state.copy(keys = listOf(identity, authzen.copy(type = KeyType.RAW128))),
            state.copy(keys = listOf(identity, authzen.copy(secret = ByteString.EMPTY))),
            state.copy(keys = listOf(identity.copy(secret = otherPrivate), authzen)),
            state.copy(keys = listOf(identity.copy(publicKey = ByteString.EMPTY), authzen)),
            state.copy(keys = listOf(identity.copy(type = KeyType.CURVE25519), authzen))
        )) assertRejected { CryptAuthBootstrapCrypto.createEnvelope(invalid, payload) }
    }

    @Test fun rejectsOversizedOrEmptyPayloadAndInconsistentHeaders() {
        assertRejected { CryptAuthBootstrapCrypto.createEnvelope(state, CryptAuthBootstrapPayload(ByteString.EMPTY)) }
        assertRejected { CryptAuthBootstrapCrypto.createEnvelope(state,
            CryptAuthBootstrapPayload(ByteArray(CryptAuthBootstrapCrypto.MAX_BODY_BYTES + 1).toByteString())) }
        assertRejected { CryptAuthBootstrapCrypto.createEnvelope(state,
            CryptAuthBootstrapPayload(payload.body, ByteArray(CryptAuthBootstrapCrypto.MAX_METADATA_BYTES + 1).toByteString())) }
        val fixedHeader = Header.ADAPTER.decode(HeaderAndBodyInternal.ADAPTER.decode(SecureMessage.ADAPTER.decode(fixture).header_and_body).header_)
        assertRejected { CryptAuthBootstrapCrypto.signCrypt(identity, authzen, payload, fixedHeader.copy(associated_data_length = 0)) }
        assertRejected { CryptAuthBootstrapCrypto.signCrypt(identity, authzen, payload, fixedHeader.copy(iv = ByteString.EMPTY)) }
        assertRejected { CryptAuthBootstrapCrypto.signCrypt(identity, authzen, payload, fixedHeader.copy(public_metadata = ByteString.EMPTY)) }
    }

    @Test fun changingAssociatedDataChangesTheAuthenticatedCiphertext() {
        val fixedHeader = Header.ADAPTER.decode(HeaderAndBodyInternal.ADAPTER.decode(SecureMessage.ADAPTER.decode(fixture).header_and_body).header_)
        val changed = CryptAuthBootstrapPayload(payload.body, "0102030405".decodeHex(), payload.publicMetadata)
        assertNotEquals(SecureMessage.ADAPTER.decode(fixture).header_and_body,
            CryptAuthBootstrapCrypto.signCrypt(identity, authzen, changed, fixedHeader).headerAndBody)
    }

    @Test fun diagnosticStringsDoNotExposePayloads() {
        assertEquals("CryptAuthBootstrapPayload(redacted)", payload.toString())
        assertEquals("CryptAuthSignedEnvelope(redacted)", CryptAuthBootstrapCrypto.createEnvelope(state, payload).toString())
    }

    private fun header(envelope: CryptAuthSignedEnvelope) = Header.ADAPTER.decode(HeaderAndBodyInternal.ADAPTER.decode(envelope.headerAndBody).header_)

    private fun verify(message: SecureMessage, domain: ByteString = "SecureMessage".encodeUtf8().sha256()): Boolean =
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(CryptAuthCrypto.parsePublicKey(fixturePublic))
            update(domain.toByteArray()); update(message.header_and_body.toByteArray())
            verify(message.signature.toByteArray())
        }

    private fun assertRejected(block: () -> Unit) {
        try { block(); fail("Expected invalid bootstrap input to be rejected") }
        catch (expected: Exception) { }
    }
}
