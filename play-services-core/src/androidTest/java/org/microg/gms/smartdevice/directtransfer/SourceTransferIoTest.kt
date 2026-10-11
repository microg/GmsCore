/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.os.ParcelFileDescriptor
import android.test.AndroidTestCase
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Uses Android's real pipes; a successful close must free the sole transfer worker. */
@Suppress("DEPRECATION")
class SourceTransferIoTest : AndroidTestCase() {
    fun testCancelUnblocksPipeReadAndClosesBothDescriptors() = withPipes { io, input, output, worker ->
        val inputFd = input.fileDescriptor
        val outputFd = output.fileDescriptor
        val entered = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        worker.execute {
            try { io.run { entered.countDown(); io.read() } } catch (_: Exception) { }
            finally { stopped.countDown() }
        }
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        assertFalse(stopped.await(100, TimeUnit.MILLISECONDS))
        io.close()
        assertTrue(stopped.await(3, TimeUnit.SECONDS))
        assertEquals(7, worker.submit<Int> { 7 }.get(3, TimeUnit.SECONDS))
        assertFalse(inputFd.valid())
        assertFalse(outputFd.valid())
        io.close()
    }

    fun testCancelUnblocksFullPipeWrite() = withPipes { io, _, _, worker ->
        val entered = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        worker.execute {
            try { io.run { entered.countDown(); io.write(ByteArray(1024 * 1024) { 1 }) } } catch (_: Exception) { }
            finally { stopped.countDown() }
        }
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        assertFalse(stopped.await(100, TimeUnit.MILLISECONDS))
        io.close()
        assertTrue(stopped.await(3, TimeUnit.SECONDS))
        assertEquals(7, worker.submit<Int> { 7 }.get(3, TimeUnit.SECONDS))
    }

    fun testCancelInterruptsSuspendedFactory() = withPipes { io, _, _, worker ->
        val entered = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val neverReturns = CompletableDeferred<Unit>()
        worker.execute {
            try { io.run { entered.countDown(); neverReturns.await() } } catch (_: Exception) { }
            finally { stopped.countDown() }
        }
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        io.close()
        assertTrue(stopped.await(3, TimeUnit.SECONDS))
        assertEquals(7, worker.submit<Int> { 7 }.get(3, TimeUnit.SECONDS))
    }

    fun testReadDeadlineTriggersSameCancellationPath() = withPipes { io, _, _, worker ->
        val entered = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        worker.execute {
            try { io.run { entered.countDown(); io.read() } } catch (_: Exception) { }
            finally { stopped.countDown() }
        }
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        assertFalse(stopped.await(100, TimeUnit.MILLISECONDS))
        try { io.enforceDeadline(Long.MAX_VALUE); fail("Expected deadline expiry") }
        catch (_: IllegalStateException) { io.close() }
        assertTrue(stopped.await(3, TimeUnit.SECONDS))
        assertEquals(7, worker.submit<Int> { 7 }.get(3, TimeUnit.SECONDS))
    }

    private fun withPipes(block: (SourceTransferIo, ParcelFileDescriptor, ParcelFileDescriptor,
                                 java.util.concurrent.ExecutorService) -> Unit) {
        val incoming = ParcelFileDescriptor.createPipe()
        val outgoing = ParcelFileDescriptor.createPipe()
        val input = ParcelFileDescriptor.dup(incoming[0].fileDescriptor)
        val output = ParcelFileDescriptor.dup(outgoing[1].fileDescriptor)
        incoming[0].close()
        outgoing[1].close()
        val io = SourceTransferIo(input, output)
        val worker = Executors.newSingleThreadExecutor()
        try { block(io, input, output, worker) }
        finally {
            io.close()
            incoming[1].close()
            outgoing[0].close()
            worker.shutdownNow()
        }
    }
}
