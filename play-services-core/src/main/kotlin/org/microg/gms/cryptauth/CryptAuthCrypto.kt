/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import cryptauthv2.KeyType
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.cryptauth.proto.EcP256PublicKey
import org.microg.gms.cryptauth.proto.GenericPublicKey
import org.microg.gms.cryptauth.proto.PublicKeyType
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * CryptAuth v2's public algorithms, not an account-transfer protocol implementation.
 * Protocol references:
 * https://chromium.googlesource.com/chromium/src/+/main/chromeos/ash/services/device_sync/cryptauth_key_creator_impl.cc
 * https://chromium.googlesource.com/chromium/src/+/main/chromeos/ash/services/device_sync/cryptauth_key_proof_computer_impl.cc
 * P-256 wire encoding and hashed ECDH are specified by PublicKeyProtoUtil and
 * EnrollmentCryptoOps in https://github.com/google/ukey2/tree/master/src/main/java/com/google/security/cryptauth/lib.
 */
internal object CryptAuthCrypto {
    private val curve: ECParameterSpec get() = AlgorithmParameters.getInstance("EC").apply {
        init(ECGenParameterSpec("secp256r1"))
    }.getParameterSpec(ECParameterSpec::class.java)

    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    fun publicKey(key: ECPublicKey): ByteString = GenericPublicKey(
        type = PublicKeyType.EC_P256,
        ec_p256_public_key = EcP256PublicKey(
            x = key.w.affineX.toByteArray().toByteString(),
            y = key.w.affineY.toByteArray().toByteString()
        )
    ).encodeByteString()

    fun parsePublicKey(encoded: ByteString): ECPublicKey {
        require(encoded.size <= 128) { "Invalid CryptAuth public key length" }
        val message = GenericPublicKey.ADAPTER.decode(encoded)
        require(message.type == PublicKeyType.EC_P256 && message.unknownFields.size == 0) { "Unsupported CryptAuth public key" }
        val point = requireNotNull(message.ec_p256_public_key) { "Missing CryptAuth curve point" }
        require(point.unknownFields.size == 0 && point.x.size in 1..33 && point.y.size in 1..33) { "Invalid CryptAuth coordinates" }
        val x = BigInteger(point.x.toByteArray())
        val y = BigInteger(point.y.toByteArray())
        val parameters = curve
        val prime = (parameters.curve.field as java.security.spec.ECFieldFp).p
        require(x.signum() >= 0 && x < prime && y.signum() >= 0 && y < prime) { "CryptAuth coordinate out of range" }
        require(y.multiply(y).mod(prime) == x.pow(3).add(parameters.curve.a.multiply(x)).add(parameters.curve.b).mod(prime)) {
            "CryptAuth point is not on P-256"
        }
        return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(ECPoint(x, y), parameters)) as ECPublicKey
    }

    fun agreement(pair: KeyPair, peer: ByteString): ByteArray {
        val raw = KeyAgreement.getInstance("ECDH").apply {
            init(pair.private)
            doPhase(parsePublicKey(peer), true)
        }.generateSecret()
        return try { MessageDigest.getInstance("SHA-256").digest(raw) } finally { raw.fill(0) }
    }

    fun derive(secret: ByteArray, name: String, size: Int): ByteString =
        hkdf(secret, "CryptAuth Enrollment".toByteArray(Charsets.UTF_8), name.toByteArray(Charsets.UTF_8), size).toByteString()

    fun proof(key: CryptAuthKey, session: ByteString): ByteString {
        require(session.size in 1..4096) { "Invalid CryptAuth session" }
        return when (key.type) {
            KeyType.P256 -> Signature.getInstance("SHA256withECDSA").run {
                initSign(KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(key.secret.toByteArray())))
                update("CryptAuth Key Proof".toByteArray(Charsets.UTF_8))
                update(session.toByteArray())
                sign().toByteString()
            }
            KeyType.RAW128, KeyType.RAW256 -> {
                val derived = hkdf(key.secret.toByteArray(), "CryptAuth Key Proof".toByteArray(Charsets.UTF_8),
                    key.name.toByteArray(Charsets.UTF_8), key.secret.size)
                try { hmac(derived, session.toByteArray()).toByteString() } finally { derived.fill(0) }
            }
            else -> error("Unsupported CryptAuth key type")
        }
    }

    // RFC 5869, HKDF-SHA-256. Enrollment requests only 16 or 32 bytes.
    internal fun hkdf(input: ByteArray, salt: ByteArray, info: ByteArray, size: Int): ByteArray {
        require(size in 1..(255 * 32))
        val prk = hmac(if (salt.isEmpty()) ByteArray(32) else salt, input)
        var previous = ByteArray(0)
        val output = ByteArray(size)
        try {
            var position = 0
            var counter = 1
            while (position < size) {
                val next = hmac(prk, previous + info + byteArrayOf(counter++.toByte()))
                previous.fill(0)
                previous = next
                val length = minOf(next.size, size - position)
                next.copyInto(output, position, 0, length)
                position += length
            }
            return output
        } finally { prk.fill(0); previous.fill(0) }
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }
}
