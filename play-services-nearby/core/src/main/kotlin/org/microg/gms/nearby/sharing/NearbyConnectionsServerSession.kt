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
import com.google.location.nearby.connections.proto.ConnectionResponseFrame
import com.google.location.nearby.connections.proto.OfflineFrame
import com.google.location.nearby.connections.proto.OsInfo
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Incoming Connections session: the advertiser side of one endpoint
 * connection. Mirrors the Nearby Share server's handshake (connection
 * request + UKEY2) but pauses before the Connection Response until the
 * app accepts or rejects through [accept]/[reject]. Only after acceptance
 * does it send ACCEPT, exchange paired-key frames, and enter payload mode
 * through [NearbyConnectionsChannel]. Rejection sends REJECT and closes.
 *
 * The session is created by ConnectionsServiceImpl for every socket the
 * Connections-mode server accepts; it reports to the service through
 * [Listener], which forwards to the app's lifecycle/payload callbacks.
 */
class NearbyConnectionsServerSession(
    private val context: Context,
    private val socket: Socket,
    private val listener: Listener
) {
    interface Listener {
        fun onConnectionInitiated(endpointId: String, remoteName: String, pinCode: String, endpointInfo: ByteArray?)
        fun onConnectionResult(endpointId: String, statusCode: Int)
        fun onPayloadReceived(endpointId: String, payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?)
        fun onPayloadTransferUpdate(endpointId: String, payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long)
        fun onDisconnected(endpointId: String)
        fun onError(endpointId: String, error: Throwable)
    }

    val endpointId: String = generateEndpointId()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private var channel: NearbyConnectionsChannel? = null
    private val acceptGate = CompletableDeferred<Boolean>()
    private val closed = AtomicBoolean(false)
    @Volatile
    var established = false
        private set

    fun start() {
        job = scope.launch { run() }
    }

    /** Called when the app accepts this incoming connection. */
    fun accept() {
        acceptGate.complete(true)
    }

    /** Called when the app rejects this incoming connection. */
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
            Log.w(NearbySharingConstants.TAG, "Error closing server session channel", e)
        }
        try {
            socket.close()
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "Error closing server session socket", e)
        }
        job?.cancel()
        scope.cancel()
    }

    private suspend fun run() {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val crypto = NearbyCrypto(isServer = true)

            // 1. Connection Request (plain).
            val connReqBytes = NearbySharingServer.readLengthPrefixed(input)
            val connReqFrame = OfflineFrame.ADAPTER.decode(connReqBytes)
            val remoteEndpointInfo = connReqFrame.v1?.connection_request?.endpoint_info?.toByteArray()
            val remoteName = if (remoteEndpointInfo != null) {
                NearbyEndpointInfo.parse(remoteEndpointInfo)?.name ?: "Nearby Sender"
            } else {
                "Nearby Sender"
            }

            // 2. UKEY2 ClientInit (plain).
            val clientInitRaw = NearbySharingServer.readLengthPrefixed(input)
            val clientInitMsg = Ukey2Message.ADAPTER.decode(clientInitRaw)
            if (clientInitMsg.message_type != Ukey2Message.Type.CLIENT_INIT) {
                throw IOException("Expected UKEY2 ClientInit")
            }
            val clientInit = Ukey2ClientInit.ADAPTER.decode(clientInitMsg.message_data!!)
            val commitment = clientInit.cipher_commitments.firstOrNull {
                it.handshake_cipher == Ukey2HandshakeCipher.P256_SHA512
            }?.commitment ?: throw IOException("No supported cipher commitment in ClientInit")

            // 3. UKEY2 ServerInit (plain).
            val myGenericKey = crypto.getGenericPublicKey()
            val random32 = ByteArray(32).apply { crypto.random.nextBytes(this) }
            val serverInit = Ukey2ServerInit(
                version = 1,
                random = random32.toByteString(),
                handshake_cipher = Ukey2HandshakeCipher.P256_SHA512,
                public_key = myGenericKey.encode().toByteString()
            )
            val serverInitMsg = Ukey2Message(
                message_type = Ukey2Message.Type.SERVER_INIT,
                message_data = serverInit.encode().toByteString()
            )
            val serverInitRaw = serverInitMsg.encode()
            NearbySharingServer.writeLengthPrefixed(output, serverInitRaw)

            // 4. UKEY2 ClientFinish (plain) + commitment verification.
            val clientFinishRaw = NearbySharingServer.readLengthPrefixed(input)
            val finishDigest = MessageDigest.getInstance("SHA-512").digest(clientFinishRaw)
            if (!MessageDigest.isEqual(finishDigest, commitment.toByteArray())) {
                throw SecurityException("ClientFinish cipher commitment mismatch")
            }
            val clientFinishMsg = Ukey2Message.ADAPTER.decode(clientFinishRaw)
            val clientFinish = Ukey2ClientFinished.ADAPTER.decode(clientFinishMsg.message_data!!)
            val peerKey = GenericPublicKey.ADAPTER.decode(clientFinish.public_key!!)
            crypto.completeKeyExchange(peerKey, clientInitRaw, serverInitRaw)

            listener.onConnectionInitiated(endpointId, remoteName, crypto.pinCode, remoteEndpointInfo)

            // 5. Wait for the app's accept/reject decision.
            val accepted = acceptGate.await()
            if (!accepted) {
                val connResp = OfflineFrame(
                    version = OfflineFrame.Version.V1,
                    v1 = V1Frame(
                        type = V1Frame.FrameType.CONNECTION_RESPONSE,
                        connection_response = ConnectionResponseFrame(
                            response = ConnectionResponseFrame.ResponseStatus.REJECT,
                            status = 0,
                            os_info = OsInfo(type = OsInfo.OsType.ANDROID)
                        )
                    )
                )
                NearbySharingServer.writeLengthPrefixed(output, connResp.encode())
                close()
                return
            }

            // 6. ACCEPT + paired-key exchange (encrypted), mirroring the Share server.
            val connResp = OfflineFrame(
                version = OfflineFrame.Version.V1,
                v1 = V1Frame(
                    type = V1Frame.FrameType.CONNECTION_RESPONSE,
                    connection_response = ConnectionResponseFrame(
                        response = ConnectionResponseFrame.ResponseStatus.ACCEPT,
                        status = 0,
                        os_info = OsInfo(type = OsInfo.OsType.ANDROID)
                    )
                )
            )
            NearbySharingServer.writeLengthPrefixed(output, connResp.encode())
            sendPairedKeyFrames(output, crypto)
            // The requester sends its paired-key frames the same way: two
            // frames per key frame (data + EOF), four frames total.
            repeat(4) {
                val enc = NearbySharingServer.readLengthPrefixed(input)
                crypto.decryptSecureMessage(enc)
            }

            // 7. Payload mode.
            val receiveDir = File(context.cacheDir, "nearby_connections")
            val ch = NearbyConnectionsChannel(socket, crypto, receiveDir, object : NearbyConnectionsChannel.PayloadListener {
                override fun onPayloadReceived(payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?) {
                    listener.onPayloadReceived(endpointId, payloadId, type, bytes, pfd)
                }

                override fun onPayloadTransferUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long) {
                    listener.onPayloadTransferUpdate(endpointId, payloadId, status, bytesTransferred, totalBytes)
                }

                override fun onDisconnected() {
                    if (!closed.get()) listener.onDisconnected(endpointId)
                }

                override fun onError(error: Throwable) {
                    if (!closed.get()) listener.onError(endpointId, error)
                }
            })
            channel = ch
            established = true
            listener.onConnectionResult(endpointId, ConnectionsStatusCodes.STATUS_OK)
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
                listener.onError(endpointId, e)
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

    private fun generateEndpointId(): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder(4)
        for (i in 0 until 4) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }
}
