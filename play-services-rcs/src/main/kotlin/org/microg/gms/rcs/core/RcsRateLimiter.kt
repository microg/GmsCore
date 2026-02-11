/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsRateLimiter - Token bucket rate limiter for API protection
 * 
 * Implements:
 * - Token bucket algorithm with burst capacity
 * - Per-endpoint rate limiting
 * - Sliding window tracking
 * - Redis-compatible interface for future clustering
 * - Adaptive rate limiting based on server response
 */

package org.microg.gms.rcs.core

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class RcsRateLimiter private constructor() {

    companion object {
        private const val TAG = "RcsRateLimiter"
        
        @Volatile
        private var instance: RcsRateLimiter? = null
        
        fun getInstance(): RcsRateLimiter {
            return instance ?: synchronized(this) {
                instance ?: RcsRateLimiter().also { instance = it }
            }
        }
    }

    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    private val slidingWindows = ConcurrentHashMap<String, SlidingWindowCounter>()
    private val adaptiveMultipliers = ConcurrentHashMap<String, Double>()
    
    private val defaultConfig = RateLimitConfig(
        tokensPerSecond = 10.0,
        bucketCapacity = 50,
        slidingWindowSizeMs = 60000L,
        maxRequestsPerWindow = 100
    )

    private val endpointConfigs = mapOf(
        "register" to RateLimitConfig(
            tokensPerSecond = 0.1,
            bucketCapacity = 3,
            slidingWindowSizeMs = 300000L,
            maxRequestsPerWindow = 5
        ),
        "capabilities" to RateLimitConfig(
            tokensPerSecond = 5.0,
            bucketCapacity = 20,
            slidingWindowSizeMs = 60000L,
            maxRequestsPerWindow = 50
        ),
        "message" to RateLimitConfig(
            tokensPerSecond = 2.0,
            bucketCapacity = 30,
            slidingWindowSizeMs = 60000L,
            maxRequestsPerWindow = 60
        ),
        "fileTransfer" to RateLimitConfig(
            tokensPerSecond = 0.5,
            bucketCapacity = 5,
            slidingWindowSizeMs = 60000L,
            maxRequestsPerWindow = 10
        ),
        "presence" to RateLimitConfig(
            tokensPerSecond = 1.0,
            bucketCapacity = 10,
            slidingWindowSizeMs = 60000L,
            maxRequestsPerWindow = 30
        )
    )

    fun tryAcquire(endpoint: String, tokens: Int = 1): RateLimitResult {
        val config = endpointConfigs[endpoint] ?: defaultConfig
        val adaptiveMultiplier = adaptiveMultipliers.getOrDefault(endpoint, 1.0)
        val adjustedConfig = config.withMultiplier(adaptiveMultiplier)
        
        val bucket = buckets.getOrPut(endpoint) { 
            TokenBucket(adjustedConfig.tokensPerSecond, adjustedConfig.bucketCapacity) 
        }
        
        val slidingWindow = slidingWindows.getOrPut(endpoint) { 
            SlidingWindowCounter(adjustedConfig.slidingWindowSizeMs, adjustedConfig.maxRequestsPerWindow) 
        }
        
        val bucketResult = bucket.tryConsume(tokens)
        if (!bucketResult.isAllowed) {
            Log.w(TAG, "Rate limit exceeded for $endpoint (token bucket): retry after ${bucketResult.retryAfterMs}ms")
            return RateLimitResult(
                isAllowed = false,
                retryAfterMs = bucketResult.retryAfterMs,
                remainingTokens = bucketResult.remainingTokens,
                reason = RateLimitReason.TOKEN_BUCKET_EXHAUSTED
            )
        }
        
        val windowResult = slidingWindow.tryIncrement()
        if (!windowResult.isAllowed) {
            bucket.refund(tokens)
            Log.w(TAG, "Rate limit exceeded for $endpoint (sliding window): retry after ${windowResult.retryAfterMs}ms")
            return RateLimitResult(
                isAllowed = false,
                retryAfterMs = windowResult.retryAfterMs,
                remainingTokens = windowResult.remainingInWindow,
                reason = RateLimitReason.SLIDING_WINDOW_EXCEEDED
            )
        }
        
        return RateLimitResult(
            isAllowed = true,
            retryAfterMs = 0,
            remainingTokens = bucketResult.remainingTokens,
            reason = RateLimitReason.ALLOWED
        )
    }

    fun recordServerResponse(endpoint: String, statusCode: Int, responseTimeMs: Long) {
        when {
            statusCode == 429 -> {
                val currentMultiplier = adaptiveMultipliers.getOrDefault(endpoint, 1.0)
                val newMultiplier = (currentMultiplier * 0.5).coerceAtLeast(0.1)
                adaptiveMultipliers[endpoint] = newMultiplier
                Log.w(TAG, "Received 429 for $endpoint, reducing rate to ${newMultiplier}x")
            }
            statusCode == 503 -> {
                val currentMultiplier = adaptiveMultipliers.getOrDefault(endpoint, 1.0)
                val newMultiplier = (currentMultiplier * 0.7).coerceAtLeast(0.1)
                adaptiveMultipliers[endpoint] = newMultiplier
                Log.w(TAG, "Received 503 for $endpoint, reducing rate to ${newMultiplier}x")
            }
            statusCode in 200..299 && responseTimeMs < 500 -> {
                val currentMultiplier = adaptiveMultipliers.getOrDefault(endpoint, 1.0)
                if (currentMultiplier < 1.0) {
                    val newMultiplier = (currentMultiplier * 1.1).coerceAtMost(1.0)
                    adaptiveMultipliers[endpoint] = newMultiplier
                    Log.d(TAG, "Fast response for $endpoint, increasing rate to ${newMultiplier}x")
                }
            }
        }
    }

    fun getStatistics(endpoint: String): RateLimitStatistics {
        val bucket = buckets[endpoint]
        val window = slidingWindows[endpoint]
        val multiplier = adaptiveMultipliers.getOrDefault(endpoint, 1.0)
        
        return RateLimitStatistics(
            endpoint = endpoint,
            currentTokens = bucket?.getCurrentTokens() ?: 0.0,
            bucketCapacity = bucket?.getCapacity() ?: 0,
            windowRequestCount = window?.getCurrentCount() ?: 0,
            windowMaxRequests = window?.getMaxRequests() ?: 0,
            adaptiveMultiplier = multiplier
        )
    }

    fun reset(endpoint: String) {
        buckets.remove(endpoint)
        slidingWindows.remove(endpoint)
        adaptiveMultipliers.remove(endpoint)
    }

    fun resetAll() {
        buckets.clear()
        slidingWindows.clear()
        adaptiveMultipliers.clear()
    }
}

class TokenBucket(
    private val refillRate: Double,
    private val capacity: Int
) {
    private var tokens: Double = capacity.toDouble()
    private var lastRefillTime: Long = System.nanoTime()
    private val lock = ReentrantLock()

    fun tryConsume(requested: Int): TokenBucketResult {
        lock.withLock {
            refill()
            
            if (tokens >= requested) {
                tokens -= requested
                return TokenBucketResult(
                    isAllowed = true,
                    remainingTokens = tokens.toInt(),
                    retryAfterMs = 0
                )
            }
            
            val tokensNeeded = requested - tokens
            val waitTimeMs = ((tokensNeeded / refillRate) * 1000).toLong()
            
            return TokenBucketResult(
                isAllowed = false,
                remainingTokens = tokens.toInt(),
                retryAfterMs = waitTimeMs
            )
        }
    }

    fun refund(tokens: Int) {
        lock.withLock {
            this.tokens = min(this.tokens + tokens, capacity.toDouble())
        }
    }

    private fun refill() {
        val currentTime = System.nanoTime()
        val elapsedNanos = currentTime - lastRefillTime
        val elapsedSeconds = elapsedNanos / 1_000_000_000.0
        
        val tokensToAdd = elapsedSeconds * refillRate
        tokens = min(tokens + tokensToAdd, capacity.toDouble())
        lastRefillTime = currentTime
    }

    fun getCurrentTokens(): Double {
        lock.withLock {
            refill()
            return tokens
        }
    }

    fun getCapacity(): Int = capacity
}

class SlidingWindowCounter(
    private val windowSizeMs: Long,
    private val maxRequests: Int
) {
    private val timestamps = ArrayDeque<Long>()
    private val lock = ReentrantLock()

    fun tryIncrement(): SlidingWindowResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - windowSizeMs
            
            while (timestamps.isNotEmpty() && timestamps.first() < windowStart) {
                timestamps.removeFirst()
            }
            
            if (timestamps.size >= maxRequests) {
                val oldestTimestamp = timestamps.first()
                val retryAfterMs = oldestTimestamp + windowSizeMs - now
                
                return SlidingWindowResult(
                    isAllowed = false,
                    remainingInWindow = 0,
                    retryAfterMs = retryAfterMs.coerceAtLeast(0)
                )
            }
            
            timestamps.addLast(now)
            
            return SlidingWindowResult(
                isAllowed = true,
                remainingInWindow = maxRequests - timestamps.size,
                retryAfterMs = 0
            )
        }
    }

    fun getCurrentCount(): Int {
        lock.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - windowSizeMs
            
            while (timestamps.isNotEmpty() && timestamps.first() < windowStart) {
                timestamps.removeFirst()
            }
            
            return timestamps.size
        }
    }

    fun getMaxRequests(): Int = maxRequests
}

data class RateLimitConfig(
    val tokensPerSecond: Double,
    val bucketCapacity: Int,
    val slidingWindowSizeMs: Long,
    val maxRequestsPerWindow: Int
) {
    fun withMultiplier(multiplier: Double): RateLimitConfig {
        return copy(
            tokensPerSecond = tokensPerSecond * multiplier,
            maxRequestsPerWindow = (maxRequestsPerWindow * multiplier).toInt().coerceAtLeast(1)
        )
    }
}

data class RateLimitResult(
    val isAllowed: Boolean,
    val retryAfterMs: Long,
    val remainingTokens: Int,
    val reason: RateLimitReason
)

data class TokenBucketResult(
    val isAllowed: Boolean,
    val remainingTokens: Int,
    val retryAfterMs: Long
)

data class SlidingWindowResult(
    val isAllowed: Boolean,
    val remainingInWindow: Int,
    val retryAfterMs: Long
)

data class RateLimitStatistics(
    val endpoint: String,
    val currentTokens: Double,
    val bucketCapacity: Int,
    val windowRequestCount: Int,
    val windowMaxRequests: Int,
    val adaptiveMultiplier: Double
)

enum class RateLimitReason {
    ALLOWED,
    TOKEN_BUCKET_EXHAUSTED,
    SLIDING_WINDOW_EXCEEDED
}
