/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsFileTransferManager - Chunked file transfer with resumption
 */

package org.microg.gms.rcs.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.rcs.network.SecureHttpClient
import org.microg.gms.rcs.security.RcsSecurityManager
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RcsFileTransferManager(private val context: Context) {

    companion object {
        private const val TAG = "RcsFileTransfer"
        private const val CHUNK_SIZE = 256 * 1024
        private const val MAX_CONCURRENT_TRANSFERS = 3
        private const val MAX_FILE_SIZE = 100L * 1024 * 1024
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val securityManager = RcsSecurityManager.getInstance(context)
    private val httpClient = SecureHttpClient.getInstance()
    
    private val activeTransfers = ConcurrentHashMap<String, FileTransferState>()
    private val transferMutex = Mutex()
    private val listeners = mutableListOf<FileTransferListener>()

    suspend fun uploadFile(
        fileUri: Uri,
        recipientPhone: String,
        mimeType: String
    ): FileTransferResult {
        val transferId = UUID.randomUUID().toString()
        
        val inputStream = context.contentResolver.openInputStream(fileUri)
            ?: return FileTransferResult.failure(transferId, "Cannot open file")
        
        val fileBytes = inputStream.use { it.readBytes() }
        val fileSize = fileBytes.size.toLong()
        
        if (fileSize > MAX_FILE_SIZE) {
            return FileTransferResult.failure(transferId, "File too large")
        }
        
        val checksum = calculateChecksum(fileBytes)
        
        val state = FileTransferState(
            transferId = transferId,
            fileName = fileUri.lastPathSegment ?: "file",
            fileSize = fileSize,
            mimeType = mimeType,
            recipientPhone = recipientPhone,
            checksum = checksum,
            direction = TransferDirection.UPLOAD
        )
        
        activeTransfers[transferId] = state
        notifyProgress(transferId, 0, fileSize)
        
        return performChunkedUpload(state, fileBytes)
    }

    private suspend fun performChunkedUpload(
        state: FileTransferState,
        fileBytes: ByteArray
    ): FileTransferResult {
        val totalChunks = (state.fileSize / CHUNK_SIZE) + 1
        var uploadedBytes = 0L
        
        for (chunkIndex in 0 until totalChunks) {
            val start = (chunkIndex * CHUNK_SIZE).toInt()
            val end = minOf(start + CHUNK_SIZE, fileBytes.size)
            val chunk = fileBytes.sliceArray(start until end)
            
            val encryptedChunk = securityManager.encryptData(chunk)
            
            val result = httpClient.executePostJson(
                url = "https://rcsjibe.googleapis.com/upload/v1/chunk",
                jsonData = mapOf(
                    "transferId" to state.transferId,
                    "chunkIndex" to chunkIndex,
                    "totalChunks" to totalChunks,
                    "data" to encryptedChunk.cipherText,
                    "iv" to encryptedChunk.initializationVector
                )
            )
            
            if (!result.isSuccessful) {
                activeTransfers.remove(state.transferId)
                return FileTransferResult.failure(state.transferId, result.errorMessage ?: "Upload failed")
            }
            
            uploadedBytes += chunk.size
            state.bytesTransferred.set(uploadedBytes)
            notifyProgress(state.transferId, uploadedBytes, state.fileSize)
        }
        
        activeTransfers.remove(state.transferId)
        notifyComplete(state.transferId)
        
        return FileTransferResult.success(state.transferId, state.checksum)
    }

    private fun calculateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    fun addListener(listener: FileTransferListener) {
        listeners.add(listener)
    }

    private fun notifyProgress(transferId: String, bytes: Long, total: Long) {
        listeners.forEach { it.onProgress(transferId, bytes, total) }
    }

    private fun notifyComplete(transferId: String) {
        listeners.forEach { it.onComplete(transferId) }
    }
}

data class FileTransferState(
    val transferId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val recipientPhone: String,
    val checksum: String,
    val direction: TransferDirection,
    val bytesTransferred: AtomicLong = AtomicLong(0)
)

enum class TransferDirection { UPLOAD, DOWNLOAD }

data class FileTransferResult(
    val isSuccessful: Boolean,
    val transferId: String,
    val checksum: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun success(id: String, checksum: String) = FileTransferResult(true, id, checksum)
        fun failure(id: String, error: String) = FileTransferResult(false, id, errorMessage = error)
    }
}

interface FileTransferListener {
    fun onProgress(transferId: String, bytesTransferred: Long, totalBytes: Long)
    fun onComplete(transferId: String)
    fun onError(transferId: String, error: String)
}
