/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.android.gms.nearby.sharing.ConnectionResponseFrame as SharingConnectionResponseFrame
import com.google.android.gms.nearby.sharing.FileMetadata
import com.google.android.gms.nearby.sharing.Frame
import com.google.android.gms.nearby.sharing.IntroductionFrame
import com.google.android.gms.nearby.sharing.PairedKeyEncryptionFrame as SharingPairedKeyEncryptionFrame
import com.google.android.gms.nearby.sharing.PairedKeyResultFrame as SharingPairedKeyResultFrame
import com.google.android.gms.nearby.sharing.V1Frame as SharingV1Frame
import com.google.location.nearby.connections.proto.ConnectionResponseFrame
import com.google.location.nearby.connections.proto.DisconnectionFrame
import com.google.location.nearby.connections.proto.KeepAliveFrame
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom

class NearbySharingServer(
    private val context: Context,
    private val deviceName: String,
    private val deviceType: NearbyDeviceType = NearbyDeviceType.PHONE,
    private val listener: Listener
) {
    interface Listener {
        fun onServerStarted(port: Int)
        fun onConnectionInitiated(remoteDeviceName: String, pinCode: String)
        fun onTransferStarted(files: List<FileMetadata>)
        fun onTransferProgress(bytesTransferred: Long, totalBytes: Long)
        fun onTransferCompleted(savedFiles: List<File>)
        fun onError(error: Throwable)
    }

    private var serverSocket: ServerSocket? = null
    private var advertiser: NearbySharingAdvertiser? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var acceptJob: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        try {
            val ss = ServerSocket(0)
            serverSocket = ss
            val port = ss.localPort
            Log.d(NearbySharingConstants.TAG, "NearbySharingServer listening on port $port")

            advertiser = NearbySharingAdvertiser(context, deviceName, deviceType).apply {
                startAdvertising(port, onStarted = {
                    listener.onServerStarted(port)
                }, onError = { err ->
                    listener.onError(IOException("mDNS advertising error: $err"))
                })
            }

            acceptJob = scope.launch {
                while (isRunning) {
                    try {
                        val clientSocket = ss.accept()
                        handleClient(clientSocket)
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(NearbySharingConstants.TAG, "Server accept error", e)
                            listener.onError(e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(NearbySharingConstants.TAG, "Failed to start server", e)
            listener.onError(e)
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            socket.use { s ->
                val input = s.getInputStream()
                val output = s.getOutputStream()
                val crypto = NearbyCrypto(isServer = true)

                try {
                    // 1. Receive Connection Request OfflineFrame
                    val connReqBytes = readLengthPrefixed(input)
                    val connReqFrame = OfflineFrame.ADAPTER.decode(connReqBytes)
                    val remoteEndpointInfo = connReqFrame.v1?.connection_request?.endpoint_info?.toByteArray()
                    val remoteName = if (remoteEndpointInfo != null) {
                        NearbyEndpointInfo.parse(remoteEndpointInfo)?.name ?: "Nearby Sender"
                    } else "Nearby Sender"

                    // 2. Receive UKEY2 ClientInit
                    val clientInitRaw = readLengthPrefixed(input)
                    val clientInitMsg = Ukey2Message.ADAPTER.decode(clientInitRaw)
                    if (clientInitMsg.message_type != Ukey2Message.Type.CLIENT_INIT) {
                        throw IOException("Expected UKEY2 ClientInit")
                    }
                    val clientInit = Ukey2ClientInit.ADAPTER.decode(clientInitMsg.message_data!!)
                    val commitment = clientInit.cipher_commitments.firstOrNull {
                        it.handshake_cipher == Ukey2HandshakeCipher.P256_SHA512
                    }?.commitment ?: throw IOException("No supported cipher commitment in ClientInit")

                    // 3. Send UKEY2 ServerInit
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
                    writeLengthPrefixed(output, serverInitRaw)

                    // 4. Receive UKEY2 ClientFinish
                    val clientFinishRaw = readLengthPrefixed(input)
                    val sha512 = MessageDigest.getInstance("SHA-512")
                    val finishDigest = sha512.digest(clientFinishRaw)
                    if (!MessageDigest.isEqual(finishDigest, commitment.toByteArray())) {
                        throw SecurityException("ClientFinish cipher commitment mismatch")
                    }
                    val clientFinishMsg = Ukey2Message.ADAPTER.decode(clientFinishRaw)
                    val clientFinish = Ukey2ClientFinished.ADAPTER.decode(clientFinishMsg.message_data!!)
                    val peerKey = GenericPublicKey.ADAPTER.decode(clientFinish.public_key!!)
                    crypto.completeKeyExchange(peerKey, clientInitRaw, serverInitRaw)

                    listener.onConnectionInitiated(remoteName, crypto.pinCode)

                    // 5. Send Connection Response ACCEPT
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
                    writeLengthPrefixed(output, connResp.encode())

                    // 6. Encrypted session: exchange Paired Key Encryption
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

                    // 7. Send Paired Key Result
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

                    // 8. Receive Introduction Frame (contains file/text list)
                    var introFrame: IntroductionFrame? = null
                    val payloadBuffers = mutableMapOf<Long, MutableList<ByteArray>>()

                    while (introFrame == null) {
                        val encryptedBytes = readLengthPrefixed(input)
                        val offline = OfflineFrame.ADAPTER.decode(crypto.decryptSecureMessage(encryptedBytes))
                        val pt = offline.v1?.payload_transfer
                        if (pt != null && pt.payload_header?.type == PayloadHeader.PayloadType.BYTES) {
                            val id = pt.payload_header.id ?: 0L
                            val chunk = pt.payload_chunk
                            if (chunk != null) {
                                val list = payloadBuffers.getOrPut(id) { mutableListOf() }
                                if (chunk.body != null && chunk.body.size > 0) {
                                    list.add(chunk.body.toByteArray())
                                }
                                if ((chunk.flags ?: 0) and 1 == 1) { // LAST_CHUNK
                                    val totalBytes = list.fold(0) { acc, arr -> acc + arr.size }
                                    val fullData = ByteArray(totalBytes)
                                    var offset = 0
                                    for (arr in list) {
                                        System.arraycopy(arr, 0, fullData, offset, arr.size)
                                        offset += arr.size
                                    }
                                    payloadBuffers.remove(id)
                                    val sharingFrame = Frame.ADAPTER.decode(fullData)
                                    if (sharingFrame.v1?.type == SharingV1Frame.FrameType.INTRODUCTION) {
                                        introFrame = sharingFrame.v1.introduction
                                    }
                                }
                            }
                        }
                    }

                    val filesToReceive = introFrame.file_metadata
                    listener.onTransferStarted(filesToReceive)

                    // 9. Send Accept Response Frame
                    val acceptResponse = Frame(
                        version = Frame.Version.V1,
                        v1 = SharingV1Frame(
                            type = SharingV1Frame.FrameType.RESPONSE,
                            connection_response = SharingConnectionResponseFrame(
                                status = SharingConnectionResponseFrame.Status.ACCEPT
                            )
                        )
                    )
                    sendEncryptedSharingFrame(output, crypto, acceptResponse)

                    // 10. Receive Payload Chunks for Files
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        ?: context.cacheDir
                    if (!downloadDir.exists()) downloadDir.mkdirs()

                    val targetFiles = mutableListOf<File>()
                    val fileStreams = mutableMapOf<Long, FileOutputStream>()
                    val fileRemaining = mutableMapOf<Long, Long>()
                    var totalExpectedBytes = filesToReceive.fold(0L) { acc, fm -> acc + (fm.size ?: 0L) }
                    var totalReceivedBytes = 0L

                    for (fm in filesToReceive) {
                        val name = fm.name ?: "received_file_${System.currentTimeMillis()}"
                        val targetFile = File(downloadDir, name)
                        targetFiles.add(targetFile)
                        val id = fm.payload_id ?: 0L
                        fileStreams[id] = FileOutputStream(targetFile)
                        fileRemaining[id] = fm.size ?: 0L
                    }

                    var allComplete = targetFiles.isEmpty()
                    while (!allComplete) {
                        val encryptedBytes = readLengthPrefixed(input)
                        val offline = OfflineFrame.ADAPTER.decode(crypto.decryptSecureMessage(encryptedBytes))
                        val pt = offline.v1?.payload_transfer
                        if (pt != null) {
                            val id = pt.payload_header?.id ?: 0L
                            val chunk = pt.payload_chunk
                            val fos = fileStreams[id]
                            if (fos != null && chunk != null) {
                                if (chunk.body != null && chunk.body.size > 0) {
                                    val b = chunk.body.toByteArray()
                                    fos.write(b)
                                    totalReceivedBytes += b.size
                                    listener.onTransferProgress(totalReceivedBytes, totalExpectedBytes)
                                }
                                if ((chunk.flags ?: 0) and 1 == 1) { // LAST_CHUNK
                                    fos.flush()
                                    fos.close()
                                    fileStreams.remove(id)
                                    if (fileStreams.isEmpty()) {
                                        allComplete = true
                                    }
                                }
                            }
                        } else if (offline.v1?.type == V1Frame.FrameType.KEEP_ALIVE) {
                            val ack = OfflineFrame(
                                version = OfflineFrame.Version.V1,
                                v1 = V1Frame(
                                    type = V1Frame.FrameType.KEEP_ALIVE,
                                    keep_alive = KeepAliveFrame(ack = true)
                                )
                            )
                            val ackEnc = crypto.encryptOfflineFrame(ack.encode())
                            writeLengthPrefixed(output, ackEnc)
                        } else if (offline.v1?.type == V1Frame.FrameType.DISCONNECTION) {
                            break
                        }
                    }

                    // Send clean disconnection
                    val disc = OfflineFrame(
                        version = OfflineFrame.Version.V1,
                        v1 = V1Frame(
                            type = V1Frame.FrameType.DISCONNECTION,
                            disconnection = DisconnectionFrame()
                        )
                    )
                    writeLengthPrefixed(output, crypto.encryptOfflineFrame(disc.encode()))
                    listener.onTransferCompleted(targetFiles)

                } catch (e: Exception) {
                    Log.e(NearbySharingConstants.TAG, "Error handling nearby client", e)
                    listener.onError(e)
                }
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
        val enc1 = crypto.encryptOfflineFrame(offlineFrame.encode())
        writeLengthPrefixed(output, enc1)

        val lastChunk = transfer.copy(
            payload_chunk = PayloadChunk(
                offset = frameBytes.size.toLong(),
                flags = 1,
                body = ByteString.EMPTY
            )
        )
        val offlineLast = offlineFrame.copy(v1 = offlineFrame.v1?.copy(payload_transfer = lastChunk))
        val enc2 = crypto.encryptOfflineFrame(offlineLast.encode())
        writeLengthPrefixed(output, enc2)
    }

    fun stop() {
        isRunning = false
        acceptJob?.cancel()
        advertiser?.stopAdvertising()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(NearbySharingConstants.TAG, "Error closing server socket", e)
        }
    }

    companion object {
        fun readFully(input: InputStream, buffer: ByteArray) {
            var read = 0
            while (read < buffer.size) {
                val count = input.read(buffer, read, buffer.size - read)
                if (count == -1) throw EOFException("Unexpected EOF while reading frame")
                read += count
            }
        }

        fun readLengthPrefixed(input: InputStream): ByteArray {
            val lenBuf = ByteArray(4)
            readFully(input, lenBuf)
            val len = ((lenBuf[0].toInt() and 0xFF) shl 24) or
                    ((lenBuf[1].toInt() and 0xFF) shl 16) or
                    ((lenBuf[2].toInt() and 0xFF) shl 8) or
                    (lenBuf[3].toInt() and 0xFF)
            if (len <= 0 || len > NearbySharingConstants.MAX_FRAME_LENGTH) {
                throw IOException("Invalid frame length: $len")
            }
            val buf = ByteArray(len)
            readFully(input, buf)
            return buf
        }

        fun writeLengthPrefixed(output: OutputStream, bytes: ByteArray) {
            val len = bytes.size
            val header = byteArrayOf(
                (len ushr 24).toByte(),
                (len ushr 16).toByte(),
                (len ushr 8).toByte(),
                len.toByte()
            )
            output.write(header)
            output.write(bytes)
            output.flush()
        }
    }
}
