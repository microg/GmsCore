/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom.utils

import com.google.crypto.tink.subtle.EllipticCurves
import com.google.crypto.tink.subtle.Hkdf
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecureBox {
    private val VERSION = byteArrayOf(0x02, 0x00)
    private val HKDF_SALT = "SECUREBOX".toByteArray() + byteArrayOf(0x02, 0x00)
    private val HKDF_INFO_P256 = "P256 HKDF-SHA-256 AES-128-GCM".toByteArray()
    private val HKDF_INFO_SHARED = "SHARED HKDF-SHA-256 AES-128-GCM".toByteArray()
    private const val AES_KEY_SIZE = 16
    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 128
    private const val KEY_PAIR_SIZE = 97
    private const val PUBLIC_KEY_SIZE = 65
    private const val UNCOMPRESSED_POINT_PREFIX: Byte = 0x04

    fun deserializePrivateKey(keyBytes: ByteArray): PrivateKey {
        if (keyBytes.size != KEY_PAIR_SIZE) {
            throw InvalidKeyException("Invalid key pair size: expected $KEY_PAIR_SIZE bytes, got ${keyBytes.size}")
        }
        val privateKeyBytes = keyBytes.copyOf(AES_KEY_SIZE * 2)
        return EllipticCurves.getEcPrivateKey(EllipticCurves.CurveType.NIST_P256, privateKeyBytes)
    }

    fun deserializePublicKey(keyBytes: ByteArray): PublicKey {
        if (keyBytes.size != PUBLIC_KEY_SIZE || keyBytes[0] != UNCOMPRESSED_POINT_PREFIX) {
            throw InvalidKeyException("Invalid public key: expected $PUBLIC_KEY_SIZE bytes starting with 0x04")
        }
        return EllipticCurves.getEcPublicKey(
            EllipticCurves.CurveType.NIST_P256,
            EllipticCurves.PointFormatType.UNCOMPRESSED,
            keyBytes
        )
    }

    private fun performECDH(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        return EllipticCurves.computeSharedSecret(
            privateKey as ECPrivateKey,
            (publicKey as ECPublicKey).w
        )
    }

    private fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLength: Int = AES_KEY_SIZE): ByteArray {
        return Hkdf.computeHkdf("HMACSHA256", ikm, salt, info, outputLength)
    }

    fun decrypt(
        privateKey: PrivateKey?,
        sharedSecret: ByteArray?,
        header: ByteArray?,
        encryptedPayload: ByteArray
    ): ByteArray {
        val secret = sharedSecret ?: ByteArray(0)
        val aad = header ?: ByteArray(0)

        require(privateKey != null || secret.isNotEmpty()) {
            "Both private key and shared secret are empty"
        }

        val buffer = ByteBuffer.wrap(encryptedPayload)

        val version = ByteArray(VERSION.size)
        buffer.get(version)
        require(version.contentEquals(VERSION)) {
            "Invalid SecureBox version: expected ${VERSION.joinToString(",")}, got ${version.joinToString(",")}"
        }

        val ecdhSecret: ByteArray
        val hkdfInfo: ByteArray

        if (privateKey == null) {
            ecdhSecret = ByteArray(0)
            hkdfInfo = HKDF_INFO_SHARED
        } else {
            val ephemeralPublicKeyBytes = ByteArray(PUBLIC_KEY_SIZE)
            buffer.get(ephemeralPublicKeyBytes)
            val ephemeralPublicKey = deserializePublicKey(ephemeralPublicKeyBytes)

            ecdhSecret = performECDH(privateKey, ephemeralPublicKey)
            hkdfInfo = HKDF_INFO_P256
        }

        val iv = ByteArray(GCM_IV_SIZE)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val combinedSecret = concat(ecdhSecret, secret)
        val aesKeyBytes = deriveKey(combinedSecret, HKDF_SALT, hkdfInfo)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        return aesGcmDecrypt(aesKey, iv, ciphertext, aad)
    }

    private fun aesGcmEncrypt(key: SecretKeySpec, iv: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext)
    }

    private fun aesGcmDecrypt(key: SecretKeySpec, iv: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    private fun concat(vararg arrays: ByteArray): ByteArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        return result
    }

    fun unwrapKey(localKeyPairBytes: ByteArray, wrappedKey: ByteArray, header: ByteArray? = null): ByteArray {
        val privateKey = deserializePrivateKey(localKeyPairBytes)
        return decrypt(privateKey, null, header, wrappedKey)
    }
}