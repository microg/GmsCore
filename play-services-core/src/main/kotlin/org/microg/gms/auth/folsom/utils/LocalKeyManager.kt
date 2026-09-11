/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.microg.gms.auth.folsom.AccountData
import org.microg.gms.auth.folsom.DomainData
import org.microg.gms.auth.folsom.DomainStatus
import org.microg.gms.auth.folsom.FolsomKeyStore
import org.microg.gms.auth.folsom.GetSecurityDomainResponse
import org.microg.gms.auth.folsom.KeyPair
import org.microg.gms.auth.folsom.Keys
import org.microg.gms.auth.folsom.ListSecurityDomainMembersResponse
import org.microg.gms.auth.folsom.SecurityDomainMemberResponse
import org.microg.gms.auth.folsom.computeMemberName
import org.microg.gms.auth.folsom.loadSecurityDomainMembers
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Random

private const val TAG = "LocalKeyManager"
private const val STORE_DIR = "folsom"
private const val STORE_FILE = "FolsomKeyStore.pb"
private const val TINK_KEYSET_PREF_NAME = "folsom_tink_keyset"
private const val TINK_KEYSET_KEY = "folsom_aead_keyset"
private const val TINK_MASTER_KEY_URI = "android-keystore://folsom_tink_master_key"
private const val MAX_KEYS_PER_DOMAIN = 100
private const val TIMESTAMP_JITTER_RANGE = 60000L

private val SENSITIVE_DOMAINS = setOf(
    "users/me/securitydomains/on_device_location_history",
    "users/me/securitydomains/passwords",
    "users/me/securitydomains/gpm_passkeys",
    "users/me/securitydomains/chrome_signin",
    "on_device_location_history",
    "passwords",
    "gpm_passkeys",
    "chrome_signin"
)

private object StoredStatus {
    const val UNKNOWN = 0
    const val NOT_RECOVERABLE = 1
    const val PENDING_RECOVERY = 2
    const val RECOVERABLE = 3
}

class TinkEncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

class LocalKeyManager(private val context: Context) {

    private val storeFile: File by lazy {
        File(context.filesDir, STORE_DIR).apply { mkdirs() }.resolve(STORE_FILE)
    }

    private val random = Random()

    @Volatile
    private var cachedStore: FolsomKeyStore? = null
    private val storeLock = Any()

    @Volatile
    private var lastFileModifiedTime: Long = 0L

    private val aead: Aead by lazy {
        runCatching {
            AeadConfig.register()
            AndroidKeysetManager.Builder()
                .withSharedPref(context, TINK_KEYSET_KEY, TINK_KEYSET_PREF_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(TINK_MASTER_KEY_URI)
                .build()
                .keysetHandle.getPrimitive(Aead::class.java)
        }.getOrElse { Log.e(TAG, "Failed to initialize AEAD", it); throw TinkEncryptionException("Failed to initialize", it) }
    }

    private val fallbackAead: Aead? by lazy {
        runCatching {
            AeadConfig.register()
            val keysetFile = File(context.filesDir, "$STORE_DIR/tink_keyset.json")
            (if (keysetFile.exists()) CleartextKeysetHandle.read(JsonKeysetReader.withFile(keysetFile))
            else KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM")).also { h ->
                keysetFile.parentFile?.mkdirs()
                CleartextKeysetHandle.write(h, JsonKeysetWriter.withFile(keysetFile))
            }).getPrimitive(Aead::class.java)
        }.onFailure { Log.e(TAG, "Failed to initialize fallback AEAD", it) }.getOrNull()
    }

    private fun obtainAead(): Aead = runCatching { aead }
        .getOrElse {
            Log.w(TAG, "Primary AEAD unavailable, using fallback", it)
            fallbackAead ?: throw TinkEncryptionException("No AEAD available")
        }

    private fun readStore(): FolsomKeyStore = synchronized(storeLock) {
        val currentModifiedTime = if (storeFile.exists()) storeFile.lastModified() else 0L
        if (cachedStore != null && currentModifiedTime == lastFileModifiedTime) return@synchronized cachedStore!!
        val store = runCatching { FolsomKeyStore.ADAPTER.decode(storeFile.readBytes()) }
            .getOrElse { Log.e(TAG, "Failed to read store file, creating new one", it); FolsomKeyStore() }
            .takeIf { storeFile.exists() } ?: FolsomKeyStore()
        cachedStore = store; lastFileModifiedTime = currentModifiedTime; store
    }

    private fun writeStore(store: FolsomKeyStore) = synchronized(storeLock) {
        runCatching { storeFile.parentFile?.mkdirs(); storeFile.writeBytes(store.encode()); cachedStore = store }
            .onFailure { Log.e(TAG, "Failed to write store file", it) }
            .getOrElse { throw IOException("Failed to write store", it) }
    }

    private inline fun updateStore(transform: (FolsomKeyStore) -> FolsomKeyStore) {
        synchronized(storeLock) { writeStore(transform(readStore())) }
    }

    private fun FolsomKeyStore.findAccount(obfuscatedId: String): AccountData? = accounts.find { it.key == obfuscatedId }?.value_
    private fun AccountData.findDomain(domainId: String): DomainData? = domains.find { it.key == domainId }?.value_

    private fun <T> updateEntry(
        list: List<T>,
        keySelector: (T) -> String?,
        key: String,
        newEntry: T,
    ): List<T> {
        val mutable = list.toMutableList()
        val idx = mutable.indexOfFirst { keySelector(it) == key }
        return if (idx >= 0) {
            mutable[idx] = newEntry; mutable
        } else {
            mutable + newEntry
        }
    }

    private fun FolsomKeyStore.updateAccount(obfuscatedId: String, accountData: AccountData): FolsomKeyStore =
        copy(
            accounts = updateEntry(
                accounts, { it.key }, obfuscatedId,
                FolsomKeyStore.AccountsEntry(key = obfuscatedId, value_ = accountData)
            )
        )

    private fun AccountData.updateDomain(domainId: String, domainData: DomainData): AccountData =
        copy(
            domains = updateEntry(
                domains, { it.key }, domainId,
                AccountData.DomainsEntry(key = domainId, value_ = domainData)
            )
        )

    private fun obfuscateAccountName(accountName: String): String =
        MessageDigest.getInstance("SHA-256").digest(accountName.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun DomainStatus.convertStatus(): Int = when (this) {
        DomainStatus.UNKNOWN -> StoredStatus.UNKNOWN
        DomainStatus.NOT_RECOVERABLE -> StoredStatus.NOT_RECOVERABLE
        DomainStatus.PENDING_RECOVERY -> StoredStatus.PENDING_RECOVERY
        DomainStatus.RECOVERABLE -> StoredStatus.RECOVERABLE
        else -> StoredStatus.UNKNOWN
    }

    private fun Int.toDomainStatus(): DomainStatus = when (this) {
        StoredStatus.UNKNOWN -> DomainStatus.UNKNOWN
        StoredStatus.NOT_RECOVERABLE -> DomainStatus.NOT_RECOVERABLE
        StoredStatus.PENDING_RECOVERY -> DomainStatus.PENDING_RECOVERY
        StoredStatus.RECOVERABLE -> DomainStatus.RECOVERABLE
        else -> DomainStatus.UNKNOWN_ERROR
    }

    private fun cipher(encrypt: Boolean, data: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray = runCatching {
        obtainAead().let { if (encrypt) it.encrypt(data, aad) else it.decrypt(data, aad) }
    }.getOrElse { throw TinkEncryptionException(if (encrypt) "Encryption" else "Decryption" + " failed", it) }

    private fun encrypt(plaintext: ByteArray, aad: ByteArray = ByteArray(0)) = cipher(true, plaintext, aad)
    private fun decrypt(ciphertext: ByteArray, aad: ByteArray = ByteArray(0)) = cipher(false, ciphertext, aad)
    private fun addJitter(timestamp: Long): Long = if (timestamp == 0L) 0L else timestamp - (random.nextFloat() * TIMESTAMP_JITTER_RANGE).toLong()
    private fun isSensitiveDomain(domainId: String): Boolean = SENSITIVE_DOMAINS.any { domainId.contains(it) || it.contains(domainId) }

    fun getDomainStatus(accountName: String, domainId: String): DomainStatus {
        val obfuscatedId = obfuscateAccountName(accountName)
        val store = readStore()
        val account = store.findAccount(obfuscatedId) ?: return DomainStatus.UNKNOWN
        val domain = account.findDomain(domainId) ?: return DomainStatus.UNKNOWN
        return (domain.recoverabilityStatus ?: 0).toDomainStatus()
    }

    fun getKeysForDomain(accountName: String, domainId: String): List<Keys> {
        val obfuscatedId = obfuscateAccountName(accountName)
        val store = readStore()
        val account = store.findAccount(obfuscatedId) ?: return emptyList()
        val domain = account.findDomain(domainId) ?: return emptyList()
        return decryptKeys(domain.keys, domainId)
    }

    private fun hasValidKeys(accountName: String, domainId: String): Boolean = runCatching {
        getKeysForDomain(accountName, domainId).let { it.isNotEmpty() && it.any { (it.keyVersion ?: 0) != 0 } }
    }.getOrElse { Log.e(TAG, "Error checking hasValidKeys", it); false }

    fun setDomainStatus(accountName: String, domainId: String, status: DomainStatus) {
        val obfuscatedId = obfuscateAccountName(accountName)
        updateStore { store ->
            val account = store.findAccount(obfuscatedId) ?: AccountData()
            val domain = account.findDomain(domainId) ?: DomainData()
            val updatedDomain = domain.copy(
                recoverabilityStatus = status.convertStatus(),
                recoverabilityStatusTimestamp = addJitter(System.currentTimeMillis())
            )
            store.updateAccount(obfuscatedId, account.updateDomain(domainId, updatedDomain))
        }
    }

    fun updateLastFetchTimestamp(accountName: String, domainId: String, timestamp: Long) {
        val obfuscatedId = obfuscateAccountName(accountName)
        updateStore { store ->
            val account = store.findAccount(obfuscatedId) ?: AccountData()
            val domain = account.findDomain(domainId) ?: DomainData()
            store.updateAccount(obfuscatedId, account.updateDomain(domainId, domain.copy(lastFetchTimestamp = addJitter(timestamp))))
        }
    }

    fun clearDomainKeys(accountName: String, domainId: String) {
        val obfuscatedId = obfuscateAccountName(accountName)
        updateStore { store ->
            val account = store.findAccount(obfuscatedId) ?: return@updateStore store
            val domain = account.findDomain(domainId) ?: DomainData()
            store.updateAccount(obfuscatedId, account.updateDomain(domainId, domain.copy(keys = emptyList())))
        }
    }

    fun saveKeysForDomain(accountName: String, domainId: String, keys: List<Keys>, callerPackage: String? = null) {
        val obfuscatedId = obfuscateAccountName(accountName)
        val keysToSave = keys.takeLast(MAX_KEYS_PER_DOMAIN).also {
            if (keys.size > MAX_KEYS_PER_DOMAIN) Log.w(TAG, "Truncating keys from ${keys.size} to $MAX_KEYS_PER_DOMAIN")
        }
        Log.d(TAG, "saveKeysForDomain: domainId=$domainId, keyCount=${keysToSave.size}, caller=$callerPackage")
        updateStore { store ->
            val account = store.findAccount(obfuscatedId) ?: AccountData()
            val domain = account.findDomain(domainId) ?: DomainData()
            store.updateAccount(
                obfuscatedId, account.updateDomain(
                    domainId, domain.copy(
                        keys = encryptKeysIfNeeded(domainId, keysToSave),
                        lastModifiedTimestamp = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    private fun decryptKeys(encryptedKeys: List<Keys>, domainId: String): List<Keys> {
        if (encryptedKeys.isEmpty()) return emptyList()
        val aad = domainId.toByteArray()
        return encryptedKeys.map { key ->
            key.encryptedKeyMaterial?.takeIf { it.size > 0 }?.let { em ->
                runCatching {
                    key.copy(keyMaterial = decrypt(em.toByteArray(), aad).toByteString(), encryptedKeyMaterial = null)
                }.getOrElse { throw TinkEncryptionException("Failed to decrypt key", it) }
            } ?: key
        }
    }

    private fun encryptKeysIfNeeded(domainId: String, keys: List<Keys>): List<Keys> {
        if (keys.isEmpty() || !isSensitiveDomain(domainId)) return keys
        val aad = domainId.toByteArray()
        return keys.map { key ->
            (key.keyMaterial ?: key.keyMetadata)?.takeIf { it.size > 0 }?.let { km ->
                key.copy(keyMaterial = null, encryptedKeyMaterial = encrypt(km.toByteArray(), aad).toByteString())
            } ?: key
        }
    }

    private fun getAccountKeyPairs(accountName: String): List<KeyPair> {
        val obfuscatedId = obfuscateAccountName(accountName)
        return readStore().findAccount(obfuscatedId)?.accountKeyPairs ?: emptyList()
    }

    private fun getPhysicalDeviceKeyPair(accountName: String): KeyPair? {
        val keyPairs = getAccountKeyPairs(accountName)
        return keyPairs.find { it.keyPairType == 3 } ?: keyPairs.find { it.keyPairType == 1 }
    }

    private fun buildKeyPairBytes(privateKey: ByteArray, publicKey: ByteArray): ByteArray = ByteArray(97).apply {
        System.arraycopy(privateKey, 0, this, 0, minOf(privateKey.size, 32))
        System.arraycopy(publicKey, 0, this, 32, minOf(publicKey.size, 65))
    }

    fun computeDomainStatus(
        accountName: String,
        domainId: String,
        serverState: GetSecurityDomainResponse?,
        membersResponse: ListSecurityDomainMembersResponse?
    ): DomainStatus {
        if (serverState == null) {
            val hasLocalKeys = hasValidKeys(accountName, domainId)
            return if (hasLocalKeys) DomainStatus.NOT_RECOVERABLE else DomainStatus.UNKNOWN
        }
        val members = membersResponse?.members
        if (members.isNullOrEmpty()) {
            return DomainStatus.NO_KEYS
        }
        val hasKeys = members.any { member ->
            member.securityDomains.any { sd ->
                sd.memberKeys.isNotEmpty() || sd.trustedVaultKeys.isNotEmpty()
            }
        }
        if (!hasKeys) {
            return DomainStatus.NO_KEYS
        }
        if (hasValidKeys(accountName, domainId)) {
            return DomainStatus.RECOVERABLE
        }
        val localKeyPair = getPhysicalDeviceKeyPair(accountName)
        if (localKeyPair != null) {
            val localPublicKey = localKeyPair.publicKey?.toByteArray()
            if (localPublicKey != null) {
                val localMemberName = computeMemberName(localPublicKey)
                val isDeviceRegistered = members.any { it.name == localMemberName }
                if (isDeviceRegistered) {
                    return DomainStatus.RECOVERABLE
                }
            }
        }
        return DomainStatus.PENDING_RECOVERY
    }

    suspend fun getLocalKeysOrSync(
        context: Context,
        accountName: String,
        domainId: String,
        sessionId: String
    ): List<Keys> = runCatching {
        getKeysForDomain(accountName, domainId).takeIf { it.isNotEmpty() }
            ?: loadSecurityDomainMembers(context, accountName, sessionId, domainId)
                .members.takeIf { it.isNotEmpty() }
                ?.let { extractKeysFromMembers(it, domainId, accountName) }
                ?.also { saveKeysForDomain(accountName, domainId, it) }
            ?: emptyList()
    }.getOrDefault(emptyList())

    private fun extractKeysFromMembers(
        members: List<SecurityDomainMemberResponse>,
        domainId: String,
        accountName: String
    ): List<Keys> = getPhysicalDeviceKeyPair(accountName)
        ?.let { keyPair ->
            val pubKey = keyPair.publicKey?.toByteArray()
            val priKey = keyPair.privateKey?.toByteArray()
            if (pubKey != null && priKey != null) {
                members.find { it.name == computeMemberName(pubKey) }
                    ?.let { decryptAllKeys(it, domainId, buildKeyPairBytes(priKey, pubKey)) }
            } else null
        } ?: emptyList()

    private fun decryptAllKeys(
        member: SecurityDomainMemberResponse,
        domainId: String,
        localKeyPairBytes: ByteArray
    ): List<Keys> = member.securityDomains
        .filter { it.name?.contains(domainId) == true }
        .flatMap { domain ->
            domain.trustedVaultKeys.mapNotNull { key -> decryptKey(key.epoch, key.wrappedKey, localKeyPairBytes) } +
                    domain.memberKeys.mapNotNull { key -> decryptKey(key.keyType, key.wrappedKey, localKeyPairBytes) }
        }

    private fun decryptKey(
        version: Int?,
        wrappedKey: ByteString?,
        localKeyPairBytes: ByteArray
    ): Keys? = runCatching {
        version?.takeIf { it != 0 } ?: return@runCatching null
        val wrappedKeyBytes = wrappedKey?.toByteArray() ?: return@runCatching null
        val keyMaterial = SecureBox.unwrapKey(localKeyPairBytes, wrappedKeyBytes)
        Keys(keyVersion = version, keyMaterial = ByteString.of(*keyMaterial))
    }.getOrNull()

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: LocalKeyManager? = null
        fun getInstance(context: Context): LocalKeyManager = instance ?: synchronized(this) {
            instance ?: LocalKeyManager(context.applicationContext).also { instance = it }
        }
    }
}