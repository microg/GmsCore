/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cameralowlight

import android.util.Log
import com.google.android.libraries.camera.capture.lowlightboost.internal.ILowLightBoostCallback
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_PENDING_SCENE_CALLBACKS = 16
private const val MAX_PENDING_LIFECYCLE_CALLBACKS = 2
private const val MAX_CONCURRENT_LIFECYCLE_CALLBACKS = 2
private const val MAX_CONCURRENT_RENDERER_CLEANUPS = 2
private const val DISPATCHER_THREAD_KEEP_ALIVE_SECONDS = 30L
private const val RENDERER_CLEANUP_RETRY_DELAY_MILLIS = 1_000L
private const val MAX_RENDERER_CLEANUP_ATTEMPTS = 3

internal object LowLightBoostDispatcher {
    private val sceneExecutor by lazy {
        newExecutor("LowLightBoostSceneCallback", 1, 1, ArrayBlockingQueue(MAX_PENDING_SCENE_CALLBACKS))
    }
    private val lifecycleExecutor by lazy {
        newExecutor("LowLightBoostLifecycleCallback", 0, MAX_CONCURRENT_LIFECYCLE_CALLBACKS, SynchronousQueue())
    }
    private val cleanupExecutor by lazy {
        newExecutor(
            "LowLightBoostCleanup",
            MAX_CONCURRENT_RENDERER_CLEANUPS,
            MAX_CONCURRENT_RENDERER_CLEANUPS,
            ArrayBlockingQueue(MAX_CONCURRENT_RENDERER_CLEANUPS),
        )
    }

    fun tryEnqueueSceneBrightness(
        callback: ILowLightBoostCallback,
        boostStrength: Float,
        callbackActive: AtomicBoolean,
    ): Boolean = tryExecute(sceneExecutor) {
        if (callbackActive.get()) callback.tryNotifySceneBrightness(boostStrength)
    }

    fun tryEnqueueSessionStatus(callback: ILowLightBoostCallback, status: Int): Boolean = tryExecute(sceneExecutor) {
        callback.tryNotifySessionStatus(status)
    }

    fun tryExecuteLifecycle(task: () -> Unit): Boolean = tryExecute(lifecycleExecutor, task)

    fun tryAwaitRendererRelease(renderer: LowLightBoostRenderer, onReleased: () -> Unit): Boolean {
        return tryExecute(cleanupExecutor) {
            repeat(MAX_RENDERER_CLEANUP_ATTEMPTS) { attempt ->
                if (tryReleaseRenderer(renderer)) {
                    onReleased()
                    return@tryExecute
                }
                if (attempt == MAX_RENDERER_CLEANUP_ATTEMPTS - 1) return@repeat
                try {
                    Thread.sleep(RENDERER_CLEANUP_RETRY_DELAY_MILLIS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(TAG, "Renderer cleanup retry was interrupted; keeping its session reservation", e)
                    return@tryExecute
                }
            }
            Log.e(TAG, "Renderer cleanup retry limit reached; keeping its session reservation")
        }.also { accepted ->
            if (!accepted) Log.e(TAG, "Renderer cleanup queue is full; keeping its session reservation")
        }
    }

    private fun tryReleaseRenderer(renderer: LowLightBoostRenderer): Boolean {
        return try {
            renderer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Renderer cleanup retry failed", e)
            false
        }
    }

    private fun tryExecute(executor: ThreadPoolExecutor, task: () -> Unit): Boolean {
        return try {
            executor.execute(task)
            true
        } catch (_: RejectedExecutionException) {
            false
        }
    }
}

internal class LowLightBoostLifecycleCallbackQueue(
    private val submit: ((() -> Unit) -> Boolean) = LowLightBoostDispatcher::tryExecuteLifecycle,
) {
    private val lock = Any()
    private val pendingCallbacks = ArrayDeque<() -> Unit>(MAX_PENDING_LIFECYCLE_CALLBACKS)
    private var workerRunning = false

    fun tryEnqueue(callback: () -> Unit): Boolean = synchronized(lock) {
        if (pendingCallbacks.size >= MAX_PENDING_LIFECYCLE_CALLBACKS) return@synchronized false
        pendingCallbacks.addLast(callback)
        if (workerRunning) return@synchronized true
        workerRunning = true
        if (submit(::drain)) return@synchronized true
        workerRunning = false
        pendingCallbacks.clear()
        false
    }

    private fun drain() {
        while (true) {
            val callback = synchronized(lock) {
                if (pendingCallbacks.isEmpty()) {
                    workerRunning = false
                    return
                }
                pendingCallbacks.removeFirst()
            }
            try {
                callback()
            } catch (e: Exception) {
                Log.e(TAG, "Lifecycle callback task failed", e)
            } catch (e: Error) {
                synchronized(lock) {
                    pendingCallbacks.clear()
                    workerRunning = false
                }
                throw e
            }
        }
    }
}

private fun newExecutor(
    threadName: String,
    coreThreads: Int,
    maxThreads: Int,
    queue: BlockingQueue<Runnable>,
): ThreadPoolExecutor = ThreadPoolExecutor(
    coreThreads,
    maxThreads,
    DISPATCHER_THREAD_KEEP_ALIVE_SECONDS,
    TimeUnit.SECONDS,
    queue,
    { task -> Thread(task, threadName).apply { isDaemon = true } },
    ThreadPoolExecutor.AbortPolicy(),
).apply { allowCoreThreadTimeOut(true) }
