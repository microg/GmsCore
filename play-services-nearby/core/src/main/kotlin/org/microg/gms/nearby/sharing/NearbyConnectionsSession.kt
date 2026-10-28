/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.sharing.Frame
import com.google.android.gms.nearby.sharing.PairedKeyEncryptionFrame as SharingPairedKeyEncryptionFrame
import com.google.android.gms.nearby.sharing.PairedKeyResultFrame as SharingPairedKeyResultFrame
import com.google.android.gms.nearby.sharing.V1Frame as SharingV1Frame
import com.google.location.nearby.connections.proto.ConnectionRequestFrame
import com.google.location.nearby.connections.proto.ConnectionResponseFrame
import com.google.location.nearby.connections.proto.OfflineFrame
import com.google.location.nearby.connections.proto.PayloadTransferFrame.PayloadChunk
import com.google.location.nearby.connections.proto.PayloadTransferFrame.PayloadHeader
import com.google.location.nearby.connections.proto.PayloadTransferFrame
import com.google.location.nearby.connections.proto.V1Frame
import com.google.security.cryptauth.lib.securegcm.Ukey2ClientFinished
import com.google.security.cryptauth.lib.securegcm.Ukey2ClientInit
import com.google.security.cryptauth.lib.securegcm.Ukey2HandshakeCipher
import com.google.security.cryptauth.lib.securegcm.Ukey2Message
import com.google.security.cryptauth.lib.securegcm.Ukey2ServerInit
import com.google.security.cryptauth.lib.securemessage.GenericPublicKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Outgoing Connections session: the requester side of one endpoint
 * connection. Mirrors the Nearby Share client's handshake (connection
 * request + UKEY2 + paired-key exchange) but keeps the encrypted session
 * open afterwards so payloads can flow in both directions through
 * [NearbyConnectionsChannel], instead of sending one shot of files and
 * disconnecting.
 *
 * Accept semantics (both sides must accept, as in the real API):
 * - The requester's app accepts/rejects through [accept]/[reject], which
 *   releases the local gate. The remote side's decision arrives as the
 *   Connection Response frame. Only when both accept does the session
 *   enter payload mode and report SUCCESS.
 * - requestConnection's Task is completed by the service when
 *   [Listener.onRequestSent] fires (handshake done, request on the wire);
 *   acceptance/rejection is reported through onConnectionResult.
 */
class NearbyConnectionsSession(
    private val context: Context,
    private val myDeviceName: String,
    private val myEndpointId: String,
    private val targetDevice: NearbyDevice,
    private val listener: Listener
) {
    interface Listener {
        fun onRequestSent()
        fun onConnectionInitiated(pinCode: String)
        fun onConnectionResult(statusCode: Int)
        fun onPayloadReceived(payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?)
        fun onPayloadTransferUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long)
        fun onDisconnected()
        fun onError(error: Throwable)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var channel: NearbyConnectionsChannel? = null
    private val acceptGate = kotlinx.coroutines.CompletableDeferred<Boolean>()
    private val closed = AtomicBoolean(false)
    @Volatile
    var established = false
        private set

    fun start() {
        job = scope.launch { run() }
    }

    /** Called when the app accepts this connection (from onConnectionInitiated). */
    fun accept() {
        acceptGate.complete(true)
    }

    /** Called when the app rejects this connection (from onConnectionInitiated). */
    fun reject() {
        acceptGate.complete(false)
    }

    fun sendBytes(payloadId: Long, bytes: ByteArray): Boolean {
        val ch = channel ?: return false
        return try {
            ch.sendBytes(payloadId, bytes)
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "sendBytes failed for $payloadId", e)
            false
        }
    }

    fun sendStream(payloadId: Long, type: Int, stream: InputStream, totalSize: Long): Boolean {
        val ch = channel ?: return false
        return try {
            ch.sendStream(payloadId, type, stream, totalSize)
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "sendStream failed for $payloadId", e)
            false
        }
    }

    fun cancelPayload(payloadId: Long) {
        channel?.cancelPayload(payloadId)
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            channel?.close()
        } catch (e: Exception) {
            Log.w(NearbySharingConstants.TAG, "Error closing session channel", e)
        }
        job?.cancel()
        scope.cancel()
    }

    private suspend fun run() {
        val host = targetDevice.host
        val port = targetDevice.port
        if (host == null || port <= 0) {
            listener.onError(IllegalArgumentException("Target device has no host address or invalid port"))
            return
        }
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(host, port), 10000)
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val crypto = NearbyCrypto(isServer = false)

            // 1. Connection Request (plain).
            val myEndpointInfo = NearbyEndpointInfo(myDeviceName, NearbyDeviceType.PHONE).serialize()
            val connReq = OfflineFrame(
                version = OfflineFrame.Version.V1,
                v1 = V1Frame(
                    type = V1Frame.FrameType.CONNECTION_REQUEST,
                    connection_request = ConnectionRequestFrame(
                        endpoint_id = myEndpointId,
                        endpoint_name = myDeviceName,
                        endpoint_info = myEndpointInfo.toByteString()
                    )
                )
            )
            NearbySharingServer.writeLengthPrefixed(output, connReq.encode())

            // 2. UKEY2 handshake (plain), mirroring NearbySharingClient.
            val myGenericKey = crypto.getGenericPublicKey()
            val clientFinish = Ukey2ClientFinished(public_key = myGenericKey.encode().toByteString())
            val clientFinishMsg = Ukey2Message(
                message_type = Ukey2Message.Type.CLIENT_FINISH,
                message_data = clientFinish.encode().toByteString()
            )
            val clientFinishRaw = clientFinishMsg.encode()
            val commitment = MessageDigest.getInstance("SHA-512").digest(clientFinishRaw)

            val random32 = ByteArray(32).apply { crypto.random.nextBytes(this) }
            val clientInit = Ukey2ClientInit(
                version = 1,
                random = random32.toByteString(),
                next_protocol = "AES_256_CBC-HMAC_SHA256",
                cipher_commitments = listOf(
                    Ukey2ClientInit.CipherCommitment(
                        handshake_cipher = Ukey2HandshakeCipher.P256_SHA512,
                        commitment = commitment.toByteString()
                    )
                )
            )
            val clientInitMsg = Ukey2Message(
                message_type = Ukey2Message.Type.CLIENT_INIT,
                message_data = clientInit.encode().toByteString()
            )
            val clientInitRaw = clientInitMsg.encode()
            NearbySharingServer.writeLengthPrefixed(output, clientInitRaw)

            val serverInitRaw = NearbySharingServer.readLengthPrefixed(input)
            val serverInitMsg = Ukey2Message.ADAPTER.decode(serverInitRaw)
            if (serverInitMsg.message_type != Ukey2Message.Type.SERVER_INIT) {
                throw IOException("Expected UKEY2 ServerInit")
            }
            val serverInit = Ukey2ServerInit.ADAPTER.decode(serverInitMsg.message_data!!)
            val serverKey = GenericPublicKey.ADAPTER.decode(serverInit.public_key!!)
            crypto.completeKeyExchange(serverKey, clientInitRaw, serverInitRaw)

            NearbySharingServer.writeLengthPrefixed(output, clientFinishRaw)

            listener.onConnectionInitiated(crypto.pinCode)
            listener.onRequestSent()

            // 3. Both gates: local app decision + remote Connection Response.
            val remoteResponse = async {
                val connRespRaw = NearbySharingServer.readLengthPrefixed(input)
                val connResp = OfflineFrame.ADAPTER.decode(connRespRaw)
                connResp.v1?.connection_response?.response == ConnectionResponseFrame.ResponseStatus.ACCEPT
            }
            val localAccepted = acceptGate.await()
            val remoteAccepted = remoteResponse.await()
            if (!localAccepted || !remoteAccepted) {
                listener.onConnectionResult(ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED)
                closeSocket(socket)
                return
            }

            // 4. Paired-key exchange (encrypted), mirroring the Share flow.
            sendPairedKeyFrames(output, crypto)
            // The peer sends its paired-key frames the same way: two frames
            // per key frame (data + EOF), four frames total. Consume them
            // here so they are never mistaken for payload frames.
            repeat(4) {
                val enc = NearbySharingServer.readLengthPrefixed(input)
                crypto.decryptSecureMessage(enc)
            }

            // 5. Payload mode.
            val receiveDir = File(context.cacheDir, "nearby_connections")
            val ch = NearbyConnectionsChannel(socket, crypto, receiveDir, object : NearbyConnectionsChannel.PayloadListener {
                override fun onPayloadReceived(payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?) {
                    listener.onPayloadReceived(payloadId, type, bytes, pfd)
                }

                override fun onPayloadTransferUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long) {
                    listener.onPayloadTransferUpdate(payloadId, status, bytesTransferred, totalBytes)
                }

                override fun onDisconnected() {
                    if (!closed.get()) listener.onDisconnected()
                }

                override fun onError(error: Throwable) {
                    if (!closed.get()) listener.onError(error)
                }
            })
            channel = ch
            established = true
            listener.onConnectionResult(ConnectionsStatusCodes.STATUS_OK)
            try {
                ch.receiveLoop()
            } finally {
                // Peer disconnect ends the loop; release the socket here
                // (the loop itself never closes it).
                try {
                    ch.close()
                } catch (ignored: Exception) {
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!closed.get()) {
                // Before the request is on the wire this surfaces as a
                // request failure; afterwards the service maps it to a
                // disconnect. The service distinguishes via onRequestSent.
                listener.onError(e)
            }
            try {
                socket.close()
            } catch (ignored: IOException) {
            }
        }
    }

    private fun sendPairedKeyFrames(output: OutputStream, crypto: NearbyCrypto) {
        val pairedKeyEnc = Frame(
            version = Frame.Version.V1,
            v1 = SharingV1Frame(
                type = SharingV1Frame.FrameType.PAIRED_KEY_ENCRYPTION,
                paired_key_encryption = SharingPairedKeyEncryptionFrame(
                    secret_id_hash = ByteArray(6).apply { crypto.random.nextBytes(this) }.toByteString(),
                    signed_data = ByteArray(72).apply { crypto.random.nextBytes(this) }.toByteString()
                )
            )
        )
        sendEncryptedSharingFrame(output, crypto, pairedKeyEnc)
        val pairedKeyRes = Frame(
            version = Frame.Version.V1,
            v1 = SharingV1Frame(
                type = SharingV1Frame.FrameType.PAIRED_KEY_RESULT,
                paired_key_result = SharingPairedKeyResultFrame(
                    status = SharingPairedKeyResultFrame.Status.UNABLE
                )
            )
        )
        sendEncryptedSharingFrame(output, crypto, pairedKeyRes)
    }

    private fun sendEncryptedSharingFrame(output: OutputStream, crypto: NearbyCrypto, frame: Frame) {
        val frameBytes = frame.encode()
        val payloadId = SecureRandom().nextLong()
        val transfer = PayloadTransferFrame(
            packet_type = PayloadTransferFrame.PacketType.DATA,
            payload_header = PayloadHeader(
                id = payloadId,
                type = PayloadHeader.PayloadType.BYTES,
                total_size = frameBytes.size.toLong()
            ),
            payload_chunk = PayloadChunk(
                offset = 0,
                flags = 0,
                body = frameBytes.toByteString()
            )
        )
        val offlineFrame = OfflineFrame(
            version = OfflineFrame.Version.V1,
            v1 = V1Frame(
                type = V1Frame.FrameType.PAYLOAD_TRANSFER,
                payload_transfer = transfer
            )
        )
        NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(offlineFrame.encode()))
        val lastChunk = transfer.copy(
            payload_chunk = PayloadChunk(
                offset = frameBytes.size.toLong(),
                flags = 1,
                body = ByteString.EMPTY
            )
        )
        val offlineLast = offlineFrame.copy(v1 = offlineFrame.v1?.copy(payload_transfer = lastChunk))
        NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(offlineLast.encode()))
    }

    private fun closeSocket(socket: Socket) {
        try {
            socket.close()
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "Error closing session socket", e)
        }
    }
}
