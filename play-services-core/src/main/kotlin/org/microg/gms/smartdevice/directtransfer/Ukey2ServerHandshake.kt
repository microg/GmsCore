/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.cryptauth.CryptAuthCrypto
import org.microg.gms.smartdevice.proto.Ukey2Alert
import org.microg.gms.smartdevice.proto.Ukey2ClientFinished
import org.microg.gms.smartdevice.proto.Ukey2ClientInit
import org.microg.gms.smartdevice.proto.Ukey2Message
import org.microg.gms.smartdevice.proto.Ukey2MessageType
import org.microg.gms.smartdevice.proto.Ukey2ServerInit
import java.io.Closeable
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey

/**
 * P256_SHA512 responder from the public UKEY2 protocol, using the existing Wire/JCA stack.
 * https://github.com/google/ukey2/blob/master/src/main/java/com/google/security/cryptauth/lib/securegcm/Ukey2Handshake.java
 *
 * Completing ECDH does not authenticate a device. The caller must verify [Result.verificationString]
 * out of band, or bind this exchange to an already authenticated transport, before using its context.
 */
internal class Ukey2ServerHandshake(
    keyPair: KeyPair = CryptAuthCrypto.generateKeyPair(),
    private val random: SecureRandom = SecureRandom()
) : Closeable {
    private enum class State { INIT, FINISH, CLOSED }
    private var state = State.INIT
    private var pair: KeyPair? = keyPair
    private var clientInit: ByteArray? = null
    private var serverInit: ByteArray? = null
    private var commitment: ByteArray? = null

    class Result(val verificationString: ByteString, val context: D2DConnectionContextV1) {
        override fun toString() = "Ukey2Result(redacted)"
    }

    @Synchronized
    fun acceptClientInit(frame: ByteArray): ByteArray = guarded {
        check(state == State.INIT) { "Unexpected UKEY2 message" }
        val wrapper = parseWrapper(frame, Ukey2MessageType.CLIENT_INIT, true)
        val init = try { Ukey2ClientInit.ADAPTER.decode(requireNotNull(wrapper.message_data)) }
            catch (_: Exception) { fail(4, "Invalid UKEY2 ClientInit") }
        if (init.version != 1) fail(100, "Unsupported UKEY2 version")
        if (init.random?.size != 32) fail(101, "Invalid UKEY2 nonce")
        if (init.next_protocol != NEXT_PROTOCOL) fail(103, "Unsupported UKEY2 next protocol")
        if (init.cipher_commitments.isEmpty() || init.cipher_commitments.size > 8 ||
            init.cipher_commitments.map { it.handshake_cipher }.distinct().size != init.cipher_commitments.size) {
            fail(102, "Invalid UKEY2 cipher commitments")
        }
        val chosen = init.cipher_commitments.singleOrNull { it.handshake_cipher == P256_SHA512 }
        if (chosen?.commitment?.size != 64) fail(102, "Missing UKEY2 P-256 commitment")
        commitment = requireNotNull(chosen?.commitment).toByteArray()
        clientInit = frame.clone()
        val payload = Ukey2ServerInit(
            version = 1,
            random = ByteArray(32).also(random::nextBytes).toByteString(),
            handshake_cipher = P256_SHA512,
            public_key = CryptAuthCrypto.publicKey(requireNotNull(pair).public as ECPublicKey)
        )
        val response = Ukey2Message(Ukey2MessageType.SERVER_INIT, payload.encodeByteString()).encode()
        serverInit = response.clone()
        state = State.FINISH
        response
    }

    @Synchronized
    fun acceptClientFinish(frame: ByteArray): Result = guarded {
        check(state == State.FINISH) { "Unexpected UKEY2 message" }
        val wrapper = parseWrapper(frame, Ukey2MessageType.CLIENT_FINISH, false)
        if (!MessageDigest.isEqual(requireNotNull(commitment), MessageDigest.getInstance("SHA-512").digest(frame))) {
            throw Ukey2ProtocolException("UKEY2 commitment mismatch")
        }
        val finished = Ukey2ClientFinished.ADAPTER.decode(requireNotNull(wrapper.message_data))
        val peer = requireNotNull(finished.public_key) { "Missing UKEY2 peer key" }
        val secret = CryptAuthCrypto.agreement(requireNotNull(pair), peer)
        val transcript = requireNotNull(clientInit) + requireNotNull(serverInit)
        try {
            val verification = CryptAuthCrypto.hkdf(secret, "UKEY2 v1 auth".toByteArray(Charsets.UTF_8), transcript, 32)
            val next = CryptAuthCrypto.hkdf(secret, "UKEY2 v1 next".toByteArray(Charsets.UTF_8), transcript, 32)
            try {
                val salt = MessageDigest.getInstance("SHA-256").digest("D2D".toByteArray(Charsets.UTF_8))
                val encode = CryptAuthCrypto.hkdf(next, salt, "server".toByteArray(Charsets.UTF_8), 32)
                val decode = CryptAuthCrypto.hkdf(next, salt, "client".toByteArray(Charsets.UTF_8), 32)
                try { Result(verification.toByteString(), D2DConnectionContextV1(encode, decode)) }
                finally { encode.fill(0); decode.fill(0) }
            } finally { next.fill(0); verification.fill(0) }
        } finally { secret.fill(0); transcript.fill(0); close() }
    }

    private fun parseWrapper(frame: ByteArray, expected: Ukey2MessageType, sendAlert: Boolean): Ukey2Message {
        if (frame.size !in 1..MAX_HANDSHAKE_BYTES) {
            if (sendAlert) fail(1, "Invalid UKEY2 frame size")
            throw Ukey2ProtocolException("Invalid UKEY2 frame size")
        }
        val message = try { Ukey2Message.ADAPTER.decode(frame) }
            catch (_: Exception) {
                if (sendAlert) fail(1, "Invalid UKEY2 frame")
                throw Ukey2ProtocolException("Invalid UKEY2 frame")
            }
        if (message.message_type == Ukey2MessageType.ALERT) throw Ukey2ProtocolException("Peer aborted UKEY2")
        if (message.message_type != expected || message.message_data == null) {
            if (sendAlert) fail(2, "Unexpected UKEY2 message type")
            throw Ukey2ProtocolException("Unexpected UKEY2 message type")
        }
        return message
    }

    private inline fun <T> guarded(block: () -> T): T = try { block() } catch (e: Exception) {
        close()
        if (e is Ukey2ProtocolException) throw e
        throw Ukey2ProtocolException("Invalid UKEY2 exchange", cause = e)
    }

    private fun fail(code: Int, reason: String): Nothing {
        val alert = Ukey2Message(Ukey2MessageType.ALERT, Ukey2Alert(code).encodeByteString()).encode()
        throw Ukey2ProtocolException(reason, alert)
    }

    @Synchronized
    override fun close() {
        state = State.CLOSED
        clientInit?.fill(0); serverInit?.fill(0); commitment?.fill(0)
        clientInit = null; serverInit = null; commitment = null; pair = null
    }

    companion object {
        const val MAX_HANDSHAKE_BYTES = 4096
        const val P256_SHA512 = 100
        const val NEXT_PROTOCOL = "AES_256_CBC-HMAC_SHA256"
    }
}

internal class Ukey2ProtocolException(message: String, val alert: ByteArray? = null, cause: Throwable? = null) :
    java.io.IOException(message, cause)
