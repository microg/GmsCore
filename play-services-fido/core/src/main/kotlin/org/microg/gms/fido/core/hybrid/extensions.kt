/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid

import android.os.ParcelUuid
import kotlinx.coroutines.CancellableContinuation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class HandshakePhase { NONE, CLIENT_HELLO_SENT, READY }
enum class CtapError(val value: Byte) {
    SUCCESS(0x00), INVALID_COMMAND(0x01), INVALID_LENGTH(0x03), INVALID_CBOR(0x12), MISSING_PARAMETER(0x14), OTHER_ERROR(0x7F),
}

const val HKDF_ALGORITHM = "HmacSHA256"
const val AEMK_ALGORITHM = "AES"
const val EC_ALGORITHM = "EC"
val UUID_ANDROID: ParcelUuid = ParcelUuid.fromString("0000fff9-0000-1000-8000-00805f9b34fb")
val UUID_IOS: ParcelUuid = ParcelUuid.fromString("0000fde2-0000-1000-8000-00805f9b34fb")
val EMPTY_SERVICE_DATA = ByteArray(20)
val EMPTY_SERVICE_DATA_MASK = ByteArray(20)

val FIXED_SERVER_HOSTS = arrayOf("cable.ua5v.com", "cable.auth.com")
val DOMAIN_SUFFIXES = arrayOf(".com", ".org", ".net", ".info")
val SERVER_BANNER_BYTES = byteArrayOf(99, 97, 66, 76, 69, 118, 50, 32, 116, 117, 110, 110, 101, 108, 32, 115, 101, 114, 118, 101, 114, 32, 100, 111, 109, 97, 105, 110)
val BASE32_ALPHABET = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '2', '3', '4', '5', '6', '7')

fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

fun generateEcKeyPair(): Pair<ECPublicKey, ECPrivateKey> {
    val kpg = KeyPairGenerator.getInstance(EC_ALGORITHM).apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }
    val kp = kpg.generateKeyPair()
    return (kp.public as ECPublicKey) to (kp.private as ECPrivateKey)
}

private fun generateDomain(domainId: Int): String {
    if (domainId < 2) {
        return FIXED_SERVER_HOSTS[domainId]
    }
    require(domainId < 256) {
        String.format(Locale.US, "This domainId: %d was an unrecognized assigned domain value.", domainId)
    }
    val buffer = ByteBuffer.allocate(31).apply {
        order(ByteOrder.LITTLE_ENDIAN)
        put(SERVER_BANNER_BYTES)
        putShort(domainId.toShort())
        put(0)
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(buffer.array())
    val hash = ByteBuffer.wrap(digest.copyOf(8)).order(ByteOrder.LITTLE_ENDIAN).long
    val suffixIndex = (hash and 3).toInt()
    val sb = StringBuilder("cable.")
    var body = hash ushr 2
    while (body != 0L) {
        sb.append(BASE32_ALPHABET[(body and 31).toInt()])
        body = body ushr 5
    }
    sb.append(DOMAIN_SUFFIXES[suffixIndex])
    return sb.toString()
}

fun buildWebSocketConnectUrl(domainId: Int, routingId: ByteArray, tunnelId: ByteArray) = buildString {
    append("wss://")
    append(generateDomain(domainId))
    append("/cable/connect/")
    append(routingId.hex())
    append("/")
    append(tunnelId.hex())
}

fun buildWebSocketNewUrl(tunnelId: ByteArray) = buildString {
    append("wss://")
    append(generateDomain(0))
    append("/cable/new/")
    append(tunnelId.hex())
}

fun <T> CancellableContinuation<T>.tryResumeData(value: T) {
    if (!isCompleted) resume(value)
}

fun <T> CancellableContinuation<T>.tryResumeWithError(e: Throwable) {
    if (!isCompleted) resumeWithException(e)
}