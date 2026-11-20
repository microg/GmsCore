/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.model.store

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class OcrModelDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pack = inputData.getString("pack") ?: return@withContext Result.failure()
        val token = inputData.getString("token") ?: return@withContext Result.failure()
        if (!OcrModels.supported || pack !in OcrModels.packs) return@withContext Result.failure()
        if (!downloads.tryLock()) {
            return@withContext if (OcrModels.isApproved(applicationContext, pack, token)) Result.retry() else Result.failure()
        }
        try {
            install(pack, token)
        } catch (e: RateLimited) {
            OcrModels.postpone(applicationContext, e.until)
            Result.retry()
        } catch (e: RejectedDownload) {
            fail(pack, token)
        } catch (e: IOException) {
            if (OcrModels.consumeIoRetry(applicationContext, pack, token)) Result.retry() else fail(pack, token)
        } finally {
            downloads.unlock()
        }
    }

    private suspend fun install(pack: String, token: String): Result {
        val context = applicationContext
        val files = OcrModels.approvedFiles(context, pack, token) ?: return Result.failure()
        if (OcrModels.retryUntil(context) > System.currentTimeMillis()) return Result.retry()
        val missing = files.filterNot { model ->
            val cached = model.blob(context)
            model.exists(context) && hash(cached) == model.sha256
        }
        val total = missing.sumOf { it.bytes }
        var completed = 0L
        report(0L, total)
        for (model in missing) {
            currentCoroutineContext().ensureActive()
            if (!OcrModels.isApproved(context, pack, token)) return Result.failure()
            val temporary = model.partial(context)
            val directory = temporary.parentFile ?: throw IOException("Missing OCR model directory")
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Could not open OCR model directory")
            try {
                download(model, temporary) { report(completed + it, total) }
                currentCoroutineContext().ensureActive()
                if (!OcrModels.publish(context, pack, token, model)) return Result.failure()
            } catch (e: RejectedDownload) {
                temporary.delete()
                throw e
            } finally {
                // WorkManager can stop and resume this request without revoking its approval.
                if (!OcrModels.isApproved(context, pack, token)) temporary.delete()
            }
            completed += model.bytes
            report(completed, total)
        }
        return if (OcrModels.activate(context, pack, token)) Result.success() else fail(pack, token)
    }

    private fun fail(pack: String, token: String): Result {
        OcrModels.fail(applicationContext, pack, token)
        return Result.failure()
    }

    private suspend fun report(bytes: Long, total: Long) {
        setProgress(workDataOf("downloadedBytes" to bytes, "totalBytes" to total))
    }

    private suspend fun download(model: OcrModelFile, destination: File, report: suspend (Long) -> Unit) {
        val existingBytes = destination.length()
        if (existingBytes != model.bytes || hash(destination) != model.sha256) {
            var offset = if (existingBytes < model.bytes) existingBytes else 0L
            var reusedPrefix = offset > 0
            while (offset < model.bytes) {
                val connection = connect(URL(model.url), offset)
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) reusedPrefix = false
                    offset = transfer(connection, model, destination, offset, report)
                } finally {
                    connection.disconnect()
                }
            }
            if (destination.length() != model.bytes || hash(destination) != model.sha256) {
                if (reusedPrefix && destination.delete()) throw IOException("Resumed OCR model failed verification")
                throw RejectedDownload()
            }
        }
        FileOutputStream(destination, true).use { it.fd.sync() }
    }

    private suspend fun transfer(
        connection: HttpURLConnection,
        model: OcrModelFile,
        destination: File,
        offset: Long,
        report: suspend (Long) -> Unit
    ): Long {
        val encoding = connection.getHeaderField("Content-Encoding")
        if (encoding != null && !encoding.equals("identity", ignoreCase = true)) throw RejectedDownload()
        var received = 0L
        var end = model.bytes
        if (connection.responseCode == HttpURLConnection.HTTP_PARTIAL) {
            val range = connection.getHeaderField("Content-Range")?.let { CONTENT_RANGE.matchEntire(it) }
                ?: throw RejectedDownload()
            val first = range.groupValues[1].toLongOrNull()
            val last = range.groupValues[2].toLongOrNull()
            val total = range.groupValues[3].toLongOrNull()
            if (offset == 0L || first != offset || last == null || last !in offset until model.bytes ||
                total != model.bytes || destination.length() != offset
            ) throw RejectedDownload()
            received = offset
            end = last + 1
        }
        val advertisedSize = connection.getHeaderField("Content-Length")
        if (advertisedSize != null && advertisedSize.toLongOrNull() != end - received) throw RejectedDownload()
        val buffer = ByteArray(64 * 1024)
        var lastProgress = received
        connection.inputStream.use { input ->
            // An OK response ignored the Range header, so replace the partial file.
            FileOutputStream(destination, received != 0L).use { output ->
                if (received != 0L) report(received)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count > end - received) throw RejectedDownload()
                    received += count
                    output.write(buffer, 0, count)
                    if (received - lastProgress >= 512 * 1024) {
                        report(received)
                        lastProgress = received
                    }
                }
                if (received != end) throw IOException("Incomplete OCR model download")
            }
        }
        return received
    }

    private suspend fun connect(initial: URL, offset: Long): HttpURLConnection {
        var url = initial
        repeat(6) {
            currentCoroutineContext().ensureActive()
            val host = url.host
            val allowed = host == "huggingface.co" || host.endsWith(".huggingface.co") ||
                    host.endsWith(".hf.co")
            if (url.protocol != "https" || !allowed || url.userInfo != null || url.port !in listOf(-1, 443)) {
                throw RejectedDownload()
            }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept-Encoding", "identity")
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }
            var keep = false
            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL -> {
                        keep = true
                        return connection
                    }

                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw RejectedDownload()
                        url = URL(url, location)
                    }

                    429 -> throw RateLimited(retryTime(connection))
                    408, in 500..599 -> throw IOException("OCR model server unavailable")
                    else -> throw RejectedDownload()
                }
            } finally {
                if (!keep) connection.disconnect()
            }
        }
        throw RejectedDownload()
    }

    private fun retryTime(connection: HttpURLConnection): Long {
        val now = System.currentTimeMillis()
        val retryAfter = connection.getHeaderField("Retry-After")
        val seconds = retryAfter?.toLongOrNull()?.takeIf { it >= 0 && it <= (Long.MAX_VALUE - now) / 1000 }
        val retryDate = if (android.os.Build.VERSION.SDK_INT >= 26 && retryAfter != null) {
            runCatching {
                ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
            }.getOrNull()
        } else {
            null
        }
        val resetSeconds = connection.getHeaderField("RateLimit")?.let { value ->
            RATE_LIMIT_RESET.find(value)?.groupValues?.get(1)?.toLongOrNull()
        }?.takeIf { it <= (Long.MAX_VALUE - now) / 1000 }
        return maxOf(
            now + 30_000L,
            seconds?.let { now + it * 1000 } ?: 0L,
            retryDate ?: 0L,
            resetSeconds?.let { now + it * 1000 } ?: 0L
        )
    }

    private fun hash(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().hex()
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 255) }

    private class RejectedDownload : IOException()
    private class RateLimited(val until: Long) : IOException()

    private companion object {
        val downloads = Mutex()
        val CONTENT_RANGE = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)", RegexOption.IGNORE_CASE)
        val RATE_LIMIT_RESET = Regex("""(?:^|[;,\s])t=(\d+)""")
    }
}
