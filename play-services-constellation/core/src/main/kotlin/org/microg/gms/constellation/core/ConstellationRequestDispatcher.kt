/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.microg.gms.constellation.core.verification.MtSmsInboxScope
import kotlin.coroutines.coroutineContext

private const val TAG = "C11NRequest"

/**
 * Owns the lifetime of a single Constellation API request.
 *
 * Every request is launched as a cancellable child of the service [CoroutineScope] and is given:
 *
 *  * its own [MtSmsInboxScope], so the MT SMS inboxes it creates are isolated from (and cannot be
 *    disposed by) any other concurrent request;
 *  * a [IBinder.DeathRecipient] on the caller's callback binder, so that when the caller process
 *    dies the request's coroutine — and therefore any in-flight gRPC call — is cancelled promptly
 *    instead of running to completion against a client that is no longer listening;
 *  * exception containment, so a failure in one request is logged rather than propagated to the
 *    service scope where it could tear down unrelated requests or crash the process.
 *
 * Cancellation is propagated to the network layer purely through structured concurrency: Wire's
 * suspending `GrpcCall.execute` registers `continuation.invokeOnCancellation { call.cancel() }`
 * before it enqueues the OkHttp call, so cancelling this request's [Job] aborts the HTTP call
 * synchronously. There is deliberately no late `Job.invokeOnCompletion { grpcCall.cancel() }` hook;
 * the death recipient is unlinked in a `finally` block instead.
 */
internal class ConstellationRequestDispatcher(private val scope: CoroutineScope) {

    /**
     * Launches [block] for the request identified by [name], delivering results through a
     * [ConstellationCallbacksWrapper] around [callbacks]. Returns the request [Job].
     */
    fun dispatch(
        callbacks: IConstellationCallbacks,
        name: String,
        block: suspend (IConstellationCallbacks) -> Unit
    ): Job {
        val wrapper = ConstellationCallbacksWrapper(callbacks)
        return scope.launch(MtSmsInboxScope() + CoroutineName("constellation-$name")) {
            val requestJob = coroutineContext.job
            val binder = callbacks.asBinder()
            val recipient = CallerDeathRecipient(requestJob, name)
            val linked = linkToDeath(binder, recipient, name, requestJob)
            try {
                requestJob.ensureActive()
                block(wrapper)
            } catch (e: CancellationException) {
                Log.d(TAG, "$name request cancelled")
                throw e
            } catch (e: Exception) {
                // Contain the failure: a single failing request must not propagate to the service
                // scope. Errors (e.g. OutOfMemoryError) are intentionally left to propagate.
                Log.e(TAG, "$name request failed", e)
            } finally {
                if (linked) unlinkToDeath(binder, recipient)
            }
        }
    }

    private fun linkToDeath(
        binder: IBinder,
        recipient: IBinder.DeathRecipient,
        name: String,
        requestJob: Job
    ): Boolean = try {
        binder.linkToDeath(recipient, 0)
        true
    } catch (e: RemoteException) {
        // The caller is already gone; there is nothing to do for it.
        Log.d(TAG, "Caller for $name already dead; cancelling request")
        requestJob.cancel(CancellationException("Caller process already dead"))
        false
    } catch (e: RuntimeException) {
        Log.w(TAG, "linkToDeath failed for $name", e)
        false
    }

    private fun unlinkToDeath(binder: IBinder, recipient: IBinder.DeathRecipient) {
        try {
            binder.unlinkToDeath(recipient, 0)
        } catch (e: Exception) {
            // Binder already died or recipient already removed — nothing left to clean up.
        }
    }

    private class CallerDeathRecipient(
        private val requestJob: Job,
        private val name: String
    ) : IBinder.DeathRecipient {
        override fun binderDied() {
            Log.d(TAG, "Caller for $name died; cancelling request")
            requestJob.cancel(CancellationException("Caller process died"))
        }
    }
}
