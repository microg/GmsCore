/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.cryptauth

import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.microg.gms.utils.hkdfSha256

/** Cryptographic operations for supported CryptAuth key types. */
object CryptAuthKeyOperations {
    /** Generates an exportable key pair of [keyType]. */
    fun generateKeyPair(keyType: CryptAuthKeyType): KeyPair {
        requireP256(keyType)
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM_EC)
        generator.initialize(ECGenParameterSpec(CURVE_P256))
        return generator.generateKeyPair()
    }

    /** Restores a key pair of [keyType] from its encoded public and private keys. */
    fun decodeKeyPair(keyType: CryptAuthKeyType, publicKey: ByteArray, privateKey: ByteArray): KeyPair {
        requireP256(keyType)
        require(publicKey.isNotEmpty()) { "CryptAuth public key must not be empty" }
        require(privateKey.isNotEmpty()) { "CryptAuth private key must not be empty" }
        val factory = KeyFactory.getInstance(KEY_ALGORITHM_EC)
        return KeyPair(
            factory.generatePublic(X509EncodedKeySpec(publicKey)),
            factory.generatePrivate(PKCS8EncodedKeySpec(privateKey)),
        )
    }

    /** Signs [value] using the signature scheme for [keyType]. */
    fun sign(keyType: CryptAuthKeyType, privateKey: PrivateKey, value: ByteArray): ByteArray {
        requireP256(keyType)
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM_P256)
        signature.initSign(privateKey)
        signature.update(value)
        return signature.sign()
    }

    /** Creates the enrollment proof for a key of [keyType]. */
    fun createKeyProof(
        keyType: CryptAuthKeyType,
        privateKey: PrivateKey,
        randomSessionId: ByteArray,
    ): ByteArray {
        requireP256(keyType)
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM_P256)
        signature.initSign(privateKey)
        signature.update(KEY_PROOF_PREFIX)
        signature.update(randomSessionId)
        return signature.sign()
    }

    /** Derives symmetric CryptAuth key material of [keyType] from an ECDH shared secret. */
    fun deriveSymmetricKey(
        keyType: CryptAuthKeyType,
        rawSharedSecret: ByteArray,
        keyName: String,
    ): ByteArray = hkdfSha256(
        MessageDigest.getInstance(DIGEST_ALGORITHM_SHA256).digest(rawSharedSecret),
        ENROLLMENT_SALT,
        keyName.toByteArray(StandardCharsets.UTF_8),
        requireSymmetricKeyLength(keyType),
    )

    /** Creates the enrollment proof for symmetric key material of [keyType]. */
    fun createKeyProof(
        keyType: CryptAuthKeyType,
        keyMaterial: ByteArray,
        keyName: String,
        randomSessionId: ByteArray,
    ): ByteArray {
        val keyLength = requireSymmetricKeyLength(keyType)
        require(keyMaterial.size == keyLength) { "Invalid symmetric CryptAuth key length" }
        val proofKey = hkdfSha256(
            keyMaterial,
            KEY_PROOF_PREFIX,
            keyName.toByteArray(StandardCharsets.UTF_8),
            keyLength,
        )
        return Mac.getInstance(HMAC_ALGORITHM_SHA256).run {
            init(SecretKeySpec(proofKey, HMAC_ALGORITHM_SHA256))
            doFinal(randomSessionId)
        }
    }

    private fun requireP256(keyType: CryptAuthKeyType) {
        require(keyType == CryptAuthKeyType.P256) { "Unsupported asymmetric CryptAuth key type: $keyType" }
    }

    private fun requireSymmetricKeyLength(keyType: CryptAuthKeyType): Int =
        requireNotNull(keyType.symmetricKeyLength) { "Unsupported symmetric CryptAuth key type: $keyType" }

    private const val KEY_ALGORITHM_EC = "EC"
    private const val CURVE_P256 = "secp256r1"
    private const val SIGNATURE_ALGORITHM_P256 = "SHA256withECDSA"
    private const val DIGEST_ALGORITHM_SHA256 = "SHA-256"
    private const val HMAC_ALGORITHM_SHA256 = "HmacSHA256"
    private val ENROLLMENT_SALT = "CryptAuth Enrollment".toByteArray(StandardCharsets.UTF_8)
    private val KEY_PROOF_PREFIX = "CryptAuth Key Proof".toByteArray(StandardCharsets.UTF_8)
}
