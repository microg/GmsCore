/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsExtensions - Utility extensions for RCS
 */

package org.microg.gms.rcs.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.telephony.PhoneNumberUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun String.normalizePhoneNumber(): String {
    return PhoneNumberUtils.normalizeNumber(this)
        .replace(Regex("[^+0-9]"), "")
}

fun String.toE164(defaultCountryCode: String = "US"): String {
    val normalized = normalizePhoneNumber()
    return if (normalized.startsWith("+")) {
        normalized
    } else {
        PhoneNumberUtils.formatNumberToE164(normalized, defaultCountryCode) ?: "+$normalized"
    }
}

fun String.maskPhone(visibleDigits: Int = 4): String {
    if (length <= visibleDigits) return this
    return "*".repeat(length - visibleDigits) + takeLast(visibleDigits)
}

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun Long.toFormattedTime(): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toFormattedDate(): String {
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> toFormattedDate()
    }
}

fun Long.formatBytes(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}

fun Context.getFileName(uri: Uri): String? {
    var result: String? = null
    
    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    
    return result
}

fun Context.getFileSize(uri: Uri): Long {
    var size = 0L
    
    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) {
                    size = cursor.getLong(index)
                }
            }
        }
    } else if (uri.scheme == "file") {
        val file = File(uri.path ?: return 0)
        size = file.length()
    }
    
    return size
}

fun getMimeType(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "mp4" -> "video/mp4"
        "3gp", "3gpp" -> "video/3gpp"
        "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        "ogg" -> "audio/ogg"
        "wav" -> "audio/wav"
        "amr" -> "audio/amr"
        "pdf" -> "application/pdf"
        "vcf" -> "text/vcard"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}

fun generateMessageId(): String {
    return UUID.randomUUID().toString()
}

fun generateTransactionId(): String {
    return "tx_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
}

fun getDeviceInfo(): String {
    return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
}

inline fun <T> runCatchingWithRetry(
    times: Int = 3,
    delayMs: Long = 1000,
    block: () -> T
): Result<T> {
    var lastException: Exception? = null
    
    repeat(times) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Exception) {
            lastException = e
            if (attempt < times - 1) {
                Thread.sleep(delayMs * (attempt + 1))
            }
        }
    }
    
    return Result.failure(lastException ?: Exception("Unknown error"))
}

fun CoroutineScope.launchWithTimeout(
    timeoutMs: Long,
    onTimeout: () -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch {
        var completed = false
        
        val job = launch {
            try {
                block()
            } finally {
                completed = true
            }
        }
        
        launch {
            delay(timeoutMs)
            if (!completed && isActive) {
                job.cancel()
                onTimeout()
            }
        }
    }
}

inline fun <T> T?.orThrow(lazyMessage: () -> String): T {
    return this ?: throw IllegalStateException(lazyMessage())
}

inline fun <T> T?.orDefault(default: () -> T): T {
    return this ?: default()
}

inline fun <T, R> T.letIf(condition: Boolean, block: (T) -> R): R? {
    return if (condition) block(this) else null
}

inline fun <T> T.alsoIf(condition: Boolean, block: (T) -> Unit): T {
    if (condition) block(this)
    return this
}
