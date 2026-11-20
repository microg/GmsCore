/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.paddle.model

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import org.microg.gms.common.Constants
import java.io.Closeable
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicBoolean

internal class OcrModelUnavailableException(cause: Throwable? = null) :
    IllegalStateException("OCR model is not installed", cause)

internal class OcrModelAccess(context: Context, private val model: RecognitionModel) : Closeable {
    private val resolver = (context.applicationContext ?: context).contentResolver
    private val changed = AtomicBoolean()
    private var version = 0L
    private var observing = false
    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            synchronized(availability) { availability.remove(model.pack) }
            changed.set(true)
        }
    }

    fun request(): Boolean {
        version = requestVersion(resolver, model.pack)
        return version > 0
    }

    fun observeChanges() {
        if (observing) return
        val uri = MODELS_URI.buildUpon().appendPath("pack").appendPath(model.pack).build()
        resolver.registerContentObserver(uri, false, observer)
        observing = true
    }

    fun hasChanged(): Boolean = changed.getAndSet(false)

    private fun open(path: String): ParcelFileDescriptor {
        if (version <= 0) throw OcrModelUnavailableException()
        val uri = MODELS_URI.buildUpon().appendPath("file").appendPath(model.pack)
            .appendPath(version.toString()).appendPath(path).build()
        try {
            return resolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException(path)
        } catch (e: FileNotFoundException) {
            throw OcrModelUnavailableException(e)
        }
    }

    fun map(path: String): ByteBuffer = ParcelFileDescriptor.AutoCloseInputStream(open(path)).use { stream ->
        val channel = stream.channel
        val length = channel.size()
        require(length in 1..Int.MAX_VALUE.toLong()) { "Invalid OCR model length" }
        channel.map(FileChannel.MapMode.READ_ONLY, 0, length)
    }

    fun readDictionary(path: String): String = ParcelFileDescriptor.AutoCloseInputStream(open(path)).use { stream ->
        stream.bufferedReader().readText()
    }

    override fun close() {
        if (!observing) return
        resolver.unregisterContentObserver(observer)
        observing = false
    }

    companion object {
        private val MODELS_URI = Uri.parse("content://${Constants.GMS_PACKAGE_NAME}.ocr.models")
        private const val AVAILABILITY_CHECK_INTERVAL_MS = 1000L
        private data class Availability(val ready: Boolean, val checkedAt: Long)
        private val availability = mutableMapOf<String, Availability>()

        @JvmStatic
        fun isAvailable(context: Context, pack: String): Boolean {
            synchronized(availability) {
                val now = SystemClock.elapsedRealtime()
                val cached = availability[pack]
                // shortcut: descriptor-only callers may see one second of stale state, subscribe if they need immediate changes.
                if (cached != null && now - cached.checkedAt < AVAILABILITY_CHECK_INTERVAL_MS) return cached.ready
                val ready = requestVersion((context.applicationContext ?: context).contentResolver, pack) > 0
                availability[pack] = Availability(ready, now)
                return ready
            }
        }

        private fun requestVersion(resolver: ContentResolver, pack: String): Long {
            return try {
                val state = resolver.call(MODELS_URI, "request", pack, null)
                if (state?.getBoolean("ready") == true) state.getLong("version") else 0L
            } catch (_: IllegalArgumentException) {
                0L
            }
        }
    }
}
