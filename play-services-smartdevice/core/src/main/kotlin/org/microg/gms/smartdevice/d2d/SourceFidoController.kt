/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SourceFidoController"
private const val CTAP_PIPE_BUFFER_SIZE = 32 * 1024

fun interface StartSourceFidoTransfer {
    fun start(
        options: SourceStartDirectTransferOptions,
        requestInput: ParcelFileDescriptor,
        responseOutput: ParcelFileDescriptor,
        onComplete: (Status) -> Unit,
    )
}

class SourceFidoController(
    private val mainHandler: Handler,
    private val starter: StartSourceFidoTransfer,
    private val onResponse: (ByteArray) -> Unit,
    private val onError: (Int, String) -> Unit,
) : Closeable {
    private val closed = AtomicBoolean(false)
    private val responseExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "D2D-FIDO-Pipe").apply { isDaemon = true }
    }
    private var requestPipe: Array<ParcelFileDescriptor>? = null
    private var responsePipe: Array<ParcelFileDescriptor>? = null
    private var requestOutput: ParcelFileDescriptor.AutoCloseOutputStream? = null
    private var responseInput: ParcelFileDescriptor.AutoCloseInputStream? = null

    fun start(options: SourceStartDirectTransferOptions) {
        val requestPipe = ParcelFileDescriptor.createPipe()
        this.requestPipe = requestPipe
        val responsePipe = ParcelFileDescriptor.createPipe()
        this.responsePipe = responsePipe
        requestOutput = ParcelFileDescriptor.AutoCloseOutputStream(requestPipe[1])
        responseInput = ParcelFileDescriptor.AutoCloseInputStream(responsePipe[0])
        responseExecutor.execute {
            val buffer = ByteArray(CTAP_PIPE_BUFFER_SIZE)
            try {
                while (!closed.get()) {
                    val count = responseInput?.read(buffer) ?: -1
                    if (count < 0) break
                    val response = buffer.copyOf(count)
                    if (!mainHandler.post { if (!closed.get()) onResponse(response) }) {
                        Log.w(TAG, "Unable to dispatch source FIDO response")
                        break
                    }
                }
            } catch (e: IOException) {
                if (!closed.get()) {
                    Log.w(TAG, "Unable to read source FIDO response", e)
                    postError(D2D_STATUS_FIDO_PIPE_ERROR, "Reading from FIDO pipe failed")
                }
            }
        }
        starter.start(options, requestPipe[0], responsePipe[1]) { status ->
            if (!status.isSuccess && !closed.get()) {
                Log.w(TAG, "FIDO source service failed with status ${status.statusCode}")
                postError(D2D_STATUS_FIDO_API_ERROR, "FIDO source service failed")
            }
        }
    }

    @Synchronized
    fun write(request: ByteArray) {
        check(!closed.get()) { "FIDO source controller is closed" }
        val stream = checkNotNull(requestOutput) { "FIDO source controller is not started" }
        stream.write(request)
        stream.flush()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { requestOutput?.close() }
        runCatching { responseInput?.close() }
        closePipe(requestPipe)
        closePipe(responsePipe)
        requestPipe = null
        responsePipe = null
        responseExecutor.shutdownNow()
    }

    private fun postError(code: Int, message: String) {
        if (!mainHandler.post { if (!closed.get()) onError(code, message) }) {
            Log.w(TAG, "Unable to dispatch source FIDO error code=$code")
        }
    }

    private fun closePipe(pipe: Array<ParcelFileDescriptor>?) {
        pipe?.forEach { descriptor -> runCatching { descriptor.close() } }
    }
}
