/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsMetricsCollector - Analytics and telemetry
 */

package org.microg.gms.rcs.metrics

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RcsMetricsCollector private constructor(context: Context) {

    companion object {
        private const val TAG = "RcsMetrics"
        
        @Volatile
        private var instance: RcsMetricsCollector? = null
        
        fun getInstance(context: Context): RcsMetricsCollector {
            return instance ?: synchronized(this) {
                instance ?: RcsMetricsCollector(context.applicationContext).also { instance = it }
            }
        }
    }

    private val counters = ConcurrentHashMap<String, AtomicLong>()
    private val timers = ConcurrentHashMap<String, TimingMetric>()
    private val gauges = ConcurrentHashMap<String, AtomicLong>()

    fun incrementCounter(name: String, delta: Long = 1) {
        counters.getOrPut(name) { AtomicLong(0) }.addAndGet(delta)
    }

    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0
    }

    fun startTimer(name: String): TimerHandle {
        val startTime = System.nanoTime()
        return TimerHandle(name, startTime)
    }

    fun recordTiming(name: String, durationMs: Long) {
        val metric = timers.getOrPut(name) { TimingMetric(name) }
        metric.record(durationMs)
    }

    fun setGauge(name: String, value: Long) {
        gauges.getOrPut(name) { AtomicLong(0) }.set(value)
    }

    fun getGauge(name: String): Long {
        return gauges[name]?.get() ?: 0
    }

    fun getTimingStats(name: String): TimingStats? {
        return timers[name]?.getStats()
    }

    fun getAllMetrics(): MetricsSnapshot {
        return MetricsSnapshot(
            counters = counters.mapValues { it.value.get() },
            gauges = gauges.mapValues { it.value.get() },
            timings = timers.mapValues { it.value.getStats() }
        )
    }

    fun reset() {
        counters.clear()
        timers.clear()
        gauges.clear()
    }

    inner class TimerHandle(private val name: String, private val startTime: Long) {
        fun stop() {
            val durationNs = System.nanoTime() - startTime
            val durationMs = durationNs / 1_000_000
            recordTiming(name, durationMs)
        }
    }
}

class TimingMetric(val name: String) {
    private var count: Long = 0
    private var totalMs: Long = 0
    private var minMs: Long = Long.MAX_VALUE
    private var maxMs: Long = Long.MIN_VALUE
    
    @Synchronized
    fun record(durationMs: Long) {
        count++
        totalMs += durationMs
        if (durationMs < minMs) minMs = durationMs
        if (durationMs > maxMs) maxMs = durationMs
    }
    
    @Synchronized
    fun getStats(): TimingStats {
        return TimingStats(
            count = count,
            totalMs = totalMs,
            avgMs = if (count > 0) totalMs / count else 0,
            minMs = if (count > 0) minMs else 0,
            maxMs = if (count > 0) maxMs else 0
        )
    }
}

data class TimingStats(
    val count: Long,
    val totalMs: Long,
    val avgMs: Long,
    val minMs: Long,
    val maxMs: Long
)

data class MetricsSnapshot(
    val counters: Map<String, Long>,
    val gauges: Map<String, Long>,
    val timings: Map<String, TimingStats>
)

object RcsMetricNames {
    const val MESSAGES_SENT = "rcs.messages.sent"
    const val MESSAGES_RECEIVED = "rcs.messages.received"
    const val MESSAGES_FAILED = "rcs.messages.failed"
    const val REGISTRATION_ATTEMPTS = "rcs.registration.attempts"
    const val REGISTRATION_SUCCESS = "rcs.registration.success"
    const val CAPABILITY_QUERIES = "rcs.capability.queries"
    const val FILE_TRANSFERS = "rcs.file.transfers"
    const val NETWORK_ERRORS = "rcs.network.errors"
    
    const val TIMER_REGISTRATION = "rcs.timer.registration"
    const val TIMER_MESSAGE_SEND = "rcs.timer.message.send"
    const val TIMER_CAPABILITY_QUERY = "rcs.timer.capability.query"
    
    const val GAUGE_ACTIVE_CONNECTIONS = "rcs.gauge.connections"
    const val GAUGE_QUEUE_SIZE = "rcs.gauge.queue.size"
}
