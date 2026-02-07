/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsLogger - Structured logging with levels and tags
 */

package org.microg.gms.rcs.logging

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

object RcsLogger {
    
    private const val MAX_LOG_ENTRIES = 1000
    private const val TAG_PREFIX = "Rcs"
    
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val isDebugEnabled = AtomicBoolean(true)
    private val isVerboseEnabled = AtomicBoolean(false)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun v(tag: String, message: String) {
        if (isVerboseEnabled.get()) {
            log(LogLevel.VERBOSE, tag, message)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebugEnabled.get()) {
            log(LogLevel.DEBUG, tag, message)
        }
    }

    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARNING, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX$tag"
        val timestamp = System.currentTimeMillis()
        
        when (level) {
            LogLevel.VERBOSE -> Log.v(fullTag, message)
            LogLevel.DEBUG -> Log.d(fullTag, message)
            LogLevel.INFO -> Log.i(fullTag, message)
            LogLevel.WARNING -> {
                if (throwable != null) Log.w(fullTag, message, throwable)
                else Log.w(fullTag, message)
            }
            LogLevel.ERROR -> {
                if (throwable != null) Log.e(fullTag, message, throwable)
                else Log.e(fullTag, message)
            }
        }
        
        val entry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message,
            throwableMessage = throwable?.toString()
        )
        
        logBuffer.add(entry)
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            logBuffer.poll()
        }
    }

    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled.set(enabled)
    }

    fun setVerboseEnabled(enabled: Boolean) {
        isVerboseEnabled.set(enabled)
    }

    fun getLogHistory(level: LogLevel? = null, limit: Int = 100): List<LogEntry> {
        return logBuffer
            .filter { level == null || it.level == level }
            .takeLast(limit)
    }

    fun exportLogs(): String {
        val builder = StringBuilder()
        builder.appendLine("=== RCS Log Export ===")
        builder.appendLine("Exported at: ${dateFormat.format(Date())}")
        builder.appendLine("Total entries: ${logBuffer.size}")
        builder.appendLine()
        
        logBuffer.forEach { entry ->
            val formattedTime = dateFormat.format(Date(entry.timestamp))
            builder.appendLine("[$formattedTime] [${entry.level}] [${entry.tag}] ${entry.message}")
            entry.throwableMessage?.let { builder.appendLine("  Exception: $it") }
        }
        
        return builder.toString()
    }

    fun clearLogs() {
        logBuffer.clear()
    }
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwableMessage: String? = null
)

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
