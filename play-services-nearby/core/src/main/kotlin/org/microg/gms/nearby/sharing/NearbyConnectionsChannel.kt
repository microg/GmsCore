/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.location.nearby.connections.proto.OfflineFrame
import com.google.location.nearby.connections.proto.PayloadTransferFrame
import com.google.location.nearby.connections.proto.PayloadTransferFrame.PayloadChunk
import com.google.location.nearby.connections.proto.PayloadTransferFrame.PayloadHeader
import com.google.location.nearby.connections.proto.V1Frame
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.EOFException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Encrypted payload transport for one established Connections session.
 * Both directions share this class: after the UKEY2 handshake (done by
 * the session classes), payloads are OfflineFrames carrying
 * PayloadTransferFrame, chunked exactly like the Nearby Share engine
 * (64 KiB data chunks, final chunk flagged LAST_CHUNK).
 *
 * - BYTES payloads are reassembled in memory (capped, see MAX_BYTES_PAYLOAD)
 *   and delivered whole.
 * - FILE/STREAM payloads are streamed to a cache file and delivered as a
 *   read-only ParcelFileDescriptor on completion. The Connections protocol
 *   carries no file name, so received files are named payload_<id>.
 * - Cancellation is signalled with a CONTROL frame carrying the payload id;
 *   the peer drops its reassembly buffers and reports CANCELED.
 *
 * All writes are serialized with [writeLock]; [receiveLoop] must be the
 * only reader. Blocking I/O: call from Dispatchers.IO only.
 */
class NearbyConnectionsChannel(
    private val socket: Socket,
    private val crypto: NearbyCrypto,
    private val receiveDir: File,
    private val listener: PayloadListener
) {
    interface PayloadListener {
        fun onPayloadReceived(payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?)
        fun onPayloadTransferUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long)
        fun onDisconnected()
        fun onError(error: Throwable)
    }

    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val writeLock = Any()
    private val cancelledIds = ConcurrentHashMap.newKeySet<Long>()

    private val byteBuffers = mutableMapOf<Long, MutableList<ByteArray>>()
    private val byteTotals = mutableMapOf<Long, Long>()
    private val fileStreams = mutableMapOf<Long, FileOutputStream>()
    private val fileTargets = mutableMapOf<Long, File>()
    private val fileReceived = mutableMapOf<Long, Long>()
    private val fileTotals = mutableMapOf<Long, Long>()
    private val fileTypes = mutableMapOf<Long, Int>()

    /** Sends a BYTES payload. Returns true when fully sent, false when cancelled. */
    fun sendBytes(payloadId: Long, bytes: ByteArray): Boolean {
        return sendChunked(payloadId, PAYLOAD_TYPE_BYTES, bytes.size.toLong(), bytes.inputStream())
    }

    /** Sends a FILE or STREAM payload from [stream]. Returns true when fully sent, false when cancelled. */
    fun sendStream(payloadId: Long, type: Int, stream: InputStream, totalSize: Long): Boolean {
        return sendChunked(payloadId, type, totalSize, stream)
    }

    private fun sendChunked(payloadId: Long, type: Int, totalSize: Long, stream: InputStream): Boolean {
        val headerType = when (type) {
            PAYLOAD_TYPE_FILE -> PayloadHeader.PayloadType.FILE
            PAYLOAD_TYPE_STREAM -> PayloadHeader.PayloadType.STREAM
            else -> PayloadHeader.PayloadType.BYTES
        }
        var offset = 0L
        val buffer = ByteArray(NearbySharingConstants.CHUNK_SIZE)
        try {
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                if (cancelledIds.contains(payloadId)) {
                    reportUpdate(payloadId, PayloadTransferUpdate.Status.CANCELED, offset, totalSize)
                    return false
                }
                val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                writeDataFrame(payloadId, headerType, totalSize, offset, 0, chunk)
                offset += read
                listener.onPayloadTransferUpdate(payloadId, PayloadTransferUpdate.Status.IN_PROGRESS, offset, totalSize)
            }
            if (cancelledIds.contains(payloadId)) {
                reportUpdate(payloadId, PayloadTransferUpdate.Status.CANCELED, offset, totalSize)
                return false
            }
            writeDataFrame(payloadId, headerType, totalSize, offset, FLAG_LAST_CHUNK, ByteArray(0))
            reportUpdate(payloadId, PayloadTransferUpdate.Status.SUCCESS, offset, totalSize)
            return true
        } catch (e: IOException) {
            reportUpdate(payloadId, PayloadTransferUpdate.Status.FAILURE, offset, totalSize)
            throw e
        }
    }

    /** Requests cancellation of [payloadId] locally and signals the peer. */
    fun cancelPayload(payloadId: Long) {
        cancelledIds.add(payloadId)
        try {
            writeControlFrame(payloadId)
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "Failed to send payload cancel for $payloadId", e)
        }
    }

    fun isCancelled(payloadId: Long): Boolean = cancelledIds.contains(payloadId)

    /**
     * Reads frames until the peer disconnects or an error occurs.
     * Returns normally on a clean DISCONNECTION frame or EOF.
     */
    fun receiveLoop() {
        try {
            while (!socket.isClosed) {
                val encrypted = try {
                    NearbySharingServer.readLengthPrefixed(input)
                } catch (e: EOFException) {
                    listener.onDisconnected()
                    return
                }
                val offline = OfflineFrame.ADAPTER.decode(crypto.decryptSecureMessage(encrypted))
                val v1 = offline.v1
                when (v1?.type) {
                    V1Frame.FrameType.DISCONNECTION -> {
                        listener.onDisconnected()
                        return
                    }
                    V1Frame.FrameType.PAYLOAD_TRANSFER -> {
                        val pt = v1.payload_transfer ?: continue
                        when (pt.packet_type) {
                            PayloadTransferFrame.PacketType.CONTROL -> handleCancelFrame(pt)
                            PayloadTransferFrame.PacketType.DATA -> handleDataFrame(pt)
                            else -> Unit
                        }
                    }
                    else -> Unit
                }
            }
            listener.onDisconnected()
        } catch (e: IOException) {
            listener.onError(e)
        } catch (e: Exception) {
            listener.onError(e)
        }
    }

    private fun handleCancelFrame(pt: PayloadTransferFrame) {
        val id = pt.payload_header?.id ?: return
        cancelledIds.add(id)
        synchronized(byteBuffers) {
            byteBuffers.remove(id)
            byteTotals.remove(id)
        }
        closeFileTarget(id, delete = true)
        listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.CANCELED, 0L, 0L)
    }

    private fun handleDataFrame(pt: PayloadTransferFrame) {
        val header = pt.payload_header ?: return
        val chunk = pt.payload_chunk ?: return
        val id = header.id ?: return
        val total = header.total_size ?: 0L
        val body = chunk.body?.toByteArray() ?: ByteArray(0)
        val isLast = (chunk.flags ?: 0) and FLAG_LAST_CHUNK == FLAG_LAST_CHUNK
        when (header.type) {
            PayloadHeader.PayloadType.BYTES -> handleBytesChunk(id, total, body, isLast)
            PayloadHeader.PayloadType.FILE, PayloadHeader.PayloadType.STREAM -> {
                val type = if (header.type == PayloadHeader.PayloadType.FILE) PAYLOAD_TYPE_FILE else PAYLOAD_TYPE_STREAM
                handleFileChunk(id, type, total, body, isLast)
            }
            else -> Unit
        }
    }

    private fun handleBytesChunk(id: Long, total: Long, body: ByteArray, isLast: Boolean) {
        if (total > MAX_BYTES_PAYLOAD) {
            listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.FAILURE, 0L, total)
            return
        }
        val full: ByteArray?
        synchronized(byteBuffers) {
            val list = byteBuffers.getOrPut(id) { mutableListOf() }
            if (body.isNotEmpty()) list.add(body)
            byteTotals[id] = total
            val received = list.fold(0L) { acc, arr -> acc + arr.size }
            listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.IN_PROGRESS, received, total)
            if (!isLast) return
            val size = list.fold(0) { acc, arr -> acc + arr.size }
            val data = ByteArray(size)
            var offset = 0
            for (arr in list) {
                System.arraycopy(arr, 0, data, offset, arr.size)
                offset += arr.size
            }
            byteBuffers.remove(id)
            byteTotals.remove(id)
            full = data
        }
        if (full != null) {
            listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.SUCCESS, full.size.toLong(), total)
            listener.onPayloadReceived(id, PAYLOAD_TYPE_BYTES, full, null)
        }
    }

    private fun handleFileChunk(id: Long, type: Int, total: Long, body: ByteArray, isLast: Boolean) {
        synchronized(fileStreams) {
            var fos = fileStreams[id]
            if (fos == null) {
                if (!receiveDir.exists()) receiveDir.mkdirs()
                val target = File(receiveDir, "payload_$id")
                fileTargets[id] = target
                fileTotals[id] = total
                fileTypes[id] = type
                fileReceived[id] = 0L
                fos = FileOutputStream(target)
                fileStreams[id] = fos
            }
            if (body.isNotEmpty()) {
                fos.write(body)
                fileReceived[id] = (fileReceived[id] ?: 0L) + body.size
            }
            val received = fileReceived[id] ?: 0L
            listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.IN_PROGRESS, received, total)
            if (!isLast) return
            fos.flush()
            fos.close()
            fileStreams.remove(id)
            val target = fileTargets.remove(id)
            val fileType = fileTypes.remove(id) ?: type
            fileReceived.remove(id)
            fileTotals.remove(id)
            if (target != null) {
                listener.onPayloadTransferUpdate(id, PayloadTransferUpdate.Status.SUCCESS, received, total)
                val pfd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
                listener.onPayloadReceived(id, fileType, null, pfd)
            }
        }
    }

    private fun closeFileTarget(id: Long, delete: Boolean) {
        synchronized(fileStreams) {
            try {
                fileStreams.remove(id)?.close()
            } catch (e: IOException) {
                Log.w(NearbySharingConstants.TAG, "Error closing partial file for $id", e)
            }
            val target = fileTargets.remove(id)
            fileTypes.remove(id)
            fileReceived.remove(id)
            fileTotals.remove(id)
            if (delete) target?.delete()
        }
    }

    private fun writeDataFrame(payloadId: Long, type: PayloadHeader.PayloadType, totalSize: Long, offset: Long, flags: Int, body: ByteArray) {
        val frame = OfflineFrame(
            version = OfflineFrame.Version.V1,
            v1 = V1Frame(
                type = V1Frame.FrameType.PAYLOAD_TRANSFER,
                payload_transfer = PayloadTransferFrame(
                    packet_type = PayloadTransferFrame.PacketType.DATA,
                    payload_header = PayloadHeader(id = payloadId, type = type, total_size = totalSize),
                    payload_chunk = PayloadChunk(offset = offset, flags = flags, body = body.toByteString())
                )
            )
        )
        synchronized(writeLock) {
            NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(frame.encode()))
        }
    }

    private fun writeControlFrame(payloadId: Long) {
        val frame = OfflineFrame(
            version = OfflineFrame.Version.V1,
            v1 = V1Frame(
                type = V1Frame.FrameType.PAYLOAD_TRANSFER,
                payload_transfer = PayloadTransferFrame(
                    packet_type = PayloadTransferFrame.PacketType.CONTROL,
                    payload_header = PayloadHeader(id = payloadId, type = PayloadHeader.PayloadType.UNKNOWN_PAYLOAD_TYPE, total_size = 0L),
                    payload_chunk = PayloadChunk(offset = 0L, flags = 0, body = ByteString.EMPTY)
                )
            )
        )
        synchronized(writeLock) {
            NearbySharingServer.writeLengthPrefixed(output, crypto.encryptOfflineFrame(frame.encode()))
        }
    }

    private fun reportUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long) {
        listener.onPayloadTransferUpdate(payloadId, status, bytesTransferred, totalBytes)
    }

    fun close() {
        try {
            socket.close()
        } catch (e: IOException) {
            Log.w(NearbySharingConstants.TAG, "Error closing connections channel socket", e)
        }
    }

    companion object {
        const val PAYLOAD_TYPE_BYTES = 1
        const val PAYLOAD_TYPE_FILE = 2
        const val PAYLOAD_TYPE_STREAM = 3
        const val FLAG_LAST_CHUNK = 1

        /** In-memory cap for reassembled BYTES payloads; larger ones fail instead of OOMing. */
        const val MAX_BYTES_PAYLOAD = 32L * 1024L * 1024L
    }
}
