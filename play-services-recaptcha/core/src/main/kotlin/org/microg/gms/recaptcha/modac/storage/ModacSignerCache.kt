/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac.storage

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private const val TAG = "RecaptchaQr"
private const val FILE_PREFIX = "rce_"
private val FILE_LOCK = Any()

internal class ModacSignerCache(private val context: Context) {

    fun <T> withExclusiveAccess(block: () -> T): T = synchronized(FILE_LOCK, block)

    fun read(key: String): String? = synchronized(FILE_LOCK) {
        val file = cacheFile(key)
        if (!file.exists()) return@synchronized null
        try {
            String(AtomicFile(file).readFully(), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Credential cache read failed", e)
            null
        }
    }

    fun write(key: String, value: String): Boolean = synchronized(FILE_LOCK) {
        val atomicFile = AtomicFile(cacheFile(key))
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(value.toByteArray(StandardCharsets.UTF_8))
            atomicFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            stream?.let(atomicFile::failWrite)
            Log.w(TAG, "Credential cache write failed", e)
            false
        }
    }

    fun delete(key: String): Boolean = synchronized(FILE_LOCK) {
        val file = cacheFile(key)
        if (!file.exists()) return@synchronized true
        try {
            AtomicFile(file).delete()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Credential cache delete failed", e)
            false
        }
    }

    private fun cacheFile(key: String): File = File(context.cacheDir, FILE_PREFIX + key.sha256())

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
        .joinToString(separator = "") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
