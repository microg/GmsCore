/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.utils

import android.util.Log
import org.microg.gms.fido.core.hybrid.AEMK_ALGORITHM
import org.microg.gms.fido.core.hybrid.EC_ALGORITHM
import org.microg.gms.fido.core.hybrid.HKDF_ALGORITHM
import org.microg.gms.fido.core.hybrid.hex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECPoint
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

object CryptoHelper {

    private const val TAG = "CryptoHelper"

    fun uncompress(pub: ECPublicKey): ByteArray {
        val p = pub.w
        val x = p.affineX.toByteArray()
        val y = p.affineY.toByteArray()
        return ByteArray(65).apply {
            this[0] = 0x04
            System.arraycopy(x, (x.size - 32).coerceAtLeast(0), this, 1 + (32 - x.size).coerceAtLeast(0), x.size.coerceAtMost(32))
            System.arraycopy(y, (y.size - 32).coerceAtLeast(0), this, 33 + (32 - y.size).coerceAtLeast(0), y.size.coerceAtMost(32))
        }
    }

    fun recd(privy: ECPrivateKey, peerUncompressedOrDer: ByteArray): ByteArray {
        val pub = try {
            if (peerUncompressedOrDer.size == 65 && peerUncompressedOrDer[0] == 0x04.toByte()) {
                val x = peerUncompressedOrDer.sliceArray(1..32)
                val y = peerUncompressedOrDer.sliceArray(33..64)
                val kg = KeyPairGenerator.getInstance(EC_ALGORITHM).apply { initialize(ECGenParameterSpec("secp256r1")) }
                val tmp = (kg.generateKeyPair().public as ECPublicKey).params
                val spec = java.security.spec.ECPublicKeySpec(
                    ECPoint(java.math.BigInteger(1, x), java.math.BigInteger(1, y)), tmp
                )
                java.security.KeyFactory.getInstance(EC_ALGORITHM).generatePublic(spec) as ECPublicKey
            } else {
                java.security.KeyFactory.getInstance(EC_ALGORITHM).generatePublic(
                    java.security.spec.X509EncodedKeySpec(peerUncompressedOrDer)
                ) as ECPublicKey
            }
        } catch (_: Throwable) {
            val derPrefix = byteArrayOf(
                0x30, 89, 0x30, 19, 0x06, 7, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x02, 0x01, 0x06, 8, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01, 0x07, 0x03, 66, 0
            )
            val full = derPrefix + peerUncompressedOrDer
            java.security.KeyFactory.getInstance(EC_ALGORITHM).generatePublic(
                java.security.spec.X509EncodedKeySpec(full)
            ) as ECPublicKey
        }

        val ka = javax.crypto.KeyAgreement.getInstance("ECDH")
        ka.init(privy)
        ka.doPhase(pub, true)
        return ka.generateSecret()
    }

    fun endif(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = endifExtract(salt, ikm)
        return endifExpand(prk, info, length)
    }

    fun endifExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val s = if (salt.isEmpty()) ByteArray(32) else salt
        val mac = Mac.getInstance(HKDF_ALGORITHM).apply { init(SecretKeySpec(s, HKDF_ALGORITHM)) }
        return mac.doFinal(ikm)
    }

    private fun endifExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val glen = 32
        val rounds = ceil(length / glen.toDouble()).toInt()
        require(rounds <= 255) { "hkdf expand too long" }
        val mac = Mac.getInstance(HKDF_ALGORITHM).apply { init(SecretKeySpec(prk, HKDF_ALGORITHM)) }

        val out = ByteArray(length)
        var prev = ByteArray(0)
        repeat(rounds) { i ->
            mac.reset()
            if (prev.isNotEmpty()) mac.update(prev)
            mac.update(info)
            mac.update((i + 1).toByte())
            prev = mac.doFinal()
            val copy = minOf(glen, length - i * glen)
            System.arraycopy(prev, 0, out, i * glen, copy)
        }
        return out
    }

    fun decryptEid(eid: ByteArray, seed: ByteArray): ByteArray? {
        Log.d(TAG, "decryptEid: eid=${eid.hex()}, seed=${seed.hex()}")
        if (eid.size != 20) {
            Log.e(TAG, "decryptEid: Invalid EID size: ${eid.size}")
            return null
        }
        val info = byteArrayOf(1, 0, 0, 0)
        val derived = endif(ikm = seed, salt = ByteArray(0), info = info, length = 64)
        val aesKey = derived.copyOfRange(0, 32)
        val hmacKey = derived.copyOfRange(32, 64)
        Log.d(TAG, "decryptEid: aesKey=${aesKey.hex()}, hmacKey=${hmacKey.hex()}")
        val ct = eid.copyOfRange(0, 16)
        val tag4 = eid.copyOfRange(16, 20)
        Log.d(TAG, "decryptEid: ct=${ct.hex()}, tag4=${tag4.hex()}")
        val mac = Mac.getInstance(HKDF_ALGORITHM).apply { init(SecretKeySpec(hmacKey, HKDF_ALGORITHM)) }
        val expect = mac.doFinal(ct).copyOf(4)
        Log.d(TAG, "decryptEid: expected tag=${expect.hex()}, actual tag=${tag4.hex()}")
        if (!MessageDigest.isEqual(expect, tag4)) {
            Log.w(TAG, "decryptEid: HMAC verification failed!")
            return null
        }
        val cipher = Cipher.getInstance("AES/CBC/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, AEMK_ALGORITHM), IvParameterSpec(ByteArray(16)))
        }
        val pt = cipher.doFinal(ct)
        Log.d(TAG, "decryptEid: decrypted pt=${pt.hex()}, first byte=${pt.first()}")
        if (pt.first() != 0.toByte()) {
            Log.w(TAG, "decryptEid: Invalid first byte!")
            return null
        }
        Log.d(TAG, "decryptEid: SUCCESS!")
        return pt
    }

    fun generateEid(eidKey: ByteArray, seed: ByteArray): ByteArray {
        val aesKey = eidKey.copyOfRange(0, 32)
        val hmacKey = eidKey.copyOfRange(32, 64)

        val ciphertext = try {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(ByteArray(16))
            )
            cipher.doFinal(seed)
        } catch (e: Exception) {
            Log.e(TAG, "AES encryption failed", e)
            ByteArray(16)
        }

        val mac = try {
            val hmac = Mac.getInstance("HmacSHA256")
            hmac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
            hmac.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.e(TAG, "HMAC calculation failed", e)
            ByteArray(32)
        }
        val tag = mac.copyOf(4)

        val eid = ByteArray(20)
        System.arraycopy(ciphertext, 0, eid, 0, 16)
        System.arraycopy(tag, 0, eid, 16, 4)
        return eid
    }

    fun generatedSeed(routingId: ByteArray): ByteArray {
        val seed = ByteArray(16).apply {
            this[0] = 0x00
            val timestamp = System.currentTimeMillis()
            val buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            buffer.putLong(timestamp)
            System.arraycopy(buffer.array(), 0, this, 1, 8)
            System.arraycopy(routingId, 0, this, 11, 3)
            this[14] = 0x00
            this[15] = 0x00
        }
        return seed.copyOf()
    }
}