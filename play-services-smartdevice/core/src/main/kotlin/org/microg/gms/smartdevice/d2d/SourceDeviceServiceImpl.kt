/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.app.PendingIntent
import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.smartdevice.d2d.AccountTransferResult
import com.google.android.gms.smartdevice.d2d.BootstrapConfigurations
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener2
import com.google.android.gms.smartdevice.d2d.internal.ISourceDeviceCallbacks
import com.google.android.gms.smartdevice.d2d.internal.ISourceDeviceService
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SourceDeviceServiceImpl"

class SourceDeviceServiceImpl(
    private val context: Context,
    private val sourceHandler: Handler,
    registerDirectTransferKeys: (Account, Boolean, (Boolean) -> Unit) -> Unit,
    startSourceFidoTransfer: StartSourceFidoTransfer,
) : ISourceDeviceService.Stub() {

    private val workExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "D2D-Dispatcher").apply { isDaemon = true }
    }

    private val transferController = D2DTransferController(
        context,
        sourceHandler,
        workExecutor,
        registerDirectTransferKeys,
        startSourceFidoTransfer,
    )

    private val destroyed = AtomicBoolean(false)

    fun destroy() {
        if (!destroyed.compareAndSet(false, true)) return
        transferController.destroy()
        val shutdownWorker = Runnable { workExecutor.shutdownNow() }
        if (!sourceHandler.post(shutdownWorker)) shutdownWorker.run()
    }

    override fun getBootstrappableAccounts(
        callbacks: ISourceDeviceCallbacks,
        apiMetadata: ApiMetadata?,
    ) {
        transferController.reportBootstrappableAccounts(callbacks)
    }

    override fun startDirectTransfer(
        callbacks: ISourceDeviceCallbacks,
        configuration: BootstrapConfigurations,
        pipes: Array<ParcelFileDescriptor>,
        transferListener: IDirectTransferListener?,
        apiMetadata: ApiMetadata?,
    ) {
        transferController.startDirectTransfer(
            callbacks,
            configuration,
            pipes,
            transferListener ?: EMPTY_DIRECT_TRANSFER_LISTENER,
        )
    }

    override fun abortDirectTransfer(
        callbacks: ISourceDeviceCallbacks,
        apiMetadata: ApiMetadata?,
    ) {
        transferController.abortDirectTransfer(callbacks)
    }

    override fun isTransferInProgress(
        callbacks: ISourceDeviceCallbacks,
        extras: Bundle,
        apiMetadata: ApiMetadata?,
    ) {
        transferController.isTransferInProgress(callbacks, extras)
    }

    override fun startDirectTransfer2(
        callbacks: ISourceDeviceCallbacks,
        configuration: BootstrapConfigurations,
        pipes: Array<ParcelFileDescriptor>,
        transferListener: IDirectTransferListener2?,
        apiMetadata: ApiMetadata?,
    ) {
        val listenerAdapter = object : IDirectTransferListener.Stub() {
            override fun onAccountsTransferred(results: Array<out AccountTransferResult?>?) {
            }

            override fun onUserActionRequired(pendingIntent: PendingIntent?) {
                transferListener?.onUserActionRequired(pendingIntent)
            }

            override fun onTransferError(errorCode: Int, description: String?) {
                transferListener?.onTransferError(errorCode, description)
            }
        }
        transferController.startDirectTransfer(
            callbacks,
            configuration,
            pipes,
            listenerAdapter,
        )
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }

}

private val EMPTY_DIRECT_TRANSFER_LISTENER = object : IDirectTransferListener.Stub() {
    override fun onAccountsTransferred(results: Array<out AccountTransferResult?>?) = Unit
    override fun onUserActionRequired(pendingIntent: PendingIntent?) = Unit
    override fun onTransferError(errorCode: Int, description: String?) = Unit
}
