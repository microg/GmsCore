/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import okio.ByteString.Companion.toByteString
import org.microg.gms.cryptauth.CryptAuthCrypto
import org.microg.gms.cryptauth.proto.EncScheme
import org.microg.gms.cryptauth.proto.Header
import org.microg.gms.cryptauth.proto.HeaderAndBodyInternal
import org.microg.gms.cryptauth.proto.SecureMessage
import org.microg.gms.cryptauth.proto.SigScheme
import org.microg.gms.smartdevice.proto.D2DGcmMetadata
import org.microg.gms.smartdevice.proto.D2DMessage
import java.io.Closeable
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Public SecureGCM D2D v1: independent directional keys and authenticated, increasing counters.
 * https://github.com/google/ukey2/blob/master/src/main/java/com/google/security/cryptauth/lib/securegcm/D2DConnectionContextV1.java
 * The HMAC is checked before decrypting. A rejected frame permanently closes this context.
 */
internal class D2DConnectionContextV1(encodeKey: ByteArray, decodeKey: ByteArray,
                                   private val random: SecureRandom = SecureRandom()) : Closeable {
    private val encodeEncryption: ByteArray
    private val encodeAuthentication: ByteArray
    private val decodeEncryption: ByteArray
    private val decodeAuthentication: ByteArray
    private var encodeSequence = 0
    private var decodeSequence = 0
    private var closed = false

    init {
        require(encodeKey.size == 32 && decodeKey.size == 32) { "Invalid D2D keys" }
        val salt = MessageDigest.getInstance("SHA-256").digest("SecureMessage".toByteArray(Charsets.UTF_8))
        fun derive(key: ByteArray, purpose: String) = CryptAuthCrypto.hkdf(key, salt, purpose.toByteArray(Charsets.UTF_8), 32)
        encodeEncryption = derive(encodeKey, "ENC:2")
        encodeAuthentication = derive(encodeKey, "SIG:1")
        decodeEncryption = derive(decodeKey, "ENC:2")
        decodeAuthentication = derive(decodeKey, "SIG:1")
    }

    @Synchronized
    fun encode(message: ByteArray): ByteArray = guarded {
        require(message.size in 1..MAX_PAYLOAD_BYTES) { "Invalid D2D payload length" }
        check(encodeSequence < Int.MAX_VALUE) { "D2D sequence exhausted" }
        val next = encodeSequence + 1
        val plaintext = D2DMessage(message.toByteString(), next).encode()
        try {
            val iv = ByteArray(16).also(random::nextBytes)
            val header = Header(signature_scheme = SigScheme.HMAC_SHA256,
                encryption_scheme = EncScheme.AES_256_CBC, iv = iv.toByteString(),
                public_metadata = D2DGcmMetadata(type = 13, version = 1).encodeByteString())
            val body = crypt(Cipher.ENCRYPT_MODE, encodeEncryption, iv, plaintext)
            val headerAndBody = HeaderAndBodyInternal(header.encodeByteString(), body.toByteString()).encodeByteString()
            val signature = authenticate(encodeAuthentication, headerAndBody.toByteArray())
            val encoded = SecureMessage(headerAndBody, signature.toByteString()).encode()
            check(encoded.size <= DirectTransferFrameCodec.MAX_FRAME_BYTES) { "D2D frame too large" }
            encodeSequence = next
            encoded
        } finally { plaintext.fill(0) }
    }

    @Synchronized
    fun decode(frame: ByteArray): ByteArray = guarded {
        require(frame.size in 1..DirectTransferFrameCodec.MAX_FRAME_BYTES) { "Invalid D2D frame length" }
        check(decodeSequence < Int.MAX_VALUE) { "D2D sequence exhausted" }
        val message = SecureMessage.ADAPTER.decode(frame)
        require(message.signature.size == 32 && MessageDigest.isEqual(message.signature.toByteArray(),
            authenticate(decodeAuthentication, message.header_and_body.toByteArray()))) { "Invalid D2D authentication" }
        val body = HeaderAndBodyInternal.ADAPTER.decode(message.header_and_body)
        val header = Header.ADAPTER.decode(body.header_)
        require(header.signature_scheme == SigScheme.HMAC_SHA256 && header.encryption_scheme == EncScheme.AES_256_CBC &&
            header.iv?.size == 16 && (header.associated_data_length ?: 0) == 0 &&
            header.verification_key_id == null && header.decryption_key_id == null) { "Unsupported D2D header" }
        val metadata = D2DGcmMetadata.ADAPTER.decode(requireNotNull(header.public_metadata))
        require(metadata.type == 13 && (metadata.version ?: 0) in 0..1) { "Unsupported D2D metadata" }
        val plaintext = crypt(Cipher.DECRYPT_MODE, decodeEncryption, requireNotNull(header.iv).toByteArray(), body.body.toByteArray())
        try {
            val payload = D2DMessage.ADAPTER.decode(plaintext)
            require(payload.sequence_number == decodeSequence + 1) { "Unexpected D2D sequence" }
            val bytes = requireNotNull(payload.message) { "Missing D2D payload" }
            require(bytes.size in 1..MAX_PAYLOAD_BYTES) { "Invalid D2D payload length" }
            decodeSequence++
            bytes.toByteArray()
        } finally { plaintext.fill(0) }
    }

    private inline fun <T> guarded(block: () -> T): T = try {
        check(!closed) { "D2D context is closed" }
        block()
    } catch (e: Exception) {
        close()
        throw IOException("Invalid encrypted direct-transfer message", e)
    }

    private fun crypt(mode: Int, key: ByteArray, iv: ByteArray, bytes: ByteArray): ByteArray =
        Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            doFinal(bytes)
        }

    private fun authenticate(key: ByteArray, bytes: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(bytes)
    }

    @Synchronized
    override fun close() {
        closed = true
        encodeEncryption.fill(0); encodeAuthentication.fill(0)
        decodeEncryption.fill(0); decodeAuthentication.fill(0)
    }

    companion object {
        const val MAX_PAYLOAD_BYTES = DirectTransferFrameCodec.MAX_FRAME_BYTES - 512
    }
}
