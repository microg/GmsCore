/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.microg.gms.cryptauth.proto.EncScheme
import org.microg.gms.cryptauth.proto.Header
import org.microg.gms.cryptauth.proto.HeaderAndBodyInternal
import org.microg.gms.cryptauth.proto.SecureMessage
import org.microg.gms.cryptauth.proto.SigScheme
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class CryptAuthBootstrapPayload(
    val body: ByteString,
    val associatedData: ByteString? = null,
    val publicMetadata: ByteString? = null
) {
    override fun toString() = "CryptAuthBootstrapPayload(redacted)"
}

internal class CryptAuthSignedEnvelope(val headerAndBody: ByteString, val secureMessage: ByteString) {
    override fun toString() = "CryptAuthSignedEnvelope(redacted)"
}

/**
 * The ECDSA-P256 / AES-256-CBC profile of Google's public SecureMessage format.
 * Algorithm: google/ukey2 SecureMessageBuilder.buildSignCryptedMessage and CryptoOps.
 * https://github.com/google/ukey2/tree/master/src/main/java/com/google/security/cryptauth/lib/securemessage
 *
 * This primitive neither authorizes callers nor transfers accounts. The transfer session must
 * obtain explicit consent and refresh enrollment before providing confirmed key state here.
 */
internal object CryptAuthBootstrapCrypto {
    private val salt = "SecureMessage".encodeUtf8().sha256()
    internal const val MAX_BODY_BYTES = 65536
    internal const val MAX_METADATA_BYTES = 4096

    fun createEnvelope(state: CryptAuthState, payload: CryptAuthBootstrapPayload): CryptAuthSignedEnvelope {
        require(state.pendingKeys.isEmpty()) { "CryptAuth enrollment is unconfirmed" }
        val identity = requireNotNull(state.keys.singleOrNull { it.name == "PublicKey" && it.active }) { "Missing active CryptAuth identity" }
        val authzen = requireNotNull(state.keys.singleOrNull { it.name == "authzen" && it.active }) { "Missing active CryptAuth authzen key" }
        validatePayload(payload)
        validateKeys(identity, authzen)
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }.toByteString()
        val header = Header(
            signature_scheme = SigScheme.ECDSA_P256_SHA256,
            encryption_scheme = EncScheme.AES_256_CBC,
            verification_key_id = identity.publicKey,
            iv = iv,
            public_metadata = payload.publicMetadata,
            associated_data_length = payload.associatedData?.size
        )
        return signCrypt(identity, authzen, payload, header)
    }

    /** Lower-level SecureMessage operation, also checked against Google's fixed public vector. */
    internal fun signCrypt(identity: CryptAuthKey, authzen: CryptAuthKey,
                           payload: CryptAuthBootstrapPayload, header: Header): CryptAuthSignedEnvelope {
        validateKeys(identity, authzen)
        validatePayload(payload)
        require(header.signature_scheme == SigScheme.ECDSA_P256_SHA256 && header.encryption_scheme == EncScheme.AES_256_CBC) {
            "Unsupported bootstrap message algorithms"
        }
        require(header.unknownFields.size == 0 && header.iv?.size == 16 && (header.verification_key_id?.size ?: 0) in 1..128 &&
            (header.decryption_key_id?.size ?: 0) <= 128) { "Invalid bootstrap message header" }
        require(header.public_metadata == payload.publicMetadata && header.associated_data_length == payload.associatedData?.size) {
            "Bootstrap header does not match payload"
        }
        val publicKey = CryptAuthCrypto.parsePublicKey(identity.publicKey)
        val encodedPrivate = identity.secret.toByteArray()
        val privateKey = try { KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(encodedPrivate)) }
            finally { encodedPrivate.fill(0) }
        val encodedHeader = header.encodeByteString()
        // Public-key signcryption binds the header and out-of-band data inside the encrypted body.
        val tag = MessageDigest.getInstance("SHA-256").run {
            update(encodedHeader.toByteArray())
            payload.associatedData?.let { update(it.toByteArray()) }
            digest().copyOf(20)
        }
        val plaintext = tag + payload.body.toByteArray()
        val masterKey = authzen.secret.toByteArray()
        val derivedKey = try { CryptAuthCrypto.hkdf(masterKey, salt.toByteArray(), "ENC:2".toByteArray(Charsets.UTF_8), 32) }
            finally { masterKey.fill(0) }
        try {
            val encryptedBody = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedKey, "AES"), IvParameterSpec(requireNotNull(header.iv).toByteArray()))
                doFinal(plaintext).toByteString()
            }
            val headerAndBody = HeaderAndBodyInternal(header_ = encodedHeader, body = encryptedBody).encodeByteString()
            val signature = Signature.getInstance("SHA256withECDSA").run {
                initSign(privateKey)
                update(salt.toByteArray())
                update(headerAndBody.toByteArray())
                sign().toByteString()
            }
            // Reject a private/public key mismatch rather than producing an unusable assertion.
            require(Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey); update(salt.toByteArray()); update(headerAndBody.toByteArray()); verify(signature.toByteArray())
            }) { "CryptAuth identity key mismatch" }
            val message = SecureMessage(header_and_body = headerAndBody, signature = signature).encodeByteString()
            return CryptAuthSignedEnvelope(headerAndBody, message)
        } finally { derivedKey.fill(0); plaintext.fill(0); tag.fill(0) }
    }

    private fun validateKeys(identity: CryptAuthKey, authzen: CryptAuthKey) {
        require(identity.name == "PublicKey" && identity.type == KeyType.P256 && identity.active &&
            identity.handle == CryptAuthEnrollment.DEVICE_KEY_HANDLE && identity.secret.size in 1..512) { "Invalid bootstrap identity" }
        require(authzen.name == "authzen" && authzen.type == KeyType.RAW256 && authzen.active && authzen.secret.size == 32 &&
            authzen.publicKey.size == 0 && authzen.handle == authzen.secret.sha256().base64Url().encodeUtf8()) { "Invalid bootstrap encryption key" }
        CryptAuthCrypto.parsePublicKey(identity.publicKey)
    }

    private fun validatePayload(payload: CryptAuthBootstrapPayload) {
        require(payload.body.size in 1..MAX_BODY_BYTES && (payload.associatedData?.size ?: 0) <= MAX_METADATA_BYTES &&
            (payload.publicMetadata?.size ?: 0) <= MAX_METADATA_BYTES) { "Invalid bootstrap payload length" }
    }
}
