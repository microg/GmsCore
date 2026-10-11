/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cryptauth

import android.annotation.TargetApi
import android.content.Context
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import cryptauthv2.ClientDirective
import cryptauthv2.KeyDirective
import cryptauthv2.KeyType
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Account-scoped storage excluded from Android backups. All callers serialize transactions with
 * cryptAuthStateMutex; AtomicFile alone provides no locking. No authentication tokens are stored.
 */
@TargetApi(21)
internal class CryptAuthKeyStore(context: Context, accountName: String) : CryptAuthStateStore {
    private val file: AtomicFile

    init {
        check(android.os.Build.VERSION.SDK_INT >= 21) { "CryptAuth enrollment requires no-backup storage" }
        val directory = File(context.noBackupFilesDir, "cryptauth")
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create CryptAuth storage" }
        Os.chmod(directory.path, 0x1c0) // 0700
        syncDirectory(context.noBackupFilesDir)
        // The account name is neither persisted in clear text nor used as a file name.
        file = AtomicFile(File(directory, accountName.encodeUtf8().sha256().hex()))
    }

    override fun read(): CryptAuthState = try {
        file.openRead().use { input ->
            if (input.channel.size() > CryptAuthStateCodec.MAX_BYTES) throw IOException("CryptAuth state exceeds limit")
            val bytes = input.readBytes()
            try { CryptAuthStateCodec.decode(bytes) } finally { bytes.fill(0) }
        }
    } catch (missing: FileNotFoundException) {
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) throw missing
        // Persist the instance identity before registration. Failed enrollment leaves no valid keys.
        CryptAuthState(generateAppId()).also { write(it) }
    }

    override fun write(state: CryptAuthState) {
        val bytes = CryptAuthStateCodec.encode(state)
        val base = file.baseFile
        val temporary = File(base.path + ".new")
        try {
            // Android AtomicFile.finishWrite only logs some sync/rename failures. Counter
            // reservation must observe them, so use checked POSIX operations for the commit.
            // read() retains AtomicFile's recovery for any legacy .bak file.
            if (File(base.path + ".bak").exists()) throw IOException("CryptAuth recovery required")
            val descriptor = Os.open(temporary.path,
                OsConstants.O_WRONLY or OsConstants.O_CREAT or OsConstants.O_TRUNC or OsConstants.O_NOFOLLOW or OsConstants.O_CLOEXEC, 0x180)
            FileOutputStream(descriptor).use { output ->
                Os.fchmod(descriptor, 0x180) // 0600, including an existing temporary file.
                output.write(bytes)
                output.fd.sync() // Unlike AtomicFile.finishWrite, this propagates sync failure.
            }
            Os.rename(temporary.path, base.path)
            syncDirectory(requireNotNull(base.parentFile))
        } catch (failure: Exception) {
            // A post-rename failure may already have consumed a counter. Never roll it back.
            // No caller receives the reserved counter unless the entire commit succeeds.
            throw IOException("CryptAuth state commit failed", failure)
        } finally { bytes.fill(0) }
    }

    private fun syncDirectory(directory: File) {
        val descriptor = Os.open(directory.path, OsConstants.O_RDONLY or OsConstants.O_CLOEXEC, 0)
        try {
            check(OsConstants.S_ISDIR(Os.fstat(descriptor).st_mode)) { "CryptAuth storage is not a directory" }
            Os.fsync(descriptor)
        } finally { Os.close(descriptor) }
    }
}

/** Bounded private storage format; not an on-the-wire representation. */
internal object CryptAuthStateCodec {
    const val MAX_BYTES = 256 * 1024
    private const val VERSION = 2

    fun encode(state: CryptAuthState): ByteArray {
        validate(state)
        return ByteArrayOutputStream().apply {
            DataOutputStream(this).use { out ->
                out.writeInt(VERSION)
                out.writeUTF(state.instanceId)
                out.writeInt(state.keys.size)
                for (key in state.keys) {
                    out.writeUTF(key.name)
                    out.bytes(key.handle)
                    out.writeInt(key.type.value)
                    out.bytes(key.publicKey)
                    out.bytes(key.secret)
                    out.writeBoolean(key.active)
                }
                out.writeInt(state.pendingKeys.size)
                for (key in state.pendingKeys) {
                    out.writeUTF(key.name)
                    out.bytes(key.handle)
                    out.writeInt(key.type.value)
                    out.bytes(key.publicKey)
                    out.bytes(key.secret)
                    out.writeBoolean(key.active)
                }
                out.writeInt(state.directives.size)
                for ((name, directive) in state.directives) {
                    out.writeUTF(name)
                    out.bytes(directive.encodeByteString())
                }
                out.bytes(state.clientDirective?.encodeByteString() ?: ByteString.EMPTY)
                out.writeInt(state.otpCounters.size)
                for ((handle, counter) in state.otpCounters) {
                    out.bytes(handle)
                    out.writeLong(counter)
                }
            }
        }.toByteArray().also { require(it.size <= MAX_BYTES) { "CryptAuth state exceeds limit" } }
    }

    fun decode(bytes: ByteArray): CryptAuthState {
        require(bytes.size <= MAX_BYTES) { "CryptAuth state exceeds limit" }
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            require(version in 1..VERSION) { "Unsupported CryptAuth state" }
            val instanceId = input.readUTF()
            val keys = List(input.count(64)) {
                CryptAuthKey(input.readUTF(), input.bytes(256),
                    requireNotNull(KeyType.fromValue(input.readInt())) { "Invalid CryptAuth key type" },
                    input.bytes(512), input.bytes(4096), input.readBoolean())
            }
            val pendingKeys = List(input.count(64)) {
                CryptAuthKey(input.readUTF(), input.bytes(256),
                    requireNotNull(KeyType.fromValue(input.readInt())) { "Invalid CryptAuth key type" },
                    input.bytes(512), input.bytes(4096), input.readBoolean())
            }
            val directives = mutableMapOf<String, KeyDirective>()
            repeat(input.count(2)) {
                val name = input.readUTF()
                require(name !in directives) { "Duplicate CryptAuth directive" }
                directives[name] = KeyDirective.ADAPTER.decode(input.bytes(16384))
            }
            val clientBytes = input.bytes(16384)
            val client = if (clientBytes.size == 0) null else ClientDirective.ADAPTER.decode(clientBytes)
            val counters = mutableMapOf<ByteString, Long>()
            if (version >= 2) repeat(input.count(32)) {
                val handle = input.bytes(256)
                require(handle !in counters) { "Duplicate CryptAuth counter" }
                counters[handle] = input.readLong()
            }
            require(input.read() == -1) { "Trailing CryptAuth state data" }
            CryptAuthState(instanceId, keys, directives, client, pendingKeys, counters).also { validate(it) }
        }
    }

    private fun validate(state: CryptAuthState) {
        require(state.instanceId.isNotEmpty() && state.instanceId.length <= 128) { "Invalid CryptAuth instance" }
        require(state.keys.size <= 64 && state.directives.keys.all { it in CryptAuthEnrollment.KEY_NAMES }) { "Invalid CryptAuth state" }
        if (state.pendingKeys.isNotEmpty()) {
            validate(state.copy(keys = state.pendingKeys, pendingKeys = emptyList(), otpCounters = emptyMap()))
            for (pending in state.pendingKeys) {
                state.keys.find { it.name == pending.name && it.handle == pending.handle }?.let { existing ->
                    require(existing.type == pending.type && existing.publicKey == pending.publicKey && existing.secret == pending.secret) {
                        "Conflicting CryptAuth pending key"
                    }
                }
            }
        }
        require(state.otpCounters.size <= 32 && state.otpCounters.all { (handle, counter) ->
            counter >= 0 && state.keys.any { it.name == "authzen" && it.handle == handle }
        }) { "Invalid CryptAuth counter state" }
        for (name in CryptAuthEnrollment.KEY_NAMES) {
            val keys = state.keys.filter { it.name == name }
            require(keys.size <= 32 && keys.map { it.handle }.distinct().size == keys.size && keys.count { it.active } <= 1) {
                "Invalid CryptAuth key state"
            }
            if (name == "PublicKey") require(keys.size <= 1) { "Multiple CryptAuth identity keys" }
        }
        for (key in state.keys) {
            require(key.name in CryptAuthEnrollment.KEY_NAMES && key.handle.size in 1..256) { "Invalid CryptAuth key" }
            if (key.name == "PublicKey") {
                require(key.type == KeyType.P256 && key.handle == CryptAuthEnrollment.DEVICE_KEY_HANDLE) { "Invalid CryptAuth identity" }
                val publicKey = CryptAuthCrypto.parsePublicKey(key.publicKey)
                val privateKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(key.secret.toByteArray()))
                val challenge = "CryptAuth stored key validation".toByteArray(Charsets.UTF_8)
                val signature = Signature.getInstance("SHA256withECDSA").run { initSign(privateKey); update(challenge); sign() }
                require(Signature.getInstance("SHA256withECDSA").run { initVerify(publicKey); update(challenge); verify(signature) }) {
                    "CryptAuth private and public keys do not match"
                }
            } else {
                require(key.publicKey.size == 0 && ((key.type == KeyType.RAW128 && key.secret.size == 16) ||
                    (key.type == KeyType.RAW256 && key.secret.size == 32))) { "Invalid CryptAuth symmetric key" }
                require(key.handle == key.secret.sha256().base64Url().encodeUtf8()) { "Invalid CryptAuth key handle" }
            }
        }
    }

    private fun DataOutputStream.bytes(bytes: ByteString) { writeInt(bytes.size); write(bytes.toByteArray()) }
    private fun DataInputStream.count(maximum: Int): Int = readInt().also { require(it in 0..maximum) { "Invalid CryptAuth state length" } }
    private fun DataInputStream.bytes(maximum: Int): ByteString = ByteArray(count(maximum)).also { readFully(it) }.toByteString()
}
