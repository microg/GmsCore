/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.util.Log

private const val TAG = "D2DEncryptionNegotiator"

class EncryptionNegotiator(
    private val supportsUnencrypted: Boolean,
    private val stream: D2DOutputStream,

    private val supportedByte: Int = computeSupportedByte(supportsUnencrypted),
    private val onComplete: (D2DEncryptor) -> Unit,
    private val onWriteFailure: (Throwable) -> Unit = {},
) {
    @Volatile private var algorithmByte: Int = -1
    @Volatile private var handshake: Ukey2Handshake? = null
    @Volatile private var encryptor: D2DEncryptor = D2DEncryptor.None
    private val negotiated = java.util.concurrent.atomic.AtomicBoolean(false)

    fun onPlaintextDuringNegotiation() {
        if (!negotiated.get() && supportsUnencrypted) {
            Log.w(TAG, "Plaintext during encryption phase — aborting encryption, NONE")

            encryptor = D2DEncryptor.None
            negotiated.set(true)
            onComplete(D2DEncryptor.None)
        }
    }

    fun start() {
        val offer = supportedByte
        writeFrame(byteArrayOf(offer.toByte()))
    }

    @Throws(SecurityException::class)
    fun selectEncryption(peerByte: Byte) {
        if (negotiated.get()) return
        val peeked = (peerByte.toInt() and 0xff) and supportedByte
        val selected = Integer.highestOneBit(peeked)
        if (selected == 0) {
            throw SecurityException("No common encryption algorithm (peer=0x${(peerByte.toInt() and 0xff).toString(16)}, support=0x${supportedByte.toString(16)})")
        }
        algorithmByte = selected
        when (selected) {
            ENCRYPTION_ALGORITHM_NONE -> {
                encryptor = D2DEncryptor.None
                negotiated.set(true)
                onComplete(encryptor)
            }
            ENCRYPTION_ALGORITHM_UKEY2 -> {
                val newHandshake = Ukey2Handshake(role = UKEY2_VERSION_V2)
                newHandshake.onFirstByte(selected)
                handshake = newHandshake
                drainOutbound()
            }
            else -> throw SecurityException("Unknown encryption type $selected")
        }
    }

    @Throws(SecurityException::class)
    fun feedPeerBytes(bytes: ByteArray): Boolean {
        val h = handshake ?: throw SecurityException("Handshake not initialized")
        h.consume(bytes)


        drainOutbound()
        if (!h.isCompleted()) {
            return false
        }

        val c = h.cipher ?: throw SecurityException("Handshake completed but no cipher produced")
        encryptor = cipherToEncryptor(c)
        negotiated.set(true)
        onComplete(encryptor)
        return true
    }

    private fun drainOutbound() {
        val h = handshake ?: return
        while (true) {
            val out = h.pollOutbound() ?: break
            writeFrame(out)
        }
    }

    private fun writeFrame(payload: ByteArray) {
        stream.writeFrame(payload) { failure ->
            if (failure != null) onWriteFailure(failure)
        }
    }

    private fun cipherToEncryptor(c: Ukey2Cipher): D2DEncryptor = object : D2DEncryptor {
        override fun encrypt(plaintext: ByteArray): ByteArray = try {
            c.encrypt(plaintext)
        } catch (e: java.security.SignatureException) {
            throw SecurityException("SignatureException when encoding data with the Ukey2 protocol: ${e.message}")
        }
        override fun decrypt(ciphertext: ByteArray): ByteArray = try {
            c.decrypt(ciphertext)
        } catch (e: java.security.SignatureException) {
            throw SecurityException("SignatureException when decoding data with the Ukey2 protocol: ${e.message}")
        }
    }

    companion object {
        fun computeSupportedByte(supportsUnencrypted: Boolean): Int =
            if (supportsUnencrypted) (ENCRYPTION_ALGORITHM_UKEY2 or ENCRYPTION_ALGORITHM_NONE)
            else ENCRYPTION_ALGORITHM_UKEY2
    }
}
