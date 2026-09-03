/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.threads.internal

import androidx.annotation.Keep
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Keep
object GlobalExecutorsImpl {
    private val lowPool = ThreadPoolExecutor(
        4, Int.MAX_VALUE, 10L, TimeUnit.SECONDS, SynchronousQueue(),
        NamedThreadFactory("lowpool", Thread.MAX_PRIORITY)
    )

    private val highPool = ThreadPoolExecutor(
        4, Int.MAX_VALUE, 10L, TimeUnit.SECONDS, SynchronousQueue(),
        NamedThreadFactory("highpool", Thread.NORM_PRIORITY)
    )

    private val activePool = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(), Int.MAX_VALUE, 10L, TimeUnit.SECONDS, SynchronousQueue(),
        NamedThreadFactory("actvpool", Thread.MIN_PRIORITY)
    )

    @Keep
    @JvmStatic
    fun getPool(priority: Int): ThreadPoolExecutor {
        return when (priority) {
            0 -> activePool
            9 -> highPool
            10 -> lowPool
            else -> throw IllegalArgumentException("Unexpected priority $priority")
        }
    }

    private class NamedThreadFactory(private val namePrefix: String, private val priority: Int) :
        ThreadFactory {
        private var count = 0

        override fun newThread(r: Runnable): Thread {
            val t = Thread(r, namePrefix + "-" + (++count))
            try {
                t.priority = priority
            } catch (ignored: Exception) {
            }
            return t
        }
    }
}
