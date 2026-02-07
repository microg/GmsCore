/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsRetryPolicy - Intelligent retry policies for resilience
 */

package org.microg.gms.rcs.retry

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

sealed class RetryPolicy {
    abstract suspend fun <T> execute(block: suspend () -> T): RetryResult<T>
    
    data class Exponential(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 60000,
        val multiplier: Double = 2.0,
        val jitterFactor: Double = 0.2,
        val retryOn: (Throwable) -> Boolean = { true }
    ) : RetryPolicy() {
        
        override suspend fun <T> execute(block: suspend () -> T): RetryResult<T> {
            var lastException: Throwable? = null
            
            repeat(maxAttempts) { attempt ->
                try {
                    val result = block()
                    return RetryResult.Success(result, attempt + 1)
                } catch (e: Throwable) {
                    lastException = e
                    
                    if (!retryOn(e) || attempt == maxAttempts - 1) {
                        return RetryResult.Failure(e, attempt + 1)
                    }
                    
                    val baseDelay = initialDelayMs * multiplier.pow(attempt.toDouble())
                    val jitter = baseDelay * jitterFactor * (Math.random() * 2 - 1)
                    val actualDelay = min((baseDelay + jitter).toLong(), maxDelayMs)
                    
                    delay(actualDelay)
                }
            }
            
            return RetryResult.Failure(lastException ?: Exception("Unknown error"), maxAttempts)
        }
    }
    
    data class Linear(
        val maxAttempts: Int = 3,
        val delayMs: Long = 1000
    ) : RetryPolicy() {
        
        override suspend fun <T> execute(block: suspend () -> T): RetryResult<T> {
            var lastException: Throwable? = null
            
            repeat(maxAttempts) { attempt ->
                try {
                    val result = block()
                    return RetryResult.Success(result, attempt + 1)
                } catch (e: Throwable) {
                    lastException = e
                    
                    if (attempt < maxAttempts - 1) {
                        delay(delayMs)
                    }
                }
            }
            
            return RetryResult.Failure(lastException ?: Exception("Unknown error"), maxAttempts)
        }
    }
    
    object NoRetry : RetryPolicy() {
        override suspend fun <T> execute(block: suspend () -> T): RetryResult<T> {
            return try {
                RetryResult.Success(block(), 1)
            } catch (e: Throwable) {
                RetryResult.Failure(e, 1)
            }
        }
    }
}

sealed class RetryResult<out T> {
    abstract val attempts: Int
    
    data class Success<T>(val value: T, override val attempts: Int) : RetryResult<T>()
    data class Failure(val exception: Throwable, override val attempts: Int) : RetryResult<Nothing>()
    
    fun getOrNull(): T? = (this as? Success)?.value
    
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw exception
    }
    
    inline fun <R> map(transform: (T) -> R): RetryResult<R> = when (this) {
        is Success -> Success(transform(value), attempts)
        is Failure -> this
    }
    
    inline fun onSuccess(action: (T) -> Unit): RetryResult<T> {
        if (this is Success) action(value)
        return this
    }
    
    inline fun onFailure(action: (Throwable) -> Unit): RetryResult<T> {
        if (this is Failure) action(exception)
        return this
    }
}

object RetryPolicies {
    
    val networkRetry = RetryPolicy.Exponential(
        maxAttempts = 5,
        initialDelayMs = 500,
        maxDelayMs = 30000,
        multiplier = 2.0,
        retryOn = { e ->
            e is java.io.IOException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException
        }
    )
    
    val registrationRetry = RetryPolicy.Exponential(
        maxAttempts = 3,
        initialDelayMs = 2000,
        maxDelayMs = 60000,
        multiplier = 3.0
    )
    
    val messageSendRetry = RetryPolicy.Exponential(
        maxAttempts = 3,
        initialDelayMs = 1000,
        maxDelayMs = 10000,
        multiplier = 2.0
    )
    
    val fileTransferRetry = RetryPolicy.Exponential(
        maxAttempts = 5,
        initialDelayMs = 2000,
        maxDelayMs = 120000,
        multiplier = 2.5
    )
    
    val quickRetry = RetryPolicy.Linear(
        maxAttempts = 2,
        delayMs = 500
    )
}

suspend inline fun <T> withRetry(
    policy: RetryPolicy = RetryPolicies.networkRetry,
    crossinline block: suspend () -> T
): T {
    return policy.execute { block() }.getOrThrow()
}

suspend inline fun <T> withRetryOrNull(
    policy: RetryPolicy = RetryPolicies.networkRetry,
    crossinline block: suspend () -> T
): T? {
    return policy.execute { block() }.getOrNull()
}
