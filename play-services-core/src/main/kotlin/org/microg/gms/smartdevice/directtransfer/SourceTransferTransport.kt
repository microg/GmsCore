/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import okio.ByteString
import java.io.Closeable
import java.io.IOException

/**
 * SourceDirectTransfer's pipe negotiation. The source phone is the UKEY2 SERVER.
 * The caller serializes returned frames with [DirectTransferFrameCodec] and schedules [checkTimeout].
 *
 * [verifyPeer] must bind the handshake to a verified peer/transport or check its out-of-band string.
 * It must not trust names or flags supplied by the remote endpoint. No account authorization is
 * implied by ENCRYPTED: account selection, consent, and recent device authentication remain required.
 */
internal class SourceTransferTransport(
    private val verifyPeer: (ByteString) -> Boolean,
    private val handshake: Ukey2ServerHandshake = Ukey2ServerHandshake(),
    private val handshakeTimeoutMillis: Long = 60_000,
    private val idleTimeoutMillis: Long = 120_000
) : Closeable {
    enum class State { NEW, SELECTION, CLIENT_INIT, CLIENT_FINISH, ENCRYPTED, CLOSED }
    @Volatile var state = State.NEW
        private set
    private var startedAt = 0L
    private var lastActivity = 0L
    private var context: D2DConnectionContextV1? = null
    @Volatile var verificationString: ByteString? = null
        private set

    init { require(handshakeTimeoutMillis > 0 && idleTimeoutMillis > 0) }

    @Synchronized
    fun begin(now: Long): ByteArray = guarded {
        check(state == State.NEW && now >= 0) { "Transfer already started" }
        startedAt = now
        lastActivity = now
        state = State.SELECTION
        // Never advertise the legacy unencrypted mode (bit 1).
        byteArrayOf(8)
    }

    /** Returns the next raw pipe frame, or null when waiting for the peer's ClientInit. */
    @Synchronized
    fun receiveHandshake(frame: ByteArray, now: Long): ByteArray? = guarded {
        checkTimeout(now)
        when (state) {
            State.SELECTION -> {
                require(frame.contentEquals(byteArrayOf(8))) { "Peer did not select UKEY2" }
                state = State.CLIENT_INIT
                null
            }
            State.CLIENT_INIT -> handshake.acceptClientInit(frame).also { state = State.CLIENT_FINISH }
            State.CLIENT_FINISH -> {
                val result = handshake.acceptClientFinish(frame)
                try {
                    check(verifyPeer(result.verificationString)) { "Direct-transfer peer was not verified" }
                    check(state == State.CLIENT_FINISH) { "Direct-transfer verification was cancelled" }
                    verificationString = result.verificationString
                    context = result.context
                    state = State.ENCRYPTED
                    lastActivity = now
                    byteArrayOf(1, 1, 2, 3, 5, 8)
                } catch (e: Exception) { result.context.close(); throw e }
            }
            else -> throw IOException("Unexpected transfer handshake message")
        }.also { lastActivity = now }
    }

    @Synchronized
    fun encode(payload: ByteArray, now: Long): ByteArray = guarded {
        checkTimeout(now)
        check(state == State.ENCRYPTED) { "Transfer is not encrypted" }
        requireNotNull(context).encode(payload).also { lastActivity = now }
    }

    @Synchronized
    fun decode(frame: ByteArray, now: Long): ByteArray = guarded {
        checkTimeout(now)
        check(state == State.ENCRYPTED) { "Transfer is not encrypted" }
        requireNotNull(context).decode(frame).also { lastActivity = now }
    }

    @Synchronized
    fun checkTimeout(now: Long) {
        if (state == State.CLOSED || state == State.NEW || now < lastActivity ||
            (state == State.ENCRYPTED && now - lastActivity >= idleTimeoutMillis) ||
            (state != State.ENCRYPTED && now - startedAt >= handshakeTimeoutMillis)) {
            close()
            throw IOException("Direct-transfer session expired or is closed")
        }
    }

    private inline fun <T> guarded(block: () -> T): T = try { block() } catch (e: Exception) {
        close()
        if (e is IOException) throw e
        throw IOException("Invalid direct-transfer state", e)
    }

    @Synchronized
    override fun close() {
        state = State.CLOSED
        handshake.close()
        context?.close()
        context = null
        verificationString = null
    }
}
