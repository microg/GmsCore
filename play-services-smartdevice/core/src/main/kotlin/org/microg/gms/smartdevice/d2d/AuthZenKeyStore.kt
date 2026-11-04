/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.microg.gms.auth.cryptauth.CryptAuthKeyRecord
import org.microg.gms.auth.cryptauth.CryptAuthKeyOperations
import org.microg.gms.auth.cryptauth.CryptAuthKeyStore
import org.microg.gms.auth.cryptauth.CryptAuthKeyType
import org.microg.gms.utils.hkdfSha256
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "AuthZenKeyStore"
private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val PREFERENCES_NAME = "authzen_account_transfer"
private const val ENROLLED_PREFIX = "enrolled."
private const val PUBLIC_KEY_PRESENT_PREFIX = "publickey.present."
private const val AUTHZEN_ENROLLED_PREFIX = "authzen.enrolled."
private const val AUTHZEN_HANDLE_PREFIX = "authzen.handle."
private const val AUTHZEN_KEY_PREFIX = "authzen.key."
private const val COUNTER_PREFIX = "counter."
private const val AUTHZEN_KEY_FILE_PREFIX = "authzen_account_transfer_"
private const val PUBLIC_KEY_NAME = "PublicKey"
private const val AUTHZEN_KEY_NAME = "authzen"
private const val SOFTWARE_PUBLIC_KEY_FILE = "authzen_public_key"
private const val AUTHZEN_STORAGE_KEY_ALIAS = "microg_authzen_transfer_storage"
private const val AUTHZEN_STORAGE_CIPHER = "AES/GCM/NoPadding"
private const val SECURE_MESSAGE_IV_LENGTH = 16
private const val AUTHZEN_HANDLE_LENGTH = 32
private const val OTP_SECRET_LENGTH = 32
private const val AUTHZEN_STORAGE_BLOB_VERSION: Byte = 1
private const val SOFTWARE_KEY_BLOB_VERSION: Byte = 1
private const val LEGACY_CREATION_TIME = 0L
private const val LEGACY_EXPIRATION_TIME = Long.MAX_VALUE

class AuthZenKeyStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val cryptAuthKeyStore = CryptAuthKeyStore(applicationContext)

    fun getOrCreatePublicKey(
        attestationChallenge: ByteArray? = null,
        strongBoxBacked: Boolean = false,
        softwareBacked: Boolean = false,
        hardwareUserPresenceRequired: Boolean = false,
        userVerificationRequired: Boolean = false,
        userAuthenticationValidityDurationSeconds: Int = 0,
    ): ECPublicKey = synchronized(KEY_LOCK) {
        getPublicKey() ?: generateKeyPair(
            attestationChallenge = attestationChallenge,
            strongBoxBacked = strongBoxBacked,
            softwareBacked = softwareBacked,
            hardwareUserPresenceRequired = hardwareUserPresenceRequired,
            userVerificationRequired = userVerificationRequired,
            userAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds,
        ).public as ECPublicKey
    }

    fun hasKey(): Boolean = synchronized(KEY_LOCK) { getPublicKey() != null }

    fun publicKeyMaterial(): ByteArray = AuthZenWire.encodeGenericPublicKey(getOrCreatePublicKey())

    private fun signSecureMessage(accountIdentifier: String, body: ByteArray, metadata: ByteArray): ByteArray {
        val authZenKey = getAuthZenKey(accountIdentifier)
            ?: error("AuthZen symmetric key is unavailable for account")
        val iv = ByteArray(SECURE_MESSAGE_IV_LENGTH).also(SecureRandom()::nextBytes)
        val header = AuthZenWire.encodeSecureMessageHeader(metadata, iv, KEY_HANDLE)
        val ciphertext = AuthZenWire.encryptSecureMessageBody(authZenKey, header, iv, body)
        val headerAndCiphertext = AuthZenWire.encodeSecureMessageSignedData(header, ciphertext)
        val signature = sign(AuthZenWire.secureMessageSignatureInput(headerAndCiphertext))
        val secureMessage = AuthZenWire.encodeSecureMessage(headerAndCiphertext, signature)
        return secureMessage
    }

    fun signIdentityAssertion(
        accountIdentifier: String,
        expectedPublicKeyHash: ByteArray,
        clientData: ByteArray,
    ): ByteArray = synchronized(KEY_LOCK) {
        val currentPublicKeyHash = AuthZenWire.sha256(publicKeyMaterial())
        check(MessageDigest.isEqual(expectedPublicKeyHash, currentPublicKeyHash)) {
            "AuthZen PublicKey changed during account transfer"
        }
        val counter = nextOtpCounter(accountIdentifier)
        val assertionBody = AuthZenWire.encodeIdentityAssertion(clientData, counter)
        signSecureMessage(accountIdentifier, assertionBody, AuthZenWire.encodeIdentityAssertionMetadata())
    }

    fun createKeyProof(randomSessionId: ByteArray): ByteArray = synchronized(KEY_LOCK) {
        CryptAuthKeyOperations.createKeyProof(CryptAuthKeyType.P256, getPrivateKey(), randomSessionId)
    }

    private fun nextOtpCounter(accountIdentifier: String): Long = synchronized(KEY_LOCK) {
        val authZenKey = getAuthZenKey(accountIdentifier)
            ?: error("AuthZen symmetric key is unavailable for account")
        val otpSecret = hkdfSha256(
            authZenKey,
            AuthZenWire.sha256(GOOGLE_OFFLINE_OTP),
            THOTP,
            OTP_SECRET_LENGTH,
        )
        val counterPreference = COUNTER_PREFIX + AuthZenWire.base64Url(AuthZenWire.sha256(otpSecret))
        val current = if (preferences.contains(counterPreference)) {
            preferences.getLong(counterPreference, 0L)
        } else {
            ByteBuffer.wrap(
                hkdfSha256(
                    otpSecret,
                    AuthZenWire.sha256(GOOGLE_OFFLINE_OTP),
                    THOTP_COUNTER,
                    OTP_SECRET_LENGTH,
                ),
            ).order(ByteOrder.LITTLE_ENDIAN).long and Long.MAX_VALUE
        }
        val next = if (current == Long.MAX_VALUE) 0L else current + 1L
        check(preferences.edit().putLong(counterPreference, next).commit()) {
            "Unable to persist AuthZen OTP counter"
        }
        current
    }

    fun isEnrolled(accountIdentifier: String): Boolean = synchronized(KEY_LOCK) {
        getPublicKeyRecord(accountIdentifier)?.active
            ?: preferences.getBoolean(ENROLLED_PREFIX + accountHash(accountIdentifier), false)
    }

    fun hasPublicKeyRecord(accountIdentifier: String): Boolean = synchronized(KEY_LOCK) {
        if (getPublicKeyRecord(accountIdentifier) != null) return@synchronized true
        val accountHash = accountHash(accountIdentifier)
        val present = preferences.getBoolean(
            PUBLIC_KEY_PRESENT_PREFIX + accountHash,
            preferences.getBoolean(ENROLLED_PREFIX + accountHash, false),
        )
        present && hasKey()
    }

    fun setPublicKeyState(accountIdentifier: String, active: Boolean) = synchronized(KEY_LOCK) {
        val record = getPublicKeyRecord(accountIdentifier)
        if (record != null) {
            cryptAuthKeyStore.setActive(accountIdentifier, PUBLIC_KEY_NAME, record.handle, active)
            return@synchronized
        }
        val accountHash = accountHash(accountIdentifier)
        check(
            preferences.edit()
                .putBoolean(PUBLIC_KEY_PRESENT_PREFIX + accountHash, true)
                .putBoolean(ENROLLED_PREFIX + accountHash, active)
                .commit(),
        ) { "Unable to persist AuthZen PublicKey state" }
    }

    fun storePublicKey(
        accountIdentifier: String,
        active: Boolean,
        creationTime: Long,
        expirationTime: Long,
    ) {
        cryptAuthKeyStore.put(publicKeyRecord(accountIdentifier, active, creationTime, expirationTime))
    }

    fun publicKeyRecord(
        accountIdentifier: String,
        active: Boolean,
        creationTime: Long,
        expirationTime: Long,
    ): CryptAuthKeyRecord = synchronized(KEY_LOCK) {
        val publicKey = getPublicKey() ?: error("AuthZen public key is unavailable")
        CryptAuthKeyRecord(
            account = accountIdentifier,
            keyName = PUBLIC_KEY_NAME,
            handle = KEY_HANDLE,
            keyType = CryptAuthKeyType.P256,
            active = active,
            creationTime = creationTime,
            expirationTime = expirationTime,
            exportable = false,
            publicKey = publicKey.encoded,
            privateKey = getStoredPrivateKey()?.encoded,
        )
    }

    fun isAuthZenEnrolled(accountIdentifier: String): Boolean = synchronized(KEY_LOCK) {
        getAuthZenKeyRecord(accountIdentifier)?.active == true
    }

    private fun isPublicKeyReady(accountIdentifier: String): Boolean =
        hasPublicKeyRecord(accountIdentifier) && isEnrolled(accountIdentifier)

    fun isTransferReady(accountIdentifier: String): Boolean =
        isPublicKeyReady(accountIdentifier) && isAuthZenEnrolled(accountIdentifier)

    private fun getAuthZenKey(accountIdentifier: String): ByteArray? = synchronized(KEY_LOCK) {
        getAuthZenKeyRecord(accountIdentifier)?.masterKey ?: readLegacyAuthZenKey(accountIdentifier)
    }

    fun getAuthZenKeyHandles(accountIdentifier: String): List<ByteArray> = synchronized(KEY_LOCK) {
        migrateLegacyAuthZenKey(accountIdentifier)
        getAuthZenKeyRecords(accountIdentifier).map { it.handle }
    }

    fun hasAuthZenKeyRecord(accountIdentifier: String): Boolean = synchronized(KEY_LOCK) {
        getAuthZenKeyRecord(accountIdentifier) != null
    }

    fun createAuthZenKeyHandle(): ByteArray = ByteArray(AUTHZEN_HANDLE_LENGTH).also {
        SecureRandom().nextBytes(it)
        it[0] = (it[0].toInt() and 0x7f).toByte()
    }

    fun storeAuthZenKey(
        accountIdentifier: String,
        handle: ByteArray,
        keyMaterial: ByteArray,
        enrolled: Boolean,
        creationTime: Long,
        expirationTime: Long,
    ) {
        cryptAuthKeyStore.put(
            authZenKeyRecord(
                accountIdentifier,
                handle,
                keyMaterial,
                enrolled,
                creationTime,
                expirationTime,
            ),
        )
        finishAuthZenKeyStore(accountIdentifier)
    }

    fun authZenKeyRecord(
        accountIdentifier: String,
        handle: ByteArray,
        keyMaterial: ByteArray,
        enrolled: Boolean,
        creationTime: Long,
        expirationTime: Long,
    ): CryptAuthKeyRecord = synchronized(KEY_LOCK) {
        require(handle.size == AUTHZEN_HANDLE_LENGTH) { "Invalid AuthZen key handle length" }
        val keyType = authZenKeyType(keyMaterial)
        CryptAuthKeyRecord(
            account = accountIdentifier,
            keyName = AUTHZEN_KEY_NAME,
            handle = handle,
            keyType = keyType,
            active = enrolled,
            creationTime = creationTime,
            expirationTime = expirationTime,
            exportable = true,
            masterKey = keyMaterial,
        )
    }

    fun finishAuthZenKeyStore(accountIdentifier: String) = synchronized(KEY_LOCK) {
        if (!removeLegacyAuthZenKey(accountIdentifier)) {
            Log.w(TAG, "Unable to remove legacy AuthZen key after storing its replacement")
        }
    }

    fun setAuthZenEnrolled(accountIdentifier: String, handle: ByteArray, enrolled: Boolean) =
        synchronized(KEY_LOCK) {
            cryptAuthKeyStore.setActive(accountIdentifier, AUTHZEN_KEY_NAME, handle, enrolled)
        }

    fun isAuthZenKeyActive(accountIdentifier: String, handle: ByteArray): Boolean = synchronized(KEY_LOCK) {
        getAuthZenKeyRecords(accountIdentifier).any { it.active && it.handle.contentEquals(handle) }
    }

    fun deleteAuthZenKey(accountIdentifier: String, handle: ByteArray) = synchronized(KEY_LOCK) {
        check(removeLegacyAuthZenKey(accountIdentifier)) { "Unable to delete legacy AuthZen symmetric key" }
        cryptAuthKeyStore.delete(accountIdentifier, AUTHZEN_KEY_NAME, handle)
    }

    fun deletePublicKey(accountIdentifier: String) = synchronized(KEY_LOCK) {
        cryptAuthKeyStore.delete(accountIdentifier, PUBLIC_KEY_NAME, KEY_HANDLE)
        val accountHash = accountHash(accountIdentifier)
        check(
            preferences.edit()
                .remove(ENROLLED_PREFIX + accountHash)
                .remove(PUBLIC_KEY_PRESENT_PREFIX + accountHash)
                .commit(),
        ) { "Unable to reset AuthZen PublicKey enrollment state" }
    }

    private fun getPublicKey(): ECPublicKey? =
        loadKeyStore().getCertificate(KEY_ALIAS)?.publicKey as? ECPublicKey
            ?: getSoftwareKeyPair()?.public as? ECPublicKey

    private fun sign(value: ByteArray): ByteArray = synchronized(KEY_LOCK) {
        CryptAuthKeyOperations.sign(CryptAuthKeyType.P256, getPrivateKey(), value)
    }

    private fun getPrivateKey(): PrivateKey {
        getOrCreatePublicKey()
        return getStoredPrivateKey()
            ?: error("AuthZen private key is unavailable")
    }

    private fun getStoredPrivateKey(): PrivateKey? =
        loadKeyStore().getKey(KEY_ALIAS, null) as? PrivateKey ?: getSoftwareKeyPair()?.private

    private fun generateKeyPair(
        attestationChallenge: ByteArray?,
        strongBoxBacked: Boolean,
        softwareBacked: Boolean,
        hardwareUserPresenceRequired: Boolean,
        userVerificationRequired: Boolean,
        userAuthenticationValidityDurationSeconds: Int,
    ): KeyPair {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            throw IllegalStateException(
                "AuthZen P-256 AndroidKeyStore requires Android 6.0 or newer"
            )
        }
        if (softwareBacked) {
            val keyPair = CryptAuthKeyOperations.generateKeyPair(CryptAuthKeyType.P256)
            writeSoftwareKeyPair(keyPair)
            return keyPair
        }
        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE).run {
            val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(userVerificationRequired)
                .setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                builder.setAttestationChallenge(attestationChallenge?.let(AuthZenWire::sha256))
                builder.setInvalidatedByBiometricEnrollment(false)
            } else {
                check(attestationChallenge == null) {
                    "CryptAuth PublicKey attestation requires Android 7.0 or newer"
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                builder.setUserPresenceRequired(hardwareUserPresenceRequired)
            } else {
                check(!hardwareUserPresenceRequired) {
                    "Hardware user presence requires Android 9.0 or newer"
                }
            }
            if (strongBoxBacked) {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    builder.setIsStrongBoxBacked(true)
                }
            }
            initialize(builder.build())
            generateKeyPair()
        }
    }

    private fun authZenKeyFile(accountHash: String): File {
        val directory = checkNotNull(
            ContextCompat.getNoBackupFilesDir(applicationContext)
        ) {
            "Unable to access AuthZen storage directory"
        }
        return File(directory, "$AUTHZEN_KEY_FILE_PREFIX$accountHash")
    }

    private fun softwarePublicKeyFile(): File {
        val directory = checkNotNull(
            ContextCompat.getNoBackupFilesDir(applicationContext)
        ) {
            "Unable to access AuthZen storage directory"
        }
        return File(directory, SOFTWARE_PUBLIC_KEY_FILE)
    }

    private fun getSoftwareKeyPair(): KeyPair? {
        val file = softwarePublicKeyFile()
        if (!file.exists()) return null
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT < 23) {
                throw IllegalStateException(
                    "AuthZen AndroidKeyStore requires Android 6.0 or newer"
                )
            }
            val buffer = ByteBuffer.wrap(decryptStoredBlob(AtomicFile(file).readFully()))
            require(buffer.get() == SOFTWARE_KEY_BLOB_VERSION) { "Unsupported software key version" }
            val publicKeySize = buffer.int
            require(publicKeySize > 0 && publicKeySize <= buffer.remaining()) { "Invalid software public key size" }
            val publicKey = ByteArray(publicKeySize).also(buffer::get)
            val privateKey = ByteArray(buffer.remaining()).also(buffer::get)
            require(privateKey.isNotEmpty()) { "Software private key is missing" }
            CryptAuthKeyOperations.decodeKeyPair(CryptAuthKeyType.P256, publicKey, privateKey)
        }.onFailure { Log.w(TAG, "Unable to read encrypted software AuthZen key", it) }.getOrNull()
    }

    @RequiresApi(23)
    private fun writeSoftwareKeyPair(keyPair: KeyPair) {
        val publicKey = keyPair.public.encoded
        val privateKey = keyPair.private.encoded
        val encoded = ByteBuffer.allocate(1 + Int.SIZE_BYTES + publicKey.size + privateKey.size)
            .put(SOFTWARE_KEY_BLOB_VERSION)
            .putInt(publicKey.size)
            .put(publicKey)
            .put(privateKey)
            .array()
        writeEncryptedBlob(softwarePublicKeyFile(), encoded)
    }

    private fun getPublicKeyRecord(accountIdentifier: String): CryptAuthKeyRecord? =
        cryptAuthKeyStore.getActive(accountIdentifier, PUBLIC_KEY_NAME)
            ?: cryptAuthKeyStore.getAll(accountIdentifier, PUBLIC_KEY_NAME).firstOrNull()

    private fun getAuthZenKeyRecords(accountIdentifier: String): List<CryptAuthKeyRecord> =
        cryptAuthKeyStore.getAll(accountIdentifier, AUTHZEN_KEY_NAME).filter { record ->
            val valid = record.handle.size == AUTHZEN_HANDLE_LENGTH &&
                (record.keyType == CryptAuthKeyType.RAW128 || record.keyType == CryptAuthKeyType.RAW256)
            if (!valid) Log.w(TAG, "Stored AuthZen key record has an invalid type or handle")
            valid
        }

    private fun getAuthZenKeyRecord(accountIdentifier: String): CryptAuthKeyRecord? {
        val storedRecord = cryptAuthKeyStore.getActive(accountIdentifier, AUTHZEN_KEY_NAME)
            ?: getAuthZenKeyRecords(accountIdentifier).firstOrNull()
        if (storedRecord != null) {
            if (
                storedRecord.handle.size == AUTHZEN_HANDLE_LENGTH &&
                (storedRecord.keyType == CryptAuthKeyType.RAW128 || storedRecord.keyType == CryptAuthKeyType.RAW256)
            ) {
                return storedRecord
            }
            Log.w(TAG, "Stored AuthZen key record has an invalid type or handle")
            return null
        }

        return migrateLegacyAuthZenKey(accountIdentifier)
    }

    private fun migrateLegacyAuthZenKey(accountIdentifier: String): CryptAuthKeyRecord? {
        val handle = readLegacyAuthZenKeyHandle(accountIdentifier) ?: return null
        val keyMaterial = readLegacyAuthZenKey(accountIdentifier) ?: return null
        if (handle.size != AUTHZEN_HANDLE_LENGTH) {
            Log.w(TAG, "Unable to migrate AuthZen key with an invalid handle")
            return null
        }

        return runCatching {
            val record = CryptAuthKeyRecord(
                account = accountIdentifier,
                keyName = AUTHZEN_KEY_NAME,
                handle = handle,
                keyType = authZenKeyType(keyMaterial),
                active = preferences.getBoolean(
                    AUTHZEN_ENROLLED_PREFIX + accountHash(accountIdentifier),
                    false,
                ),
                creationTime = LEGACY_CREATION_TIME,
                expirationTime = LEGACY_EXPIRATION_TIME,
                exportable = true,
                masterKey = keyMaterial,
            )
            cryptAuthKeyStore.put(record)
            if (!removeLegacyAuthZenKey(accountIdentifier)) {
                Log.w(TAG, "Unable to remove legacy AuthZen key after migration")
            }
            record
        }.onFailure { Log.w(TAG, "Unable to migrate legacy AuthZen key", it) }.getOrNull()
    }

    private fun readLegacyAuthZenKey(accountIdentifier: String): ByteArray? {
        val hash = accountHash(accountIdentifier)
        val file = authZenKeyFile(hash)
        if (file.exists()) {
            return runCatching {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    decryptStoredBlob(AtomicFile(file).readFully()).also { authZenKeyType(it) }
                } else {
                    throw UnsupportedOperationException(
                        "AuthZen encrypted storage requires Android 6.0 or newer"
                    )
                }
            }.onFailure { Log.w(TAG, "Unable to read encrypted AuthZen key", it) }.getOrNull()
        }

        return runCatching {
            preferences.getString(AUTHZEN_KEY_PREFIX + hash, null)
                ?.let(AuthZenWire::decodeBase64)
                ?.also { authZenKeyType(it) }
        }.onFailure { Log.w(TAG, "Unable to read legacy AuthZen key", it) }.getOrNull()
    }

    private fun readLegacyAuthZenKeyHandle(accountIdentifier: String): ByteArray? = runCatching {
        preferences.getString(AUTHZEN_HANDLE_PREFIX + accountHash(accountIdentifier), null)
            ?.let(AuthZenWire::decodeBase64)
    }.onFailure { Log.w(TAG, "Unable to read legacy AuthZen key handle", it) }.getOrNull()

    private fun removeLegacyAuthZenKey(accountIdentifier: String): Boolean {
        val accountHash = accountHash(accountIdentifier)
        val keyFile = authZenKeyFile(accountHash)
        if (keyFile.exists() && !keyFile.delete()) return false
        return preferences.edit()
            .remove(AUTHZEN_HANDLE_PREFIX + accountHash)
            .remove(AUTHZEN_KEY_PREFIX + accountHash)
            .remove(AUTHZEN_ENROLLED_PREFIX + accountHash)
            .commit()
    }

    private fun authZenKeyType(keyMaterial: ByteArray): CryptAuthKeyType = when (keyMaterial.size) {
        CryptAuthKeyType.RAW128.symmetricKeyLength -> CryptAuthKeyType.RAW128
        CryptAuthKeyType.RAW256.symmetricKeyLength -> CryptAuthKeyType.RAW256
        else -> throw IllegalArgumentException("Invalid AuthZen key length")
    }

    @RequiresApi(23)
    private fun writeEncryptedBlob(file: File, value: ByteArray) {
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(encryptStoredBlob(value))
            atomicFile.finishWrite(output)
        } catch (e: Exception) {
            atomicFile.failWrite(output)
            throw e
        }
    }

    @RequiresApi(23)
    private fun encryptStoredBlob(value: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AUTHZEN_STORAGE_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAuthZenStorageKey())
        val ciphertext = cipher.doFinal(value)
        val iv = cipher.iv
        return ByteBuffer.allocate(2 + iv.size + ciphertext.size)
            .put(AUTHZEN_STORAGE_BLOB_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()
    }

    @RequiresApi(23)
    private fun decryptStoredBlob(blob: ByteArray): ByteArray {
        require(blob.size > 2) { "Encrypted AuthZen key is truncated" }
        val buffer = ByteBuffer.wrap(blob)
        require(buffer.get() == AUTHZEN_STORAGE_BLOB_VERSION) { "Unsupported encrypted AuthZen key version" }
        val ivLength = buffer.get().toInt() and 0xff
        require(ivLength in 12..16 && buffer.remaining() > ivLength) { "Invalid encrypted AuthZen key IV" }
        val iv = ByteArray(ivLength).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        return Cipher.getInstance(AUTHZEN_STORAGE_CIPHER).run {
            init(Cipher.DECRYPT_MODE, getOrCreateAuthZenStorageKey(), GCMParameterSpec(128, iv))
            doFinal(ciphertext)
        }
    }

    @RequiresApi(23)
    private fun getOrCreateAuthZenStorageKey(): SecretKey {
        val keyStore = loadKeyStore()
        (keyStore.getKey(AUTHZEN_STORAGE_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    AUTHZEN_STORAGE_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        return keyStore
    }

    private fun accountHash(accountIdentifier: String): String =
        Base64.encodeToString(
            AuthZenWire.sha256(accountIdentifier.toByteArray(StandardCharsets.UTF_8)),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

    companion object {
        private val KEY_LOCK = Any()
        private val GOOGLE_OFFLINE_OTP = "GoogleOfflineOTP".toByteArray(StandardCharsets.UTF_8)
        private val THOTP = "THOTP".toByteArray(StandardCharsets.UTF_8)
        private val THOTP_COUNTER = "THOTP-Counter".toByteArray(StandardCharsets.UTF_8)

        @JvmField
        val KEY_HANDLE: ByteArray = "device_key".toByteArray(StandardCharsets.UTF_8)

        @JvmField
        val KEY_ALIAS: String = Base64.encodeToString(
            KEY_HANDLE,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }
}
