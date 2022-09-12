/*
 * SPDX-FileCopyrightText: 2016 JetBrains s.r.o.
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tasks

import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * Converts this deferred to the instance of [Task].
 * If deferred is cancelled then resulting task will be cancelled as well.
 */
fun <T> Deferred<T>.asTask(): Task<T> {
    val cancellation = CancellationTokenSource()
    val source = TaskCompletionSource<T>(cancellation.token)

    invokeOnCompletion callback@{
        if (it is CancellationException) {
            cancellation.cancel()
            return@callback
        }

        val t = getCompletionExceptionOrNull()
        if (t == null) {
            source.setResult(getCompleted())
        } else {
            source.setException(t as? Exception ?: RuntimeExecutionException(t))
        }
    }

    return source.task
}

/**
 * Converts this task to an instance of [Deferred].
 * If task is cancelled then resulting deferred will be cancelled as well.
 * However, the opposite is not true: if the deferred is cancelled, the [Task] will not be cancelled.
 * For bi-directional cancellation, an overload that accepts [CancellationTokenSource] can be used.
 */
fun <T> Task<T>.asDeferred(): Deferred<T> = asDeferredImpl(null)

/**
 * Converts this task to an instance of [Deferred] with a [CancellationTokenSource] to control cancellation.
 * The cancellation of this function is bi-directional:
 * * If the given task is cancelled, the resulting deferred will be cancelled.
 * * If the resulting deferred is cancelled, the provided [cancellationTokenSource] will be cancelled.
 *
 * Providing a [CancellationTokenSource] that is unrelated to the receiving [Task] is not supported and
 * leads to an unspecified behaviour.
 */
@ExperimentalCoroutinesApi // Since 1.5.1, tentatively until 1.6.0
fun <T> Task<T>.asDeferred(cancellationTokenSource: CancellationTokenSource): Deferred<T> =
        asDeferredImpl(cancellationTokenSource)

private fun <T> Task<T>.asDeferredImpl(cancellationTokenSource: CancellationTokenSource?): Deferred<T> {
    val deferred = CompletableDeferred<T>()
    if (isComplete) {
        val e = exception
        if (e == null) {
            if (isCanceled) {
                deferred.cancel()
            } else {
                @Suppress("UNCHECKED_CAST")
                deferred.complete(result as T)
            }
        } else {
            deferred.completeExceptionally(e)
        }
    } else {
        addOnCompleteListener {
            val e = it.exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (it.isCanceled) deferred.cancel() else deferred.complete(it.result as T)
            } else {
                deferred.completeExceptionally(e)
            }
        }
    }

    if (cancellationTokenSource != null) {
        deferred.invokeOnCompletion {
            cancellationTokenSource.cancel()
        }
    }
    // Prevent casting to CompletableDeferred and manual completion.
    return object : Deferred<T> by deferred {}
}

/**
 * Awaits the completion of the task without blocking a thread.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * stops waiting for the completion stage and immediately resumes with [CancellationException].
 *
 * For bi-directional cancellation, an overload that accepts [CancellationTokenSource] can be used.
 */
suspend fun <T> Task<T>.await(): T = awaitImpl(null)

/**
 * Awaits the completion of the task that is linked to the given [CancellationTokenSource] to control cancellation.
 *
 * This suspending function is cancellable and cancellation is bi-directional:
 * * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * cancels the [cancellationTokenSource] and throws a [CancellationException].
 * * If the task is cancelled, then this function will throw a [CancellationException].
 *
 * Providing a [CancellationTokenSource] that is unrelated to the receiving [Task] is not supported and
 * leads to an unspecified behaviour.
 */
@ExperimentalCoroutinesApi // Since 1.5.1, tentatively until 1.6.0
suspend fun <T> Task<T>.await(cancellationTokenSource: CancellationTokenSource): T = awaitImpl(cancellationTokenSource)

private suspend fun <T> Task<T>.awaitImpl(cancellationTokenSource: CancellationTokenSource?): T {
    // fast path
    if (isComplete) {
        val e = exception
        return if (e == null) {
            if (isCanceled) {
                throw CancellationException("Task $this was cancelled normally.")
            } else {
                @Suppress("UNCHECKED_CAST")
                result as T
            }
        } else {
            throw e
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = it.exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (it.isCanceled) cont.cancel() else cont.resume(it.result as T)
            } else {
                cont.resumeWithException(e)
            }
        }

        if (cancellationTokenSource != null) {
            cont.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}
