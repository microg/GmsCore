/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.ocr.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class OcrModelDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pack = inputData.getString("pack") ?: return@withContext Result.failure()
        val token = inputData.getString("token") ?: return@withContext Result.failure()
        if (!OcrModels.supported || pack !in OcrModels.packs) return@withContext Result.failure()
        downloads.withLock {
            try {
                install(pack, token)
            } catch (e: RateLimited) {
                OcrModels.postpone(applicationContext, e.until)
                Result.retry()
            } catch (e: RejectedDownload) {
                fail(pack, token)
            } catch (e: IOException) {
                if (runAttemptCount < 4) Result.retry() else fail(pack, token)
            }
        }
    }

    private suspend fun install(pack: String, token: String): Result {
        val context = applicationContext
        if (!OcrModels.isApproved(context, pack, token)) return Result.failure()
        if (OcrModels.retryUntil(context) > System.currentTimeMillis()) return Result.retry()
        val files = OcrModels.approvedFiles(context, pack, token) ?: return fail(pack, token)
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
            } finally {
                temporary.delete()
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
        val connection = connect(URL(model.url))
        try {
            val advertisedSize = connection.getHeaderField("Content-Length")?.toLongOrNull()
            if (advertisedSize != null && advertisedSize != model.bytes) throw RejectedDownload()
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var received = 0L
            var lastProgress = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > model.bytes) throw RejectedDownload()
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        if (received - lastProgress >= 512 * 1024) {
                            report(received)
                            lastProgress = received
                        }
                    }
                    if (received != model.bytes) throw IOException("Incomplete OCR model download")
                    if (digest.digest().hex() != model.sha256) throw RejectedDownload()
                    output.fd.sync()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun connect(initial: URL): HttpURLConnection {
        var url = initial
        repeat(6) {
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
            }
            var keep = false
            try {
                when (connection.responseCode) {
                    200 -> {
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
        val RATE_LIMIT_RESET = Regex("""(?:^|[;,\s])t=(\d+)""")
    }
}
