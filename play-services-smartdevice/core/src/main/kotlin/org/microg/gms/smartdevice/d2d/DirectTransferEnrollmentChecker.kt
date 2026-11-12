/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.accounts.Account
import android.os.Looper
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "DirectTransferEnroll"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"
private const val ENROLLMENT_CACHE_MILLIS = 600_000L
private const val ENROLLMENT_TIMEOUT_MILLIS = 30_000L

class DirectTransferEnrollmentChecker internal constructor(
    private val registerDirectTransferKeys: (Account, Boolean, (Boolean) -> Unit) -> Unit,
) {
    @Volatile private var registrationLatch: CountDownLatch? = null
    @Volatile private var registrationDispatchedAtMillis: Long = 0L
    @Volatile private var registrationSucceeded: AtomicBoolean? = null

    @Synchronized
    fun forceRegister(accounts: List<Account>, fidoFlow: Boolean): Boolean {
        val latch = CountDownLatch(accounts.size)
        val succeeded = AtomicBoolean(true)
        val now = System.currentTimeMillis()
        var registrationStarted = false
        accounts.forEachIndexed { index, account ->
            if (account.type != GOOGLE_ACCOUNT_TYPE) {
                succeeded.set(false)
                latch.countDown()
                return@forEachIndexed
            }
            val shouldRegister = fidoFlow || synchronized(attemptCacheLock) {
                val previousAttempt = attemptByAccountName[account.name]
                if (previousAttempt != null && now - previousAttempt < ENROLLMENT_CACHE_MILLIS) {
                    false
                } else {
                    true
                }
            }
            if (!shouldRegister) {
                latch.countDown()
                return@forEachIndexed
            }
            val operationCompleted = AtomicBoolean(false)
            fun countDownOnce() {
                if (operationCompleted.compareAndSet(false, true)) {
                    latch.countDown()
                }
            }
            try {
                registerDirectTransferKeys(
                    account,
                    fidoFlow,
                ) { success ->
                        if (!success) {
                            succeeded.set(false)
                            Log.w(TAG, "CryptAuth ForceRegistration failed accountIndex=$index")
                        } else if (!fidoFlow) {
                            synchronized(attemptCacheLock) {
                                attemptByAccountName[account.name] = System.currentTimeMillis()
                            }
                        }
                        countDownOnce()
                    }
                registrationStarted = true
            } catch (e: Exception) {
                succeeded.set(false)
                Log.e(TAG, "Unable to start ForceRegistration accountIndex=$index", e)
                countDownOnce()
            }
        }
        registrationDispatchedAtMillis = System.currentTimeMillis()
        registrationSucceeded = succeeded
        registrationLatch = latch
        return registrationStarted
    }

    fun awaitCompletion(): Boolean {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "CryptAuth registration must not be awaited on the main thread"
        }
        val latch = registrationLatch ?: return registrationSucceeded?.get() == true
        val succeeded = registrationSucceeded ?: return false
        val elapsedSinceDispatch = (
            System.currentTimeMillis() - registrationDispatchedAtMillis
        ).coerceAtLeast(0L)
        val remainingWaitMillis = (
            ENROLLMENT_TIMEOUT_MILLIS - elapsedSinceDispatch
        ).coerceAtLeast(0L)
        var completed = false
        try {
            completed = latch.await(remainingWaitMillis, TimeUnit.MILLISECONDS)
            if (!completed) {
                Log.w(TAG, "Timed out waiting for CryptAuth ForceRegistration elapsedSinceDispatch=$elapsedSinceDispatch")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w(TAG, "Interrupted while waiting for CryptAuth ForceRegistration", e)
        } finally {
            synchronized(this) {
                if (registrationLatch === latch) registrationLatch = null
            }
        }
        return completed && succeeded.get()
    }

    private companion object {
        val attemptCacheLock = Any()
        val attemptByAccountName = mutableMapOf<String, Long>()
    }
}
