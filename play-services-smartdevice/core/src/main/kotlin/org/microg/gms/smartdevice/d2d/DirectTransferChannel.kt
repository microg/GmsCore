/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.os.Handler
import android.os.Looper
import com.google.android.gms.smartdevice.d2d.data.MessagePayload
import java.io.EOFException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

internal class DirectTransferChannel(
    private val sessionHandler: Handler,
    private val readerExecutor: Executor,
    private val input: D2DInputStream,
    private val output: D2DOutputStream,
    private val supportsUnencrypted: Boolean,
    private val onMessageActivity: () -> Unit,
    private val onPayload: (MessagePayload) -> Unit,
    private val onFailure: (Int, String, Throwable?) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val readingStopped = AtomicBoolean(false)
    @Volatile private var encryptor: D2DEncryptor = D2DEncryptor.None
    @Volatile private var ready = false
    private var selectionReceived = false
    @Volatile private var packetMode = false
    private var packetCodec: PacketProcessor? = null
    private val negotiator = EncryptionNegotiator(
        supportsUnencrypted = supportsUnencrypted,
        stream = output,
        onComplete = { negotiatedEncryptor ->
            encryptor = negotiatedEncryptor
            ready = true
        },
        onWriteFailure = { failure ->
            reportFailure(
                D2D_STATUS_MESSAGE_SEND_ERROR,
                "Error writing encryption message",
                failure,
            )
        },
    )

    val isReady: Boolean
        get() = ready

    fun start() {
        check(!closed.get()) { "DirectTransfer channel is closed" }
        negotiator.start()
        readerExecutor.execute(::readInboundLoop)
    }

    @Synchronized
    fun send(
        payload: MessagePayload,
        onComplete: ((Throwable?) -> Unit)? = null,
    ): Future<Unit> {
        check(ready) { "Encryption negotiation is incomplete" }
        onMessageActivity()
        val encoded = payload.aw()
        val encrypted = try {
            encryptor.encrypt(encoded)
        } catch (e: SecurityException) {
            if (onComplete != null) {
                onComplete(e)
            } else {
                reportFailure(
                    D2D_STATUS_ENCRYPT_MESSAGE_ERROR,
                    "Error encrypting MessagePayload",
                    e,
                )
            }
            return failedFuture(e)
        }
        val packets = packetCodec?.takeIf { packetMode }?.split(encrypted) ?: listOf(encrypted)
        return output.writeFrames(packets) { failure ->
            if (onComplete != null) {
                onComplete(failure)
            } else if (failure != null) {
                reportFailure(
                    D2D_STATUS_MESSAGE_SEND_ERROR,
                    "Error sending MessagePayload",
                    failure,
                )
            }
        }
    }

    @Synchronized
    fun enablePacketMode(packetSize: Int) {
        require(packetSize > 0) { "Invalid target maxPacketSize=$packetSize" }
        packetCodec = PacketProcessor(packetSize)
        packetMode = true
    }

    fun stopReading() {
        if (readingStopped.compareAndSet(false, true)) input.closeQuietly()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        stopReading()
        output.closeQuietly()
    }

    private fun readInboundLoop() {
        try {
            while (!closed.get() && !readingStopped.get()) {
                val frame = input.readFrame()
                onMessageActivity()
                if (!sessionHandler.post {
                        if (!closed.get() && !readingStopped.get()) handleFrame(frame)
                    }
                ) {
                    reportFailure(
                        D2D_STATUS_SESSION_INVALID,
                        "Unable to dispatch DirectTransfer input",
                        null,
                    )
                    return
                }
            }
        } catch (_: EOFException) {
            if (!closed.get() && !readingStopped.get()) {
                reportFailure(
                    D2D_STATUS_SESSION_INVALID,
                    "DirectTransfer input pipe closed",
                    null,
                )
            }
        } catch (e: Exception) {
            if (!closed.get() && !readingStopped.get()) {
                reportFailure(
                    D2D_STATUS_SESSION_INVALID,
                    e.message ?: "DirectTransfer input failed",
                    e,
                )
            }
        }
    }

    private fun handleFrame(frame: ByteArray) {
        if (!ready) {
            val plaintextPayload = if (supportsUnencrypted) decodePlaintextPayload(frame) else null
            if (plaintextPayload != null) {
                negotiator.onPlaintextDuringNegotiation()
                onPayload(plaintextPayload)
                return
            }

            if (!selectionReceived) {
                if (frame.size != D2D_ENCRYPTION_OFFER_BYTE_LENGTH) {
                    onFailure(
                        D2D_STATUS_ENCRYPTION_NEGOTIATION_ERROR,
                        "Error establishing encrypted connection: expected one-byte encryption selection",
                        null,
                    )
                    return
                }
                selectionReceived = true
                try {
                    negotiator.selectEncryption(frame[0])
                } catch (e: SecurityException) {
                    onFailure(
                        D2D_STATUS_ENCRYPTION_NEGOTIATION_ERROR,
                        "Error establishing encrypted connection: ${e.message}",
                        e,
                    )
                }
            } else {
                try {
                    negotiator.feedPeerBytes(frame)
                } catch (e: SecurityException) {
                    onFailure(
                        D2D_STATUS_ENCRYPTION_NEGOTIATION_ERROR,
                        "Error establishing encrypted connection: ${e.message}",
                        e,
                    )
                }
            }
            return
        }

        val encryptedPayload = if (packetMode) {
            try {
                packetCodec?.accept(frame) ?: return
            } catch (e: Exception) {
                onFailure(
                    D2D_STATUS_PACKET_PROCESSING_ERROR,
                    e.message ?: "Unable to decode packet frame",
                    e,
                )
                return
            }
        } else {
            frame
        }

        val payload = try {
            val messagePayload = MessagePayload()
            messagePayload.av(encryptor.decrypt(encryptedPayload))
            messagePayload
        } catch (e: SecurityException) {
            onFailure(
                D2D_STATUS_ENCRYPT_DECRYPT_ERROR,
                e.message ?: "Unable to decrypt MessagePayload",
                e,
            )
            return
        } catch (e: Exception) {
            onFailure(
                D2D_STATUS_BAD_COMPLETE_ORDER,
                e.message ?: "Unable to decode MessagePayload",
                e,
            )
            return
        }
        onPayload(payload)
    }

    private fun decodePlaintextPayload(frame: ByteArray): MessagePayload? = runCatching {
        val payload = MessagePayload()
        payload.av(frame)
        payload
    }.getOrNull()?.takeIf { it.effectiveProtocolVersion > 0 }

    private fun reportFailure(code: Int, message: String, cause: Throwable?) {
        if (closed.get() || readingStopped.get()) return
        val action = Runnable {
            if (!closed.get() && !readingStopped.get()) onFailure(code, message, cause)
        }
        if (Looper.myLooper() == sessionHandler.looper) {
            action.run()
        } else if (!sessionHandler.post(action)) {
            action.run()
        }
    }

    private fun failedFuture(failure: Throwable): Future<Unit> =
        FutureTask<Unit> { throw failure }.apply { run() }
}
