/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.google.android.gms.droidguard.DroidGuardHandle
import java.io.Closeable
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class DroidGuardSessionException(val code: Int, message: String) : RuntimeException(message)

/** Retains actual handles; capacity is released only after native cleanup finishes. */
internal class DroidGuardSessionStore(
    private val openHandle: (String, String, Map<String, String>) -> DroidGuardHandle,
    private val operationTimeoutMillis: Long = 45_000,
    private val idleTimeoutMillis: Long = 120_000,
    private val maxSessions: Int = 8,
    private val now: () -> Long = System::nanoTime
) : Closeable {
    private val sessions = linkedMapOf<String, Session>()
    private val capacity = Semaphore(maxSessions)
    private val workers = ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
        ArrayBlockingQueue(maxSessions), threads("DroidGuardSession"))
    private val cleanup = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        ArrayBlockingQueue(maxSessions), threads("DroidGuardSessionClose"))
    private val reaper = Executors.newSingleThreadScheduledExecutor(threads("DroidGuardSessionExpiry"))
    private var stopped = false

    init {
        require(operationTimeoutMillis > 0 && idleTimeoutMillis > 0 && maxSessions > 0)
        val reapInterval = minOf(idleTimeoutMillis, 30_000)
        reaper.scheduleWithFixedDelay(::expireIdle, reapInterval, reapInterval, TimeUnit.MILLISECONDS)
    }

    fun begin(owner: Int, flow: String, source: String, request: Map<String, String>): String {
        val session = synchronized(sessions) {
            if (stopped || !capacity.tryAcquire()) throw DroidGuardSessionException(503, "Native session capacity exhausted")
            Session(owner).also { sessions[it.id] = it }
        }
        return execute(session) {
            val handle = openHandle(flow, source, request)
            session.handle = handle
            if (!handle.isOpened) throw DroidGuardSessionException(502, "Native DroidGuard initialization failed")
            synchronized(sessions) {
                if (sessions[session.id] !== session || session.closed.get()) {
                    throw DroidGuardSessionException(404, "Session was closed during initialization")
                }
                session.id
            }
        }
    }

    fun snapshot(owner: Int, id: String, data: Map<String, String>): String {
        val session = lookup(owner, id)
        return execute(session) {
            val handle = session.handle ?: throw DroidGuardSessionException(404, "Session is not ready")
            if (!handle.isOpened) throw DroidGuardSessionException(502, "Native DroidGuard handle is closed")
            handle.snapshot(data)
        }
    }

    /** Invalidates the ID immediately; an operation already running finishes before native close. */
    fun close(owner: Int, id: String) {
        val session = synchronized(sessions) {
            val found = sessions[id] ?: return
            if (found.owner != owner) throw DroidGuardSessionException(404, "Unknown session")
            sessions.remove(id)
            found
        }
        session.requestClose()
    }

    private fun lookup(owner: Int, id: String): Session {
        val session = synchronized(sessions) {
            sessions[id]?.takeIf { it.owner == owner }
                ?: throw DroidGuardSessionException(404, "Unknown session")
        }
        // Admission and idle retirement share the sessions monitor. An executing or
        // queued native operation owns its session until it updates touched and exits.
        val expired = synchronized(sessions) {
            if (sessions[id] === session &&
                !session.busy.get() &&
                now() - session.touched >= TimeUnit.MILLISECONDS.toNanos(idleTimeoutMillis)
            ) {
                sessions.remove(id)
                true
            } else {
                false
            }
        }
        if (expired) {
            session.requestClose()
            throw DroidGuardSessionException(404, "Session expired")
        }
        return session
    }

    private fun <T> execute(session: Session, operation: () -> T): T {
        // Admit one operation per handle before using a global worker. Waiting on the same
        // handle must not consume every worker and prevent unrelated sessions from progressing.
        synchronized(sessions) {
            if (sessions[session.id] !== session || session.closed.get()) {
                throw DroidGuardSessionException(404, "Session is closed")
            }
            if (!session.busy.compareAndSet(false, true)) {
                throw DroidGuardSessionException(503, "Session already has an operation in progress")
            }
        }
        var submitted = false
        try {
            val future = workers.submit<T> {
                try {
                    session.lock.withLock {
                        if (session.closed.get()) throw DroidGuardSessionException(404, "Session is closed")
                        session.touched = now()
                        operation().also { session.touched = now() }
                    }
                } finally {
                    session.busy.set(false)
                    if (session.closed.get()) session.enqueueCleanup()
                }
            }
            submitted = true
            try {
                return future.get(operationTimeoutMillis, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                future.cancel(false)
                throw DroidGuardSessionException(504, "Native DroidGuard operation timed out")
            } catch (e: ExecutionException) {
                throw (e.cause as? DroidGuardSessionException
                    ?: DroidGuardSessionException(502, "Native DroidGuard operation failed"))
            } catch (e: InterruptedException) {
                future.cancel(false)
                Thread.currentThread().interrupt()
                throw DroidGuardSessionException(503, "Native DroidGuard operation interrupted")
            }
        } catch (e: Exception) {
            if (!submitted) session.busy.set(false)
            synchronized(sessions) { sessions.remove(session.id) }
            session.requestClose()
            if (e is RejectedExecutionException) throw DroidGuardSessionException(503, "Native DroidGuard worker is busy")
            throw e
        }
    }

    internal fun expireIdle() {
        val cutoff = now() - TimeUnit.MILLISECONDS.toNanos(idleTimeoutMillis)
        val expired = synchronized(sessions) {
            sessions.values.filter { !it.busy.get() && it.touched <= cutoff }.also { values ->
                values.forEach { sessions.remove(it.id) }
            }
        }
        expired.forEach { it.requestClose() }
    }

    fun clear() {
        val retained = synchronized(sessions) { sessions.values.toList().also { sessions.clear() } }
        retained.forEach { it.requestClose() }
    }

    override fun close() {
        synchronized(sessions) { stopped = true }
        clear()
        reaper.shutdownNow()
        workers.shutdown()
        cleanup.shutdown()
    }

    private inner class Session(val owner: Int) {
        val id = UUID.randomUUID().toString()
        val lock = ReentrantLock()
        val closed = AtomicBoolean()
        val busy = AtomicBoolean()
        private val cleanupQueued = AtomicBoolean()
        @Volatile var touched = now()
        var handle: DroidGuardHandle? = null

        fun requestClose() {
            closed.set(true)
            // A running operation owns the native handle and queues cleanup in its finally block.
            if (lock.tryLock()) {
                lock.unlock()
                enqueueCleanup()
            }
        }

        fun enqueueCleanup() {
            if (!cleanupQueued.compareAndSet(false, true)) return
            val release = Runnable {
                lock.withLock {
                    try {
                        handle?.close()
                    } catch (_: Exception) {
                        // The unusable session stays retired even if native cleanup reports an error.
                    } finally {
                        handle = null
                        capacity.release()
                    }
                }
            }
            try {
                cleanup.execute(release)
            } catch (_: RejectedExecutionException) {
                // Only teardown can reject: retained sessions also bound the cleanup queue.
                release.run()
            }
        }
    }

    companion object {
        private fun threads(name: String) = ThreadFactory { runnable ->
            Thread(runnable, name).apply { isDaemon = true }
        }
    }
}
