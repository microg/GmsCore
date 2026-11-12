/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.droidguard.DroidGuardHandle
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Session-transport client that uses the public handle lifecycle.
 *
 * [init] returns [Task]<[DroidGuardHandle]>. The handle then uses [DroidGuardHandle.snapshot],
 * [DroidGuardHandle.isOpened], and [DroidGuardHandle.close] against a retained-session server.
 * This is not a single-request/attest fallback client and does not mint Play Integrity verdicts.
 */
class RemoteDroidGuardHandleClient(
    private val serverUrl: String,
    private val packageName: String,
    private val timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    private val executor: Executor = DEFAULT_EXECUTOR
) : DroidGuardClient {
    override fun init(flow: String, request: DroidGuardResultsRequest?): Task<DroidGuardHandle> {
        val completion = TaskCompletionSource<DroidGuardHandle>()
        try {
            executor.execute {
                try {
                    completion.setResult(openHandle(flow, request))
                } catch (e: Exception) {
                    completion.trySetException(e)
                }
            }
        } catch (e: RejectedExecutionException) {
            completion.trySetException(e)
        }
        return completion.task
    }

    override fun getResults(
        flow: String,
        data: Map<String, String>?,
        request: DroidGuardResultsRequest?
    ): Task<String> {
        val completion = TaskCompletionSource<String>()
        try {
            executor.execute {
                var handle: DroidGuardHandle? = null
                try {
                    handle = openHandle(flow, request)
                    completion.setResult(handle.snapshot(data ?: emptyMap()))
                } catch (e: Exception) {
                    completion.trySetException(e)
                } finally {
                    // The task is already complete here; cleanup failure must not escape the worker.
                    try {
                        handle?.close()
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: RejectedExecutionException) {
            completion.trySetException(e)
        }
        return completion.task
    }

    private fun openHandle(flow: String, request: DroidGuardResultsRequest?): DroidGuardHandle {
        val timeout = (request?.timeoutMillis ?: timeoutMillis).coerceAtLeast(1)
        val session = RemoteDroidGuardSession(
            RemoteDroidGuardHttpClient(serverUrl, timeout),
            buildRequestParameters(flow, request)
        )
        session.begin()
        return RemoteDroidGuardHandle(session)
    }

    private fun buildRequestParameters(flow: String, request: DroidGuardResultsRequest?): Map<String, String> {
        val parameters = linkedMapOf("flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty().sorted()) {
            val value = request?.bundle?.get(key) ?: continue
            val scalar = when (value) {
                is String -> value
                is Number, is Boolean -> value.toString()
                else -> null
            } ?: continue
            parameters["x-request-$key"] = scalar
        }
        return parameters
    }

    private class RemoteDroidGuardHandle(
        private val session: RemoteDroidGuardSession
    ) : DroidGuardHandle {
        @Volatile
        private var opened = true

        override fun snapshot(data: Map<String, String>): String {
            check(opened) { "Remote DroidGuard handle is closed" }
            return session.snapshot(LinkedHashMap<Any?, Any?>(data))
        }

        override fun isOpened(): Boolean = opened

        override fun close() {
            if (!opened) return
            opened = false
            session.close()
        }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 60000
        private val DEFAULT_EXECUTOR: Executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "RemoteDroidGuardHandle").apply { isDaemon = true }
        }
    }
}
