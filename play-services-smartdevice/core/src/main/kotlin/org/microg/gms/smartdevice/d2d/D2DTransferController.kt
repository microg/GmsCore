/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.smartdevice.d2d.BootstrapAccount
import com.google.android.gms.smartdevice.d2d.BootstrapConfigurations
import com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener
import com.google.android.gms.smartdevice.d2d.internal.ISourceDeviceCallbacks
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "D2DTransferController"
private const val DIRECT_TRANSFER_PIPE_COUNT = 2
private const val SESSION_ID_KEY = "sessionId"
private const val ANY_SESSION_ID = -1L

class D2DTransferController(
    private val context: Context,
    private val sessionHandler: Handler,
    private val executor: Executor,
    registerDirectTransferKeys: (Account, Boolean, (Boolean) -> Unit) -> Unit,
    private val startSourceFidoTransfer: StartSourceFidoTransfer,
) {
    private val session = AtomicReference<DirectTransferSession?>(null)
    private val enrollmentChecker = DirectTransferEnrollmentChecker(
        registerDirectTransferKeys,
    )

    fun startDirectTransfer(
        callbacks: ISourceDeviceCallbacks,
        configurations: BootstrapConfigurations,
        pipes: Array<ParcelFileDescriptor>,
        transferListener: IDirectTransferListener,
    ) {
        val operation = Runnable {
            startDirectTransferOnHandler(
                callbacks,
                configurations,
                pipes,
                transferListener,
            )
        }
        if (Looper.myLooper() == sessionHandler.looper) {
            operation.run()
        } else if (!sessionHandler.post(operation)) {
            val message = "Source DirectTransfer handler is unavailable"
            Log.e(TAG, message)
            closePipes(pipes)
            runCatching {
                callbacks.onStartDirectTransferResult(Status(CommonStatusCodes.INTERNAL_ERROR, message))
            }.onFailure { Log.w(TAG, "Failed to report handler rejection", it) }
        }
    }

    private fun startDirectTransferOnHandler(
        callbacks: ISourceDeviceCallbacks,
        configurations: BootstrapConfigurations,
        pipes: Array<ParcelFileDescriptor>,
        transferListener: IDirectTransferListener,
    ) {
        synchronized(this) {
            if (session.get() != null) {
                Log.e(TAG, "Bootstrap cannot be started — bootstrap already in progress.")
                closePipes(pipes)
                runCatching {
                    callbacks.onStartDirectTransferResult(Status(D2D_STATUS_BUSY))
                }.onFailure { Log.w(TAG, "Failed to report busy DirectTransfer", it) }
                return
            }

            if (pipes.size != DIRECT_TRANSFER_PIPE_COUNT) {
                val message = "Expected $DIRECT_TRANSFER_PIPE_COUNT DirectTransfer pipes, got ${pipes.size}"
                Log.e(TAG, message)
                closePipes(pipes)
                runCatching {
                    callbacks.onStartDirectTransferResult(
                        Status(D2D_STATUS_DIRECT_TRANSFER_PIPE_ERROR, message),
                    )
                }.onFailure { Log.w(TAG, "Failed to report invalid DirectTransfer pipes", it) }
                runCatching {
                    transferListener.onTransferError(D2D_STATUS_DIRECT_TRANSFER_PIPE_ERROR, message)
                }.onFailure { Log.w(TAG, "Failed to report DirectTransfer pipe error", it) }
                return
            }

            val s = try {
                DirectTransferSession(
                    context = context,
                    sessionHandler = sessionHandler,
                    readerExecutor = executor,
                    configurations = configurations,
                    input = D2DInputStream(pipes[0]),
                    output = D2DOutputStream(pipes[1]),
                    transferListener = transferListener,
                    enrollmentChecker = enrollmentChecker,
                    startSourceFidoTransfer = startSourceFidoTransfer,
                    onComplete = { sessionRef ->
                        session.compareAndSet(sessionRef, null)
                    },
                )
            } catch (e: Exception) {
                val message = "Unable to open DirectTransfer pipes: ${e.message ?: e.javaClass.simpleName}"
                Log.e(TAG, message, e)
                closePipes(pipes)
                runCatching {
                    callbacks.onStartDirectTransferResult(
                        Status(D2D_STATUS_DIRECT_TRANSFER_PIPE_ERROR, message),
                    )
                }.onFailure { Log.w(TAG, "Failed to report DirectTransfer setup error", it) }
                runCatching {
                    transferListener.onTransferError(D2D_STATUS_DIRECT_TRANSFER_PIPE_ERROR, message)
                }.onFailure { Log.w(TAG, "Failed to report DirectTransfer setup error to listener", it) }
                return
            }

            session.set(s)


            s.start()
            runCatching {
                callbacks.onStartDirectTransferResult(Status(CommonStatusCodes.SUCCESS))
            }.onFailure { Log.w(TAG, "Failed to report DirectTransfer start", it) }
        }
    }

    fun abortDirectTransfer(callbacks: ISourceDeviceCallbacks) {
        dispatchToHandler(
            operation = "abortDirectTransfer",
            onFailure = { status ->
                deliver("onAbortDirectTransferResult") {
                    callbacks.onAbortDirectTransferResult(status)
                }
            },
        ) {
            synchronized(this) {
                val current = session.get()
                if (current == null) {
                    Log.w(TAG, "Bootstrap cannot be aborted -- no bootstrap in progress.")
                    deliver("onAbortDirectTransferResult") {
                        callbacks.onAbortDirectTransferResult(Status(D2D_STATUS_IDLE))
                    }
                    return@synchronized
                }
                current.abort {
                    deliver("onAbortDirectTransferResult") {
                        callbacks.onAbortDirectTransferResult(Status(CommonStatusCodes.SUCCESS))
                    }
                }
            }
        }
    }

    fun isTransferInProgress(callbacks: ISourceDeviceCallbacks, extras: Bundle) {
        dispatchToHandler(
            operation = "isTransferInProgress",
            onFailure = { status ->
                deliver("onIsTransferInProgressResult") {
                    callbacks.onIsTransferInProgressResult(status)
                }
            },
        ) {
            val requestedSessionId = extras.getLong(SESSION_ID_KEY)
            val activeSessionId = DirectTransferSession.activeSessionId()
            val status = when {
                requestedSessionId == ANY_SESSION_ID && activeSessionId == ANY_SESSION_ID ->
                    Status(D2D_STATUS_IDLE)
                requestedSessionId == ANY_SESSION_ID -> Status(CommonStatusCodes.SUCCESS)
                requestedSessionId == activeSessionId -> Status(CommonStatusCodes.SUCCESS)
                else -> Status(D2D_STATUS_SESSION_ID_MISMATCH)
            }
            deliver("onIsTransferInProgressResult") {
                callbacks.onIsTransferInProgressResult(status)
            }
        }
    }

    fun reportBootstrappableAccounts(
        callbacks: ISourceDeviceCallbacks,
    ) {
        dispatchToHandler(
            operation = "getBootstrappableAccounts",
            onFailure = { status ->
                deliver("onAccountTransferResults") {
                    callbacks.onAccountTransferResults(status, emptyList())
                }
            },
        ) {
            synchronized(this) {
                val list = getBootstrappableAccounts(context)
                deliver("onAccountTransferResults") {
                    callbacks.onAccountTransferResults(Status(CommonStatusCodes.SUCCESS), list)
                }
            }
        }
    }

    fun destroy() {
        dispatchToHandler(operation = "destroy", onFailure = {}) {
            synchronized(this) {
                session.getAndSet(null)?.destroy()
            }
        }
    }

    private fun closePipes(pipes: Array<ParcelFileDescriptor>) {
        pipes.forEach { pipe -> runCatching { pipe.close() } }
    }

    private fun dispatchToHandler(
        operation: String,
        onFailure: (Status) -> Unit,
        action: () -> Unit,
    ) {
        val runnable = Runnable {
            try {
                action()
            } catch (e: Exception) {
                Log.e(TAG, "$operation failed", e)
                onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, "$operation failed"))
            }
        }
        if (Looper.myLooper() == sessionHandler.looper) {
            runnable.run()
        } else if (!sessionHandler.post(runnable)) {
            Log.e(TAG, "$operation rejected: source Handler unavailable")
            onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, "Source Handler unavailable"))
        }
    }

    private fun deliver(callback: String, action: () -> Unit) {
        runCatching(action).onFailure { Log.w(TAG, "$callback failed", it) }
    }

    companion object {
        fun getBootstrappableAccounts(context: Context): List<BootstrapAccount> {
            val accs = android.accounts.AccountManager.get(context)
                .getAccountsByType("com.google")
            return accs.map { BootstrapAccount(it.name, it.type) }
        }
    }
}
