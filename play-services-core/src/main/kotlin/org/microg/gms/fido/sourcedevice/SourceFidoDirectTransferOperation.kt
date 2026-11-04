/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.sourcedevice

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.fido.sourcedevice.SourceStartDirectTransferOptions
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.msgs.Ctap2CommandCode
import org.microg.gms.fido.core.transport.CtapStatus
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SourceFidoTransfer"

class SourceFidoDirectTransferOperation(
    context: Context,
    options: SourceStartDirectTransferOptions,
    input: ParcelFileDescriptor,
    output: ParcelFileDescriptor,
) : Closeable {
    private val authenticator = SourceFidoAuthenticator(context, options)
    private val inputStream = DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(input))
    private val outputStream = DataOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(output))
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "FidoSourceTransfer").apply { isDaemon = true }
    }
    private val closed = AtomicBoolean(false)

    fun start(callback: (Status) -> Unit) {
        executor.execute {
            val status = runCatching {
                var result = Status.SUCCESS
                while (!closed.get()) {
                    val command = inputStream.read()
                    if (command < 0) {
                        result = Status(CommonStatusCodes.DEAD_CLIENT)
                        break
                    }
                    val commandCode = Ctap2CommandCode.entries.firstOrNull {
                        (it.byte.toInt() and 0xff) == command
                    }
                    val request = if (commandCode == Ctap2CommandCode.AuthenticatorGetAssertion) {
                        val parameters = CBORObject.Read(inputStream)
                            .EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical)
                        byteArrayOf(command.toByte()) + parameters
                    } else {
                        byteArrayOf(command.toByte())
                    }
                    val response = authenticator.handle(request)
                    outputStream.write(response)
                    outputStream.flush()
                    val ctapStatus = CtapStatus.fromByte(response[0])
                    result = when (ctapStatus) {
                        CtapStatus.CTAP2_ERR_OPERATION_DENIED -> Status.CANCELED
                        CtapStatus.CTAP1_ERR_INVALID_CHANNEL -> Status(CommonStatusCodes.DEAD_CLIENT)
                        CtapStatus.CTAP1_ERR_TIMEOUT -> Status(CommonStatusCodes.TIMEOUT)
                        CtapStatus.CTAP1_ERR_SUCCESS,
                        CtapStatus.CTAP2_ERR_NO_CREDENTIALS,
                        CtapStatus.CTAP1_ERR_OTHER -> Status.SUCCESS
                        else -> Status.INTERNAL_ERROR
                    }
                    if (authenticator.isComplete || !result.isSuccess) break
                }
                result
            }.getOrElse {
                if (!closed.get()) Log.w(TAG, "Source FIDO pipe operation failed", it)
                Status.INTERNAL_ERROR
            }
            runCatching { callback(status) }
            close()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { inputStream.close() }
        runCatching { outputStream.close() }
        executor.shutdownNow()
    }
}
