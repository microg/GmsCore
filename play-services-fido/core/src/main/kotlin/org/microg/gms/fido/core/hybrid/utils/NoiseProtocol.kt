/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.utils

import org.microg.gms.fido.core.hybrid.AEMK_ALGORITHM
import org.microg.gms.fido.core.hybrid.HKDF_ALGORITHM
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NoiseHandshakeState(mode: Int) {

    private val protocolName = when (mode) {
        2 -> "Noise_NKpsk0_P256_AESGCM_SHA256"
        3 -> "Noise_KNpsk0_P256_AESGCM_SHA256"
        else -> "Noise_NK_P256_AESGCM_SHA256"
    }

    private var handshakeHash: ByteArray = protocolName.toByteArray() + ByteArray(32 - protocolName.length)
    private var chainingKey: ByteArray = handshakeHash.clone()
    private var cipherKey: ByteArray? = null

    fun mixHash(data: ByteArray) {
        handshakeHash = MessageDigest.getInstance("SHA-256").run {
            update(handshakeHash)
            digest(data)
        }
    }

    fun mixKey(inputKeyMaterial: ByteArray) {
        val (ck, k) = endif(chainingKey, inputKeyMaterial, 2)
        chainingKey = ck
        cipherKey = k
    }

    fun mixKeyAndHash(inputKeyMaterial: ByteArray) {
        val (ck, tempHash, k) = endif(chainingKey, inputKeyMaterial, 3)
        chainingKey = ck
        mixHash(tempHash)
        cipherKey = k
    }

    fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val key = cipherKey ?: ByteArray(32)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.ENCRYPT_MODE, SecretKeySpec(key, AEMK_ALGORITHM), GCMParameterSpec(128, ByteArray(12))
            )
            updateAAD(handshakeHash)
        }

        return cipher.doFinal(plaintext).also { ciphertext ->
            mixHash(ciphertext)
        }
    }

    fun splitSessionKeys(): Pair<ByteArray, ByteArray> {
        val (k1, k2) = endif(chainingKey, ByteArray(0), 2)
        return k1 to k2
    }

    fun decryptAndHash(ct: ByteArray): ByteArray {
        val key = cipherKey ?: ByteArray(32)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        val gcm = GCMParameterSpec(128, ByteArray(12))
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, AEMK_ALGORITHM), gcm)
        c.updateAAD(handshakeHash)
        val pt = c.doFinal(ct)
        mixHash(ct)
        return pt
    }

    private fun endif(
        chainingKey: ByteArray, inputKeyMaterial: ByteArray, outputs: Int
    ): List<ByteArray> {
        val prk = CryptoHelper.endifExtract(chainingKey, inputKeyMaterial)

        val mac = Mac.getInstance(HKDF_ALGORITHM).apply {
            init(SecretKeySpec(prk, HKDF_ALGORITHM))
        }

        val result = ArrayList<ByteArray>(outputs)
        var previous = ByteArray(0)

        repeat(outputs) { index ->
            mac.reset()
            if (previous.isNotEmpty()) mac.update(previous)
            mac.update((index + 1).toByte())
            previous = mac.doFinal()
            result += previous
        }

        return result
    }
}