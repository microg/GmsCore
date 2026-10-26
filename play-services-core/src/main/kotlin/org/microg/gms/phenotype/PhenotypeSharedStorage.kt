/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import okio.ByteString.Companion.toByteString

private fun Context.sharedDirectory(): File = getDir("phenotype_shared", Context.MODE_PRIVATE)

internal object PhenotypeSharedStorage {
    fun write(
        context: Context,
        secret: ByteArray,
        packageName: String,
        user: String,
        payload: ByteArray
    ): File {
        val base = context.sharedDirectory()
        base.parentFile?.let(::ensureWorldExecutable)
        ensureWorldExecutable(base)
        val target = File(base, FlagsContentCodec.sharedFileRelativePath(secret, packageName, user))
        if (target.isFile && target.length() == payload.size.toLong() && target.readBytes().contentEquals(payload)) return target
        val secretDirectory = requireNotNull(target.parentFile)
        ensureWorldExecutable(secretDirectory)
        val temporary = File.createTempFile(target.name, null, secretDirectory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(payload)
                output.fd.sync()
            }
            check(temporary.setReadable(true, false)) { "Failed to set shared file world-readable" }
            check(temporary.renameTo(target)) { "Failed to atomically publish shared file" }
            return target
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun ensureWorldExecutable(directory: File) {
        check(directory.exists() || directory.mkdir()) { "Failed to create shared directory" }
        check(directory.setExecutable(true, false)) { "Failed to set shared directory world-executable" }
    }

    fun delete(context: Context, storageInfo: StorageInfo) {
        val deviceContext = if (SDK_INT >= 24)
            context.createDeviceProtectedStorageContext() else context
        for ((targetContext, secret) in listOf(
            context to storageInfo.secret,
            deviceContext to storageInfo.deviceEncryptedSecret
        )) {
            val directoryName = FlagsContentCodec.sharedFileRelativePath(secret, "", "").substringBefore('/')
            val directory = File(targetContext.sharedDirectory(), directoryName)
            if (directory.exists() && !directory.deleteRecursively()) {
                Log.w(PHENOTYPE_LOG_TAG, "Failed to delete shared storage directory")
            }
        }
    }
}

internal data class StorageInfo(
    val secret: ByteArray,
    val deviceEncryptedSecret: ByteArray
) {
    fun toByteArray(context: Context, baselineCl: Long): ByteArray {
        val deviceContext = if (SDK_INT >= 24) context.createDeviceProtectedStorageContext() else context
        val credentialDirectory = context.sharedDirectory()
        val deviceDirectory = deviceContext.sharedDirectory()
        val types = buildList {
            if (baselineCl == -1L || baselineCl >= 570393914L) add(6)
            if (baselineCl == -1L || baselineCl >= 691852360L) add(4)
        }
        val sdk = SDK_INT.toLong()
        return SharedStorageInfo(
            credentialEncrypted = CredentialStorageInfo(
                secret = secret.toByteString(), enabled = true, directory = credentialDirectory.name,
                generatedAtMillis = System.currentTimeMillis(), storageTypes = types,
                path = StoragePath(parentDirectory = credentialDirectory.parent, sdkVersion = sdk),
                enableCommitV2Api = true, allowEmptySnapshotToken = true
            ),
            deviceEncrypted = DeviceStorageInfo(
                secret = deviceEncryptedSecret.toByteString(), enabled = true, directory = deviceDirectory.name,
                storageTypes = types, path = StoragePath(parentDirectory = deviceDirectory.parent, sdkVersion = sdk),
                enableCommitV2Api = true, allowEmptySnapshotToken = true
            )
        ).encode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StorageInfo
        if (!secret.contentEquals(other.secret)) return false
        if (!deviceEncryptedSecret.contentEquals(other.deviceEncryptedSecret)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = secret.contentHashCode()
        result = 31 * result + deviceEncryptedSecret.contentHashCode()
        return result
    }
}
