/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsErrorHandler - Centralized error handling and recovery
 * 
 * Provides:
 * - Error classification and categorization
 * - Automatic recovery strategies
 * - Error reporting and aggregation
 * - Circuit breaker integration
 * - User-friendly error messages
 */

package org.microg.gms.rcs.error

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.microg.gms.rcs.events.ErrorEvent
import org.microg.gms.rcs.events.RcsEventBus
import org.microg.gms.rcs.metrics.RcsMetricNames
import org.microg.gms.rcs.metrics.RcsMetricsCollector
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLException

class RcsErrorHandler private constructor() {

    companion object {
        private const val TAG = "RcsErrorHandler"
        
        @Volatile
        private var instance: RcsErrorHandler? = null
        
        fun getInstance(): RcsErrorHandler {
            return instance ?: synchronized(this) {
                instance ?: RcsErrorHandler().also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventBus = RcsEventBus.getInstance()
    
    private val errorCounts = ConcurrentHashMap<ErrorCategory, AtomicInteger>()
    private val recentErrors = CopyOnWriteArrayList<RcsError>()
    private val errorListeners = CopyOnWriteArrayList<ErrorListener>()
    
    private val maxRecentErrors = 100

    fun handleException(
        exception: Throwable,
        component: String,
        context: Map<String, Any> = emptyMap()
    ): RcsError {
        val error = classifyException(exception, component, context)
        recordError(error)
        
        Log.e(TAG, "Error in $component: ${error.message}", exception)
        
        coroutineScope.launch {
            eventBus.publish(ErrorEvent(
                errorCode = error.code,
                errorMessage = error.message,
                component = component
            ))
        }
        
        notifyListeners(error)
        
        return error
    }

    fun handleError(
        errorCode: String,
        message: String,
        component: String,
        category: ErrorCategory = ErrorCategory.UNKNOWN,
        recoverable: Boolean = true,
        context: Map<String, Any> = emptyMap()
    ): RcsError {
        val error = RcsError(
            code = errorCode,
            message = message,
            category = category,
            component = component,
            isRecoverable = recoverable,
            timestamp = System.currentTimeMillis(),
            context = context
        )
        
        recordError(error)
        Log.e(TAG, "Error in $component: [$errorCode] $message")
        
        coroutineScope.launch {
            eventBus.publish(ErrorEvent(errorCode, message, component))
        }
        
        notifyListeners(error)
        
        return error
    }

    private fun classifyException(
        exception: Throwable,
        component: String,
        context: Map<String, Any>
    ): RcsError {
        val (category, code, message, recoverable) = when (exception) {
            is SocketTimeoutException -> ErrorClassification(
                ErrorCategory.NETWORK,
                "NETWORK_TIMEOUT",
                "The connection timed out. Please check your internet connection.",
                true
            )
            is UnknownHostException -> ErrorClassification(
                ErrorCategory.NETWORK,
                "NETWORK_UNREACHABLE",
                "Unable to reach the server. Please check your internet connection.",
                true
            )
            is SSLException -> ErrorClassification(
                ErrorCategory.SECURITY,
                "SSL_ERROR",
                "A secure connection could not be established.",
                true
            )
            is IOException -> ErrorClassification(
                ErrorCategory.NETWORK,
                "IO_ERROR",
                "A network error occurred: ${exception.message}",
                true
            )
            is SecurityException -> ErrorClassification(
                ErrorCategory.PERMISSION,
                "PERMISSION_DENIED",
                "Required permission was denied.",
                false
            )
            is IllegalStateException -> ErrorClassification(
                ErrorCategory.STATE,
                "INVALID_STATE",
                "Operation not allowed in current state.",
                true
            )
            is IllegalArgumentException -> ErrorClassification(
                ErrorCategory.VALIDATION,
                "INVALID_ARGUMENT",
                "Invalid input: ${exception.message}",
                false
            )
            is OutOfMemoryError -> ErrorClassification(
                ErrorCategory.RESOURCE,
                "OUT_OF_MEMORY",
                "Insufficient memory to complete operation.",
                false
            )
            else -> ErrorClassification(
                ErrorCategory.UNKNOWN,
                "UNKNOWN_ERROR",
                exception.message ?: "An unexpected error occurred.",
                true
            )
        }
        
        return RcsError(
            code = code,
            message = message,
            category = category,
            component = component,
            isRecoverable = recoverable,
            timestamp = System.currentTimeMillis(),
            context = context,
            cause = exception
        )
    }

    private fun recordError(error: RcsError) {
        errorCounts.getOrPut(error.category) { AtomicInteger(0) }.incrementAndGet()
        
        recentErrors.add(error)
        
        while (recentErrors.size > maxRecentErrors) {
            recentErrors.removeAt(0)
        }
    }

    fun getRecoveryStrategy(error: RcsError): RecoveryStrategy {
        if (!error.isRecoverable) {
            return RecoveryStrategy.None
        }
        
        return when (error.category) {
            ErrorCategory.NETWORK -> RecoveryStrategy.Retry(
                maxAttempts = 3,
                initialDelayMs = 1000,
                maxDelayMs = 30000,
                backoffMultiplier = 2.0
            )
            ErrorCategory.AUTHENTICATION -> RecoveryStrategy.Reauthenticate
            ErrorCategory.REGISTRATION -> RecoveryStrategy.Reregister
            ErrorCategory.STATE -> RecoveryStrategy.ResetState
            ErrorCategory.RATE_LIMIT -> RecoveryStrategy.Backoff(
                delayMs = 60000
            )
            else -> RecoveryStrategy.Retry(
                maxAttempts = 1,
                initialDelayMs = 5000,
                maxDelayMs = 5000,
                backoffMultiplier = 1.0
            )
        }
    }

    fun getErrorStatistics(): ErrorStatistics {
        return ErrorStatistics(
            totalErrors = recentErrors.size,
            errorsByCategory = errorCounts.mapValues { it.value.get() },
            recentErrors = recentErrors.takeLast(10)
        )
    }

    fun clearErrorHistory() {
        recentErrors.clear()
        errorCounts.clear()
    }

    fun addErrorListener(listener: ErrorListener) {
        errorListeners.add(listener)
    }

    fun removeErrorListener(listener: ErrorListener) {
        errorListeners.remove(listener)
    }

    private fun notifyListeners(error: RcsError) {
        errorListeners.forEach { listener ->
            try {
                listener.onError(error)
            } catch (e: Exception) {
                Log.e(TAG, "Error listener threw exception", e)
            }
        }
    }
}

data class RcsError(
    val code: String,
    val message: String,
    val category: ErrorCategory,
    val component: String,
    val isRecoverable: Boolean,
    val timestamp: Long,
    val context: Map<String, Any> = emptyMap(),
    val cause: Throwable? = null
) {
    fun getUserFriendlyMessage(): String {
        return when (category) {
            ErrorCategory.NETWORK -> "Network connection issue. Please check your internet."
            ErrorCategory.AUTHENTICATION -> "Authentication failed. Please try again."
            ErrorCategory.REGISTRATION -> "Unable to register for messaging. Retry later."
            ErrorCategory.PERMISSION -> "Required permission not granted."
            ErrorCategory.SECURITY -> "Security error. Please update the app."
            else -> message
        }
    }
}

enum class ErrorCategory {
    NETWORK,
    AUTHENTICATION,
    REGISTRATION,
    PERMISSION,
    SECURITY,
    VALIDATION,
    STATE,
    RESOURCE,
    RATE_LIMIT,
    CONFIGURATION,
    UNKNOWN
}

private data class ErrorClassification(
    val category: ErrorCategory,
    val code: String,
    val message: String,
    val recoverable: Boolean
)

sealed class RecoveryStrategy {
    object None : RecoveryStrategy()
    
    data class Retry(
        val maxAttempts: Int,
        val initialDelayMs: Long,
        val maxDelayMs: Long,
        val backoffMultiplier: Double
    ) : RecoveryStrategy()
    
    data class Backoff(val delayMs: Long) : RecoveryStrategy()
    
    object Reauthenticate : RecoveryStrategy()
    object Reregister : RecoveryStrategy()
    object ResetState : RecoveryStrategy()
}

data class ErrorStatistics(
    val totalErrors: Int,
    val errorsByCategory: Map<ErrorCategory, Int>,
    val recentErrors: List<RcsError>
)

interface ErrorListener {
    fun onError(error: RcsError)
}
