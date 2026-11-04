/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import java.io.ByteArrayOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_SHA256 = "HmacSHA256"
private const val SHA256_LENGTH = 32

/**
 * Derives [length] bytes using HKDF with HMAC-SHA-256.
 *
 * @param inputKeyMaterial source key material
 * @param salt non-secret salt; an empty array uses the RFC 5869 default
 * @param info context that binds the derived key to its purpose
 * @param length requested output length in bytes
 */
fun hkdfSha256(
    inputKeyMaterial: ByteArray,
    salt: ByteArray,
    info: ByteArray,
    length: Int,
): ByteArray {
    require(length in 0..(255 * SHA256_LENGTH))
    val extract = Mac.getInstance(HMAC_SHA256).run {
        init(SecretKeySpec(if (salt.isEmpty()) ByteArray(SHA256_LENGTH) else salt, HMAC_SHA256))
        doFinal(inputKeyMaterial)
    }
    val output = ByteArrayOutputStream(length)
    var previous = ByteArray(0)
    var counter = 1
    while (output.size() < length) {
        previous = Mac.getInstance(HMAC_SHA256).run {
            init(SecretKeySpec(extract, HMAC_SHA256))
            update(previous)
            update(info)
            doFinal(byteArrayOf(counter.toByte()))
        }
        output.write(previous)
        counter++
    }
    return output.toByteArray().copyOf(length)
}
