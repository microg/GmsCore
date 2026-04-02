/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.utils

import org.microg.gms.fido.core.hybrid.AEMK_ALGORITHM
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NoiseCrypter(private val rKey: ByteArray, private val wKey: ByteArray) {
    private var rCtr = 0
    private var wCtr = 0

    fun encrypt(plain: ByteArray): ByteArray? = try {
        val padded = pad32(plain)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(
            Cipher.ENCRYPT_MODE, SecretKeySpec(wKey, AEMK_ALGORITHM), GCMParameterSpec(128, nonce(wCtr++))
        )
        c.doFinal(padded)
    } catch (_: Throwable) {
        null
    }

    fun decrypt(cipher: ByteArray): ByteArray? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(
            Cipher.DECRYPT_MODE, SecretKeySpec(rKey, AEMK_ALGORITHM), GCMParameterSpec(128, nonce(rCtr++))
        )
        val padded = c.doFinal(cipher)
        unpad32(padded)
    } catch (_: Throwable) {
        null
    }

    private fun pad32(src: ByteArray): ByteArray {
        val block = 32
        val rem = src.size % block
        val pad = if (rem == 0) block else (block - rem)
        val out = ByteArray(src.size + pad)
        System.arraycopy(src, 0, out, 0, src.size)
        out[out.lastIndex] = (pad - 1).toByte()
        return out
    }

    private fun nonce(c: Int) = byteArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, ((c ushr 24) and 0xFF).toByte(), ((c ushr 16) and 0xFF).toByte(), ((c ushr 8) and 0xFF).toByte(), (c and 0xFF).toByte()
    )

    private fun unpad32(padded: ByteArray): ByteArray? {
        if (padded.isEmpty()) return null
        val padLen = (padded[padded.lastIndex].toInt() and 0xFF) + 1
        if (padLen < 1 || padLen > 32 || padLen > padded.size) return null
        val dataLen = padded.size - padLen
        return padded.copyOfRange(0, dataLen)
    }
}