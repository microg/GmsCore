/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.os.ParcelFileDescriptor
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

class D2DInputStream(
    private val pipe: ParcelFileDescriptor,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val input: DataInputStream = DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(pipe))

    @Synchronized
    @Throws(IOException::class)
    fun readFrame(): ByteArray {
        check(!closed.get()) { "input stream closed" }
        val len = input.readInt()
        if (len < 0) {
            throw IOException("Invalid D2D frame size: $len")
        }
        return ByteArray(len).also(input::readFully)
    }

    fun closeQuietly() {
        if (closed.compareAndSet(false, true)) {
            runCatching { input.close() }
        }
    }

    override fun close() = closeQuietly()
}

class D2DOutputStream(
    private val pipe: ParcelFileDescriptor,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val output: DataOutputStream = DataOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(pipe))
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "D2D-PipeWrite").apply { isDaemon = true }
    }

    fun writeFrame(
        payload: ByteArray,
        onComplete: ((Throwable?) -> Unit)? = null,
    ): Future<Unit> = writeFrames(listOf(payload), onComplete)

    fun writeFrames(
        payloads: List<ByteArray>,
        onComplete: ((Throwable?) -> Unit)? = null,
    ): Future<Unit> {
        check(!closed.get()) { "output stream closed" }
        val task = object : FutureTask<Unit>(Callable {
            for (payload in payloads) {
                output.writeInt(payload.size)
                output.write(payload)
            }
            Unit
        }) {
            override fun done() {
                val failure = try {
                    get()
                    null
                } catch (e: ExecutionException) {
                    e.cause ?: e
                } catch (e: CancellationException) {
                    e
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    e
                }
                onComplete?.invoke(failure)
            }
        }
        try {
            writer.execute(task)
        } catch (e: RuntimeException) {
            task.cancel(false)
            throw e
        }
        return task
    }

    fun closeQuietly() {
        if (closed.compareAndSet(false, true)) {
            try {

                writer.execute { runCatching { output.close() } }
                writer.shutdown()
            } catch (_: RuntimeException) {
                writer.shutdownNow()
                runCatching { output.close() }
            }
        }
    }

    override fun close() = closeQuietly()
}
