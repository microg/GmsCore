/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.os.SystemClock
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private const val CHECK_INTERVAL_MILLIS = 500L

internal class DirectTransferTimeout(
    initialTimeoutMillis: Long,
    private val onTimeout: (Long) -> Unit,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "DirectTransfer-Timeout").apply { isDaemon = true }
    }
    private var active = true
    private var paused = false
    private var running = false
    private var lastActivityElapsedRealtime = 0L
    private var timeoutMillis = initialTimeoutMillis
    private var checkFuture: ScheduledFuture<*>? = null

    private val check = object : Runnable {
        override fun run() {
            val expiredTimeout = synchronized(this@DirectTransferTimeout) {
                if (!running || !active || paused) return
                val elapsed = SystemClock.elapsedRealtime() - lastActivityElapsedRealtime
                if (elapsed > timeoutMillis) {
                    running = false
                    checkFuture = null
                    timeoutMillis
                } else {
                    scheduleNextLocked()
                    null
                }
            }
            expiredTimeout?.let(onTimeout)
        }
    }

    @Synchronized
    fun start() {
        if (active && !paused) startLocked()
    }

    @Synchronized
    fun pause() {
        paused = true
        stopLocked()
    }

    @Synchronized
    fun resume() {
        paused = false
        if (active) startLocked()
    }

    @Synchronized
    fun markMessageActivity() {
        lastActivityElapsedRealtime = SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun updateTimeout(value: Long) {
        require(value > 0L) { "DirectTransfer timeout must be positive" }
        timeoutMillis = value
    }

    @Synchronized
    fun close() {
        active = false
        stopLocked()
        scheduler.shutdownNow()
    }

    private fun startLocked() {
        checkFuture?.cancel(false)
        lastActivityElapsedRealtime = SystemClock.elapsedRealtime()
        running = true
        scheduleNextLocked()
    }

    private fun stopLocked() {
        checkFuture?.cancel(false)
        checkFuture = null
        running = false
    }

    private fun scheduleNextLocked() {
        checkFuture = scheduler.schedule(check, CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
    }
}
