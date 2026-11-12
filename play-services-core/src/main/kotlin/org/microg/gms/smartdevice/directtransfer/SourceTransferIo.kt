/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Owns both duplicated descriptors and the coroutine job for their one serial worker. */
internal class SourceTransferIo(
    inputDescriptor: ParcelFileDescriptor,
    outputDescriptor: ParcelFileDescriptor
) : Closeable {
    private val job = Job()
    private val closed = AtomicBoolean()
    private val operationDeadline = AtomicLong()
    private val input = ParcelFileDescriptor.AutoCloseInputStream(inputDescriptor)
    private val output = ParcelFileDescriptor.AutoCloseOutputStream(outputDescriptor)

    fun run(block: suspend () -> Unit) = runBlocking(job) { block() }

    fun read(maximum: Int = DirectTransferFrameCodec.MAX_FRAME_BYTES): ByteArray = operation(120_000) {
        DirectTransferFrameCodec.read(input, maximum)
    }

    fun write(frame: ByteArray) = operation(30_000) { DirectTransferFrameCodec.write(output, frame) }

    fun enforceDeadline(now: Long) {
        val deadline = operationDeadline.get()
        check(!closed.get() && (deadline == 0L || now < deadline)) { "Transfer IO expired" }
    }

    private fun <T> operation(timeoutMillis: Long, block: () -> T): T {
        job.ensureActive()
        check(!closed.get())
        check(operationDeadline.compareAndSet(0L, SystemClock.elapsedRealtime() + timeoutMillis))
        try { return block().also { job.ensureActive() } }
        finally { operationDeadline.set(0L) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        job.cancel()
        // Android's streams signal blocked descriptor operations; cancellation also interrupts awaits.
        try { input.close() } catch (_: Exception) { }
        try { output.close() } catch (_: Exception) { }
    }
}
