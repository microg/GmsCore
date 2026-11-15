/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism.core

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext

private const val TAG = "AsterismRequest"

internal class AsterismRequestDispatcher(private val scope: CoroutineScope) {
    fun dispatch(
        callbacks: IAsterismCallbacks,
        name: String,
        block: suspend (IAsterismCallbacks) -> Unit
    ): Job = scope.launch(CoroutineName("asterism-$name")) {
        val wrapped = AsterismCallbacksWrapper(callbacks)
        val requestJob = currentCoroutineContext()[Job]
            ?: error("Asterism request coroutine has no Job")
        val binder = callbacks.asBinder()
        val recipient = CallerDeathRecipient(requestJob, name)
        var linked = false
        try {
            try {
                binder.linkToDeath(recipient, 0)
                linked = true
            } catch (e: RemoteException) {
                Log.d(TAG, "Caller for $name already dead; cancelling request")
                requestJob.cancel(CancellationException("Caller process already dead for $name"))
            } catch (e: RuntimeException) {
                Log.w(TAG, "linkToDeath failed for $name", e)
                requestJob.cancel(CancellationException("Cannot monitor caller Binder for $name").apply {
                    initCause(e)
                })
            }
            requestJob.ensureActive()
            block(wrapped)
        } catch (e: CancellationException) {
            Log.d(TAG, "$name request cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$name request failed", e)
        } finally {
            if (linked) unlinkToDeath(binder, recipient)
        }
    }

    private fun unlinkToDeath(binder: IBinder, recipient: IBinder.DeathRecipient) {
        try {
            binder.unlinkToDeath(recipient, 0)
        } catch (_: Exception) {
            // The binder may be dead or the recipient may already have been removed.
        }
    }

    private class CallerDeathRecipient(private val job: Job, private val name: String) : IBinder.DeathRecipient {
        override fun binderDied() {
            Log.d(TAG, "Caller for $name died; cancelling request")
            job.cancel(CancellationException("Caller process died for $name"))
        }
    }
}
