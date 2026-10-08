/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.cast.proto.AuthChallenge
import org.microg.gms.cast.proto.CastMessage
import org.microg.gms.cast.proto.DeviceAuthMessage
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

const val NAMESPACE_DEVICE_AUTH = "urn:x-cast:com.google.cast.tp.deviceauth"
const val NAMESPACE_CONNECTION = "urn:x-cast:com.google.cast.tp.connection"
const val NAMESPACE_HEARTBEAT = "urn:x-cast:com.google.cast.tp.heartbeat"
const val NAMESPACE_RECEIVER = "urn:x-cast:com.google.cast.receiver"
const val NAMESPACE_MEDIA = "urn:x-cast:com.google.cast.media"

const val RECEIVER_ID = "receiver-0"
const val BROADCAST_ID = "*"

/** Largest serialized CastMessage body a receiver accepts, excluding the 4-byte length prefix. */
const val MAX_PAYLOAD_SIZE = 64 * 1024

class MessageTooLargeException(size: Int, serializedBody: Boolean) : IOException(
    "Cast message ${if (serializedBody) "serialized body" else "raw payload"} of $size bytes exceeds $MAX_PAYLOAD_SIZE"
)

class DeviceAuthException(message: String) : IOException(message)

/**
 * One CastV2 control connection to a receiver: TLS socket, length-prefixed CastMessage frames, device authentication,
 * virtual connections and heartbeat.
 *
 * [connect] blocks and must not be called on the main thread. Sending is thread safe. Received messages are handed to
 * [Listener.onMessage] on the reader thread, except heartbeat traffic, which is answered here.
 */
class CastChannel(
    private val host: String,
    private val port: Int,
    private val listener: Listener,
    val senderId: String = "sender-0",
) {
    interface Listener {
        fun onMessage(message: CastMessage)

        /** The receiver closed the virtual connection to [transportId]. */
        fun onTransportClosed(transportId: String) {}

        /** The channel is gone. [error] is null if it was closed through [close]. */
        fun onClosed(error: IOException?)
    }

    private val lock = Any()
    private var socket: SSLSocket? = null
    private var output: DataOutputStream? = null
    private var reader: Thread? = null
    private var heartbeat: ScheduledExecutorService? = null
    private var heartbeatTask: ScheduledFuture<*>? = null
    private val connectedTransports = HashSet<String>()

    @Volatile
    private var lastReceivedNanos = 0L

    @Volatile
    private var closed = false

    val isConnected: Boolean
        get() = synchronized(lock) { socket != null && !closed }

    @Throws(IOException::class)
    fun connect(timeoutMillis: Int = CONNECT_TIMEOUT_MILLIS) {
        synchronized(lock) {
            check(socket == null && !closed) { "connect() can only be called once" }
            val context = SSLContext.getInstance("TLS")
            // Receivers present self-signed certificates; the receiver is identified through device authentication.
            context.init(null, arrayOf(TrustAllManager), SecureRandom())
            val sslSocket = context.socketFactory.createSocket() as SSLSocket
            try {
                sslSocket.enabledProtocols = sslSocket.supportedProtocols.filter { it.startsWith("TLSv1") }.toTypedArray()
                sslSocket.connect(InetSocketAddress(host, port), timeoutMillis)
                sslSocket.soTimeout = timeoutMillis
                sslSocket.startHandshake()
                socket = sslSocket
                output = DataOutputStream(sslSocket.outputStream.buffered())
                val input = DataInputStream(sslSocket.inputStream.buffered())
                authenticate(input)
                sslSocket.soTimeout = 0
                writeLocked(RECEIVER_ID, NAMESPACE_CONNECTION, CONNECT_PAYLOAD)
                connectedTransports.add(RECEIVER_ID)
                lastReceivedNanos = System.nanoTime()
                reader = Thread({ readLoop(input) }, "CastChannel-$host").apply { isDaemon = true; start() }
                heartbeat = Executors.newSingleThreadScheduledExecutor { Thread(it, "CastHeartbeat-$host").apply { isDaemon = true } }.also {
                    heartbeatTask = it.scheduleWithFixedDelay(::heartbeatTick, HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                }
            } catch (e: Exception) {
                socket = null
                output = null
                runCatching { sslSocket.close() }
                closed = true
                throw e as? IOException ?: IOException(e)
            }
        }
    }

    private fun authenticate(input: DataInputStream) {
        val challenge = DeviceAuthMessage(challenge = AuthChallenge())
        writeLocked(RECEIVER_ID, NAMESPACE_DEVICE_AUTH, challenge.encodeByteString())
        while (true) {
            val message = readMessage(input)
            if (message.namespace != NAMESPACE_DEVICE_AUTH) continue
            val reply = DeviceAuthMessage.ADAPTER.decode(message.payload_binary ?: ByteString.EMPTY)
            reply.error?.let { throw DeviceAuthException("Device authentication failed: ${it.error_type}") }
            if (reply.response == null) throw DeviceAuthException("Device authentication reply without response")
            return
        }
    }

    /** Opens the virtual connection to [transportId] (a launched application) if it is not open yet. */
    @JvmOverloads
    @Throws(IOException::class)
    fun connectTransport(transportId: String, connectionType: Int = 0) {
        synchronized(lock) {
            if (connectedTransports.contains(transportId)) return
            // The receiver platform connection is always strong; the client selects the application connection type.
            val type = if (transportId == RECEIVER_ID) 0 else connectionType
            writeLocked(transportId, NAMESPACE_CONNECTION, """{"type":"CONNECT","connType":$type,"origin":{}}""")
            connectedTransports.add(transportId)
        }
    }

    /** Closes the virtual connection to [transportId], the application keeps running on the receiver. */
    @Throws(IOException::class)
    fun closeTransport(transportId: String) {
        synchronized(lock) {
            if (!connectedTransports.contains(transportId)) return
            writeLocked(transportId, NAMESPACE_CONNECTION, CLOSE_PAYLOAD)
            connectedTransports.remove(transportId)
        }
    }

    @Throws(IOException::class)
    fun send(destinationId: String, namespace: String, payload: String) {
        val size = payload.toByteArray(Charsets.UTF_8).size
        if (size > MAX_PAYLOAD_SIZE) throw MessageTooLargeException(size, serializedBody = false)
        synchronized(lock) { writeLocked(destinationId, namespace, payload) }
    }

    @Throws(IOException::class)
    fun send(destinationId: String, namespace: String, payload: ByteArray) {
        if (payload.size > MAX_PAYLOAD_SIZE) throw MessageTooLargeException(payload.size, serializedBody = false)
        synchronized(lock) { writeLocked(destinationId, namespace, payload.toByteString()) }
    }

    fun close() = shutdown(null)

    private fun writeLocked(destinationId: String, namespace: String, payload: String) =
        writeLocked(CastMessage(CastMessage.ProtocolVersion.CASTV2_1_0, senderId, destinationId, namespace, CastMessage.PayloadType.STRING, payload_utf8 = payload))

    private fun writeLocked(destinationId: String, namespace: String, payload: ByteString) =
        writeLocked(CastMessage(CastMessage.ProtocolVersion.CASTV2_1_0, senderId, destinationId, namespace, CastMessage.PayloadType.BINARY, payload_binary = payload))

    private fun writeLocked(message: CastMessage) {
        val out = output ?: throw IOException("Cast channel to $host is not connected")
        if (closed) throw IOException("Cast channel to $host is closed")
        val bodySize = CastMessage.ADAPTER.encodedSize(message)
        if (bodySize > MAX_PAYLOAD_SIZE) throw MessageTooLargeException(bodySize, serializedBody = true)
        val bytes = message.encode()
        out.writeInt(bodySize)
        out.write(bytes)
        out.flush()
    }

    private fun readMessage(input: DataInputStream): CastMessage {
        val length = input.readInt()
        if (length < 0 || length > MAX_PAYLOAD_SIZE) throw IOException("Invalid Cast frame length $length")
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return CastMessage.ADAPTER.decode(bytes)
    }

    private fun controlMessageType(message: CastMessage): String? {
        if (message.payload_type != CastMessage.PayloadType.STRING) return null
        val payload = message.payload_utf8 ?: return null
        return try {
            JSONObject(payload).opt("type") as? String
        } catch (_: JSONException) {
            null
        }
    }

    private fun readLoop(input: DataInputStream) {
        try {
            while (!closed) {
                val message = readMessage(input)
                // Control frames must address this sender or all senders before they affect the channel.
                if (message.destination_id != senderId && message.destination_id != BROADCAST_ID) continue
                lastReceivedNanos = System.nanoTime()
                when (message.namespace) {
                    NAMESPACE_HEARTBEAT -> if (controlMessageType(message) == "PING") {
                        synchronized(lock) { writeLocked(message.source_id, NAMESPACE_HEARTBEAT, PONG_PAYLOAD) }
                    }

                    NAMESPACE_CONNECTION -> if (controlMessageType(message) == "CLOSE") {
                        val removed = synchronized(lock) { connectedTransports.remove(message.source_id) }
                        if (message.source_id == RECEIVER_ID) throw EOFException("Receiver closed the connection")
                        if (removed) listener.onTransportClosed(message.source_id)
                    }

                    else -> listener.onMessage(message)
                }
            }
        } catch (e: IOException) {
            shutdown(if (closed) null else e)
        } catch (e: RuntimeException) {
            shutdown(IOException("Cast channel reader failed", e))
        }
    }

    private fun heartbeatTick() {
        // Wall-clock corrections must not change the elapsed heartbeat timeout.
        if (System.nanoTime() - lastReceivedNanos > TimeUnit.MILLISECONDS.toNanos(HEARTBEAT_TIMEOUT_MILLIS)) {
            shutdown(SocketTimeoutException("No message from $host for ${HEARTBEAT_TIMEOUT_MILLIS}ms"))
            return
        }
        try {
            synchronized(lock) { writeLocked(RECEIVER_ID, NAMESPACE_HEARTBEAT, PING_PAYLOAD) }
        } catch (e: IOException) {
            shutdown(e)
        }
    }

    private fun shutdown(error: IOException?) {
        synchronized(lock) {
            if (closed && socket == null) return
            if (error == null && !closed && output != null) {
                for (transport in connectedTransports) runCatching { writeLocked(transport, NAMESPACE_CONNECTION, CLOSE_PAYLOAD) }
            }
            closed = true
            connectedTransports.clear()
            heartbeatTask?.cancel(false)
            heartbeat?.shutdown()
            runCatching { socket?.close() }
            socket = null
            output = null
        }
        listener.onClosed(error)
    }

    private object TrustAllManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    companion object {
        const val DEFAULT_PORT = 8009
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val HEARTBEAT_INTERVAL_MILLIS = 5_000L
        const val HEARTBEAT_TIMEOUT_MILLIS = 20_000L
        private const val CONNECT_PAYLOAD = """{"type":"CONNECT","connType":0,"origin":{}}"""
        private const val CLOSE_PAYLOAD = """{"type":"CLOSE"}"""
        private const val PING_PAYLOAD = """{"type":"PING"}"""
        private const val PONG_PAYLOAD = """{"type":"PONG"}"""
    }
}
