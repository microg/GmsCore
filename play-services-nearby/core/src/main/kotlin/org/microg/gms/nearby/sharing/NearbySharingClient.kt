/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.nearby.sharing.ConnectionResponseFrame as SharingConnectionResponseFrame
import com.google.android.gms.nearby.sharing.FileMetadata
import com.google.android.gms.nearby.sharing.Frame
import com.google.android.gms.nearby.sharing.IntroductionFrame
import com.google.android.gms.nearby.sharing.PairedKeyEncryptionFrame as SharingPairedKeyEncryptionFrame
import com.google.android.gms.nearby.sharing.PairedKeyResultFrame as SharingPairedKeyResultFrame
import com.google.android.gms.nearby.sharing.TextMetadata
import com.google.android.gms.nearby.sharing.V1Frame as SharingV1Frame
import com.google.location.nearby.connections.proto.ConnectionRequestFrame
import com.google.location.nearby.connections.proto.DisconnectionFrame
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom

class NearbySharingClient(
    private val context: Context,
    private val myDeviceName: String,
    private val listener: Listener
) {
    interface Listener {
        fun onConnectionInitiated(targetDevice: NearbyDevice, pinCode: String)
        fun onTransferAccepted()
        fun onTransferProgress(bytesTransferred: Long, totalBytes: Long)
        fun onTransferCompleted()
        fun onError(error: Throwable)
    }

    suspend fun sendFiles(
        targetDevice: NearbyDevice,
        files: List<File>,
        textToSend: String? = null
    ) = withContext(Dispatchers.IO) {
        val host = targetDevice.host ?: throw IllegalArgumentException("Target device has no host address")
        val port = targetDevice.port
        if (port <= 0) throw IllegalArgumentException("Target device has invalid port: $port")

        Socket().use { socket ->
            try {
                socket.connect(InetSocketAddress(host, port), 10000)
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val crypto = NearbyCrypto(isServer = false)

                // 1. Send Connection Request OfflineFrame
                val myEndpointInfo = NearbyEndpointInfo(myDeviceName, NearbyDeviceType.PHONE).serialize()
                val connReq = OfflineFrame(
                    version = OfflineFrame.Version.V1,
                    v1 = V1Frame(
                        type = V1Frame.FrameType.CONNECTION_REQUEST,
                        connection_request = ConnectionRequestFrame(
                            endpoint_id = "0000",
                            endpoint_name = myDeviceName,
                            endpoint_info = myEndpointInfo.toByteString()
                        )
                    )
                )
                NearbySharingServer.writeLengthPrefixed(output, connReq.encode())

                // 2. Prepare UKEY2 ClientFinish
                val myGenericKey = crypto.getGenericPublicKey()
                val clientFinish = Ukey2ClientFinished(public_key = myGenericKey.encode().toByteString())
                val clientFinishMsg = Ukey2Message(
                    message_type = Ukey2Message.Type.CLIENT_FINISH,
                    message_data = clientFinish.encode().toByteString()
                )
                val clientFinishRaw = clientFinishMsg.encode()

                val sha512 = MessageDigest.getInstance("SHA-512")
                val commitment = sha512.digest(clientFinishRaw)

                // 3. Send UKEY2 ClientInit
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

                // 4. Receive UKEY2 ServerInit
                val serverInitRaw = NearbySharingServer.readLengthPrefixed(input)
                val serverInitMsg = Ukey2Message.ADAPTER.decode(serverInitRaw)
                if (serverInitMsg.message_type != Ukey2Message.Type.SERVER_INIT) {
                    throw IOException("Expected UKEY2 ServerInit")
                }
                val serverInit = Ukey2ServerInit.ADAPTER.decode(serverInitMsg.message_data!!)
                val serverKey = GenericPublicKey.ADAPTER.decode(serverInit.public_key!!)

                // Complete key exchange
                crypto.completeKeyExchange(serverKey, clientInitRaw, serverInitRaw)

                // 5. Send UKEY2 ClientFinish
                NearbySharingServer.writeLengthPrefixed(output, clientFinishRaw)

                listener.onConnectionInitiated(targetDevice, crypto.pinCode)

                // 6. Receive Connection Response
                val connRespRaw = NearbySharingServer.readLengthPrefixed(input)
                val connResp = OfflineFrame.ADAPTER.decode(connRespRaw)
                if (connResp.v1?.connection_response?.response != com.google.location.nearby.connections.proto.ConnectionResponseFrame.ResponseStatus.ACCEPT) {
                    throw IOException("Connection rejected by remote party")
                }

                // 7. Send Paired Key Encryption
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

                // 8. Send Paired Key Result
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

                // 9. Send Introduction Frame
                val fileMetas = mutableListOf<FileMetadata>()
                val filePayloadMap = mutableMapOf<File, Long>()
                val rng = SecureRandom()

                for (f in files) {
                    val pId = rng.nextLong()
                    filePayloadMap[f] = pId
                    fileMetas.add(
                        FileMetadata(
                            name = f.name,
                            size = f.length(),
                            type = FileMetadata.Type.DOCUMENT,
                            payload_id = pId
                        )
                    )
                }

                val textMetas = mutableListOf<TextMetadata>()
                if (textToSend != null) {
                    textMetas.add(
                        TextMetadata(
                            text_title = textToSend.take(20),
                            type = TextMetadata.Type.TEXT,
                            size = textToSend.toByteArray(Charsets.UTF_8).size.toLong(),
                            payload_id = rng.nextLong()
                        )
                    )
                }

                val intro = Frame(
                    version = Frame.Version.V1,
                    v1 = SharingV1Frame(
                        type = SharingV1Frame.FrameType.INTRODUCTION,
                        introduction = IntroductionFrame(
                            file_metadata = fileMetas,
                            text_metadata = textMetas
                        )
                    )
                )
                sendEncryptedSharingFrame(output, crypto, intro)

                // 10. Wait for Accept Response Frame
                var accepted = false
                while (!accepted) {
                    val encBytes = NearbySharingServer.readLengthPrefixed(input)
                    val offline = OfflineFrame.ADAPTER.decode(crypto.decryptSecureMessage(encBytes))
                    val pt = offline.v1?.payload_transfer
                    if (pt != null && pt.payload_chunk?.body != null && pt.payload_chunk.body.size > 0) {
                        try {
                            val respFrame = Frame.ADAPTER.decode(pt.payload_chunk.body.toByteArray())
                            if (respFrame.v1?.type == SharingV1Frame.FrameType.RESPONSE) {
                                val status = respFrame.v1.connection_response?.status
                                if (status == SharingConnectionResponseFrame.Status.ACCEPT) {
                                    accepted = true
                                    listener.onTransferAccepted()
                                } else {
                                    throw IOException("Transfer rejected by receiver: $status")
                                }
                            }
                        } catch (e: Exception) {
                            // Non-frame payload, continue
                        }
                    }
                }

                // 11. Send file payloads in chunks
                val totalExpectedBytes = files.fold(0L) { acc, f -> acc + f.length() }
                var totalSentBytes = 0L

                val chunkBuf = ByteArray(NearbySharingConstants.CHUNK_SIZE)
                for ((f, pId) in filePayloadMap) {
                    FileInputStream(f).use { fis ->
                        var offset = 0L
                        var bytesRead: Int
                        while (fis.read(chunkBuf).also { bytesRead = it } != -1) {
                            val chunkData = if (bytesRead == chunkBuf.size) chunkBuf else chunkBuf.copyOf(bytesRead)
                            val ptFrame = PayloadTransferFrame(
                                packet_type = PayloadTransferFrame.PacketType.DATA,
                                payload_header = PayloadHeader(
                                    id = pId,
                                    type = PayloadHeader.PayloadType.FILE,
                                    total_size = f.length()
                                ),
                                payload_chunk = PayloadChunk(
                                    offset = offset,
                                    flags = 0,
                                    body = chunkData.toByteString()
                                )
                            )
                            val offFrame = OfflineFrame(
                                version = OfflineFrame.Version.V1,
                                v1 = V1Frame(
                                    type = V1Frame.FrameType.PAYLOAD_TRANSFER,
                                    payload_transfer = ptFrame
                                )
                            )
                            NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(offFrame.encode()))

                            offset += bytesRead
                            totalSentBytes += bytesRead
                            listener.onTransferProgress(totalSentBytes, totalExpectedBytes)
                        }

                        // Send EOF (flags = 1)
                        val lastPt = PayloadTransferFrame(
                            packet_type = PayloadTransferFrame.PacketType.DATA,
                            payload_header = PayloadHeader(
                                id = pId,
                                type = PayloadHeader.PayloadType.FILE,
                                total_size = f.length()
                            ),
                            payload_chunk = PayloadChunk(
                                offset = offset,
                                flags = 1,
                                body = ByteString.EMPTY
                            )
                        )
                        val lastOff = OfflineFrame(
                            version = OfflineFrame.Version.V1,
                            v1 = V1Frame(
                                type = V1Frame.FrameType.PAYLOAD_TRANSFER,
                                payload_transfer = lastPt
                            )
                        )
                        NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(lastOff.encode()))
                    }
                }

                // 12. Send Disconnection
                val disc = OfflineFrame(
                    version = OfflineFrame.Version.V1,
                    v1 = V1Frame(
                        type = V1Frame.FrameType.DISCONNECTION,
                        disconnection = DisconnectionFrame()
                    )
                )
                NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(disc.encode()))
                listener.onTransferCompleted()

            } catch (e: Exception) {
                Log.e(NearbySharingConstants.TAG, "Error during sendFiles", e)
                listener.onError(e)
            }
        }
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
}
