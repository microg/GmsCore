/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsFileEncryption - File encryption for secure file transfer
 */

package org.microg.gms.rcs.transfer

import android.content.Context
import org.microg.gms.rcs.security.RcsSecurityManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RcsFileEncryption(context: Context) {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val BUFFER_SIZE = 8192
    }

    private val securityManager = RcsSecurityManager.getInstance(context)

    fun encryptFile(sourceFile: File, destFile: File): EncryptedFileResult {
        val key = ByteArray(KEY_SIZE / 8)
        val iv = ByteArray(GCM_IV_LENGTH)
        
        SecureRandom().apply {
            nextBytes(key)
            nextBytes(iv)
        }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(destFile).use { fos ->
                fos.write(iv)
                
                CipherOutputStream(fos, cipher).use { cos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        cos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        
        val keyBase64 = Base64.getEncoder().encodeToString(key)
        val checksum = calculateChecksum(destFile)
        
        return EncryptedFileResult(
            encryptedFile = destFile,
            encryptionKey = keyBase64,
            checksum = checksum,
            originalSize = sourceFile.length(),
            encryptedSize = destFile.length()
        )
    }

    fun decryptFile(
        sourceFile: File,
        destFile: File,
        keyBase64: String
    ): Boolean {
        return try {
            val key = Base64.getDecoder().decode(keyBase64)
            
            FileInputStream(sourceFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                fis.read(iv)
                
                val cipher = Cipher.getInstance(ALGORITHM)
                val secretKey = SecretKeySpec(key, "AES")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                
                FileOutputStream(destFile).use { fos ->
                    CipherInputStream(fis, cipher).use { cis ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (cis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    fun encryptStream(
        inputStream: InputStream,
        outputStream: OutputStream
    ): Pair<String, String> {
        val key = ByteArray(KEY_SIZE / 8)
        val iv = ByteArray(GCM_IV_LENGTH)
        
        SecureRandom().apply {
            nextBytes(key)
            nextBytes(iv)
        }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        
        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val keyBase64 = Base64.getEncoder().encodeToString(key)
        
        outputStream.write(iv)
        
        CipherOutputStream(outputStream, cipher).use { cos ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                cos.write(buffer, 0, bytesRead)
            }
        }
        
        return keyBase64 to ivBase64
    }

    fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        
        return Base64.getEncoder().encodeToString(digest.digest())
    }

    fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        val actualChecksum = calculateChecksum(file)
        return actualChecksum == expectedChecksum
    }
}

data class EncryptedFileResult(
    val encryptedFile: File,
    val encryptionKey: String,
    val checksum: String,
    val originalSize: Long,
    val encryptedSize: Long
) {
    fun toFileInfo(): String {
        return """
            Key: $encryptionKey
            Checksum: $checksum
            Original Size: $originalSize bytes
            Encrypted Size: $encryptedSize bytes
        """.trimIndent()
    }
}
