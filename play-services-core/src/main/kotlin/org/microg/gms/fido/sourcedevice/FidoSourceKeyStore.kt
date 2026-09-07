/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.sourcedevice

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.microg.gms.auth.cryptauth.CryptAuthKeyRecord
import org.microg.gms.auth.cryptauth.CryptAuthKeyOperations
import org.microg.gms.auth.cryptauth.CryptAuthKeyStore
import org.microg.gms.auth.cryptauth.CryptAuthKeyType

private const val TAG = "FidoSourceKeyStore"
private const val PREFERENCES_NAME = "fido_source_direct_transfer"
private const val KEY_FILE_PREFIX = "fido_source_key."
private const val COUNTER_PREFIX = "counter."
private const val KEY_HANDLE_LENGTH = 32
private const val LEGACY_KEY_TYPE_P256: Byte = 1
private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val STORAGE_KEY_ALIAS = "microg_fido_source_transfer_storage"
private const val STORAGE_CIPHER = "AES/GCM/NoPadding"
private const val STORAGE_BLOB_VERSION: Byte = 2
private const val LEGACY_CREATION_TIME = 0L
private const val LEGACY_EXPIRATION_TIME = Long.MAX_VALUE

internal data class PendingFidoKey(
    val keyType: CryptAuthKeyType,
    val handle: ByteArray,
    val keyPair: KeyPair,
) {
    val publicKey: ECPublicKey
        get() = keyPair.public as ECPublicKey
}

private data class FidoKeyRecord(
    val keyType: CryptAuthKeyType,
    val handle: ByteArray,
    val keyPair: KeyPair,
    val active: Boolean,
    val creationTime: Long,
    val expirationTime: Long,
)

/** Account-scoped key repository used by source-device FIDO DirectTransfer. */
class FidoSourceKeyStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val sharedKeyStore = CryptAuthKeyStore(applicationContext)

    fun hasRecord(accountName: String, keyName: String): Boolean = synchronized(KEY_LOCK) {
        readKeyRecord(accountName, keyName)
        sharedKeyStore.getAll(accountName, keyName).isNotEmpty()
    }

    fun isActive(accountName: String, keyName: String): Boolean = synchronized(KEY_LOCK) {
        readActiveKeyRecord(accountName, keyName) != null
    }

    fun getHandle(accountName: String, keyName: String): ByteArray? = synchronized(KEY_LOCK) {
        readActiveKeyRecord(accountName, keyName)?.handle
    }

    fun getHandles(accountName: String, keyName: String): List<ByteArray> = synchronized(KEY_LOCK) {
        readKeyRecord(accountName, keyName)
        sharedKeyStore.getAll(accountName, keyName).map { it.handle }
    }

    fun isActive(accountName: String, keyName: String, handle: ByteArray): Boolean = synchronized(KEY_LOCK) {
        sharedKeyStore.getAll(accountName, keyName).any { it.active && it.handle.contentEquals(handle) }
    }

    internal fun createPendingKey(keyType: CryptAuthKeyType): PendingFidoKey {
        require(keyType == CryptAuthKeyType.P256) { "Unsupported FIDO account key type: $keyType" }
        val handle = ByteArray(KEY_HANDLE_LENGTH).also(SecureRandom()::nextBytes)
        val keyPair = CryptAuthKeyOperations.generateKeyPair(keyType)
        return PendingFidoKey(keyType, handle, keyPair)
    }

    internal fun keyRecord(
        accountName: String,
        keyName: String,
        pendingKey: PendingFidoKey,
        active: Boolean,
        creationTime: Long,
        expirationTime: Long,
    ): CryptAuthKeyRecord = synchronized(KEY_LOCK) {
        require(pendingKey.keyType == CryptAuthKeyType.P256) {
            "Unsupported FIDO account key type: ${pendingKey.keyType}"
        }
        CryptAuthKeyRecord(
            account = accountName,
            keyName = keyName,
            handle = pendingKey.handle,
            keyType = pendingKey.keyType,
            active = active,
            creationTime = creationTime,
            expirationTime = expirationTime,
            exportable = false,
            publicKey = pendingKey.keyPair.public.encoded,
            privateKey = pendingKey.keyPair.private.encoded,
        )
    }

    internal fun finishKeyStore(accountName: String, keyName: String) = synchronized(KEY_LOCK) {
        removeLegacyKeyFile(accountName, keyName)
    }

    fun setActive(accountName: String, keyName: String, handle: ByteArray, active: Boolean) =
        synchronized(KEY_LOCK) {
            sharedKeyStore.setActive(accountName, keyName, handle, active)
        }

    fun delete(accountName: String, keyName: String, handle: ByteArray) = synchronized(KEY_LOCK) {
        sharedKeyStore.delete(accountName, keyName, handle)
        if (sharedKeyStore.getAll(accountName, keyName).isNotEmpty()) return@synchronized

        val file = keyFile(accountName, keyName)
        AtomicFile(file).delete()
        check(!file.exists()) { "Unable to delete FIDO key" }
        check(
            preferences.edit()
                .remove(counterPreference(accountName, keyName))
                .commit(),
        ) { "Unable to delete FIDO key metadata" }
    }

    fun sign(accountName: String, value: ByteArray): ByteArray = synchronized(KEY_LOCK) {
        val record = readActiveKeyRecord(accountName, SOFTWARE_KEY_NAME)
            ?: error("FIDO account key is unavailable")
        CryptAuthKeyOperations.sign(record.keyType, record.keyPair.private, value)
    }

    internal fun createKeyProof(pendingKey: PendingFidoKey, randomSessionId: ByteArray): ByteArray =
        CryptAuthKeyOperations.createKeyProof(
            pendingKey.keyType,
            pendingKey.keyPair.private,
            randomSessionId,
        )

    fun nextCounter(accountName: String): Int = synchronized(KEY_LOCK) {
        val preference = counterPreference(accountName, SOFTWARE_KEY_NAME)
        val current = preferences.getLong(preference, 0L)
        check(current in 0 until UInt.MAX_VALUE.toLong()) {
            "FIDO signature counter is outside the CTAP range"
        }
        val next = current + 1L
        check(preferences.edit().putLong(preference, next).commit()) {
            "Unable to persist FIDO signature counter"
        }
        next.toInt()
    }

    private fun readKeyRecord(accountName: String, keyName: String): FidoKeyRecord? {
        val storedRecord = sharedKeyStore.getActive(accountName, keyName)
            ?: sharedKeyStore.getAll(accountName, keyName).firstOrNull()
        if (storedRecord != null) {
            return decodeKeyRecord(storedRecord)
        }

        val file = keyFile(accountName, keyName)
        if (!file.exists()) return null
        val legacyRecord = runCatching {
            val buffer = ByteBuffer.wrap(decryptStoredBlob(AtomicFile(file).readFully()))
            val keyType = when (buffer.get()) {
                LEGACY_KEY_TYPE_P256 -> CryptAuthKeyType.P256
                else -> error("Unsupported legacy FIDO key type")
            }
            val active = when (buffer.get().toInt()) {
                0 -> false
                1 -> true
                else -> error("Invalid FIDO key state")
            }
            val handleSize = buffer.int
            require(handleSize == KEY_HANDLE_LENGTH && handleSize <= buffer.remaining()) {
                "Invalid FIDO key handle size"
            }
            val handle = ByteArray(handleSize).also(buffer::get)
            val publicKeySize = buffer.int
            require(publicKeySize > 0 && publicKeySize <= buffer.remaining()) { "Invalid FIDO public key size" }
            val publicKey = ByteArray(publicKeySize).also(buffer::get)
            val privateKey = ByteArray(buffer.remaining()).also(buffer::get)
            require(privateKey.isNotEmpty()) { "FIDO private key is missing" }
            FidoKeyRecord(
                keyType = keyType,
                handle = handle,
                keyPair = CryptAuthKeyOperations.decodeKeyPair(keyType, publicKey, privateKey),
                active = active,
                creationTime = file.lastModified().coerceAtLeast(LEGACY_CREATION_TIME),
                expirationTime = LEGACY_EXPIRATION_TIME,
            )
        }.onFailure { Log.w(TAG, "Unable to read FIDO account key", it) }.getOrNull()
            ?: return null

        return try {
            writeKeyRecord(accountName, keyName, legacyRecord)
            removeLegacyKeyFile(accountName, keyName)
            legacyRecord
        } catch (e: Exception) {
            Log.w(TAG, "Unable to migrate FIDO account key", e)
            null
        }
    }

    private fun readActiveKeyRecord(accountName: String, keyName: String): FidoKeyRecord? {
        readKeyRecord(accountName, keyName)
        return sharedKeyStore.getActive(accountName, keyName)?.let(::decodeKeyRecord)
    }

    private fun writeKeyRecord(accountName: String, keyName: String, record: FidoKeyRecord) {
        require(record.handle.size == KEY_HANDLE_LENGTH) { "Invalid FIDO key handle size" }
        sharedKeyStore.put(
            CryptAuthKeyRecord(
                account = accountName,
                keyName = keyName,
                handle = record.handle,
                keyType = record.keyType,
                active = record.active,
                creationTime = record.creationTime,
                expirationTime = record.expirationTime,
                exportable = false,
                publicKey = record.keyPair.public.encoded,
                privateKey = record.keyPair.private.encoded,
            ),
        )
    }

    private fun decodeKeyRecord(record: CryptAuthKeyRecord): FidoKeyRecord? = runCatching {
        require(record.handle.size == KEY_HANDLE_LENGTH) { "Invalid FIDO key handle size" }
        val publicKey = requireNotNull(record.publicKey) { "FIDO public key is missing" }
        val privateKey = requireNotNull(record.privateKey) { "FIDO private key is missing" }
        FidoKeyRecord(
            keyType = record.keyType,
            handle = record.handle,
            keyPair = CryptAuthKeyOperations.decodeKeyPair(record.keyType, publicKey, privateKey),
            active = record.active,
            creationTime = record.creationTime,
            expirationTime = record.expirationTime,
        )
    }.onFailure { Log.w(TAG, "Unable to read FIDO account key", it) }.getOrNull()

    private fun removeLegacyKeyFile(accountName: String, keyName: String) {
        val file = keyFile(accountName, keyName)
        AtomicFile(file).delete()
        if (file.exists()) Log.w(TAG, "Unable to remove migrated FIDO key file")
    }

    private fun keyFile(accountName: String, keyName: String): File = File(
        ContextCompat.getNoBackupFilesDir(applicationContext),
        "$KEY_FILE_PREFIX${stableHash(accountName)}.${stableHash(keyName)}",
    )

    private fun counterPreference(accountName: String, keyName: String) =
        "$COUNTER_PREFIX${stableHash(accountName)}.${stableHash(keyName)}"

    private fun stableHash(value: String): String = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    private fun decryptStoredBlob(blob: ByteArray): ByteArray {
        require(blob.size > 2) { "Encrypted FIDO key is truncated" }
        val buffer = ByteBuffer.wrap(blob)
        require(buffer.get() == STORAGE_BLOB_VERSION) { "Unsupported encrypted FIDO key version" }
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength in 12..16 && buffer.remaining() > ivLength) { "Invalid encrypted FIDO key IV" }
        val iv = ByteArray(ivLength).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        return Cipher.getInstance(STORAGE_CIPHER).run {
            init(Cipher.DECRYPT_MODE, getStorageKey(), GCMParameterSpec(128, iv))
            doFinal(ciphertext)
        }
    }

    private fun getStorageKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        return keyStore.getKey(STORAGE_KEY_ALIAS, null) as? SecretKey
            ?: error("FIDO storage key is unavailable")
    }

    companion object {
        const val SOFTWARE_KEY_NAME = "fido:android_software_key"
        private val KEY_LOCK = Any()
    }
}
