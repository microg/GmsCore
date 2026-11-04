/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.google.android.gms.droidguard.DroidGuardHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DroidGuardSessionStoreTest {
    @Test
    fun beginDoesNotPublishSessionClearedDuringInitialization() {
        val openStarted = CountDownLatch(1)
        val finishOpen = CountDownLatch(1)
        val handle = object : DroidGuardHandle {
            @Volatile private var opened = true
            override fun isOpened(): Boolean = opened
            override fun snapshot(data: Map<String, String>): String = "unused"
            override fun close() {
                opened = false
            }
        }

        DroidGuardSessionStore(
            openHandle = { _, _, _ ->
                openStarted.countDown()
                check(finishOpen.await(5, TimeUnit.SECONDS)) { "initialization did not unblock" }
                handle
            },
            operationTimeoutMillis = 5_000,
            idleTimeoutMillis = 30_000,
            maxSessions = 1
        ).use { store ->
            val callers = Executors.newSingleThreadExecutor()
            try {
                val begin = callers.submit<String> {
                    store.begin(12, "integrity", "test", emptyMap())
                }
                assertTrue("native initialization must start", openStarted.await(5, TimeUnit.SECONDS))

                store.clear()
                finishOpen.countDown()

                try {
                    begin.get(5, TimeUnit.SECONDS)
                    fail("clear must prevent publishing an already-retired session ID")
                } catch (e: java.util.concurrent.ExecutionException) {
                    val cause = e.cause as? DroidGuardSessionException ?: throw e
                    assertEquals(404, cause.code)
                }
            } finally {
                finishOpen.countDown()
                callers.shutdownNow()
                callers.awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }

    @Test
    fun reaperRetainsBusyNativeSnapshotUntilCompletionThenExpiresIdle() {
        val clock = AtomicLong(0L)
        val snapshotStarted = CountDownLatch(1)
        val finishSnapshot = CountDownLatch(1)
        val handle = object : DroidGuardHandle {
            @Volatile private var opened = true
            override fun isOpened(): Boolean = opened
            override fun snapshot(data: Map<String, String>): String {
                snapshotStarted.countDown()
                check(finishSnapshot.await(5, TimeUnit.SECONDS)) { "snapshot did not unblock" }
                return "native-result"
            }
            override fun close() {
                opened = false
            }
        }

        DroidGuardSessionStore(
            openHandle = { _, _, _ -> handle },
            operationTimeoutMillis = 5_000,
            idleTimeoutMillis = 2_000,
            maxSessions = 1,
            now = clock::get
        ).use { store ->
            val id = store.begin(12, "integrity", "test", emptyMap())
            val callers = Executors.newSingleThreadExecutor()
            try {
                val snapshot = callers.submit<String> { store.snapshot(12, id, emptyMap()) }
                assertTrue("native snapshot must start", snapshotStarted.await(5, TimeUnit.SECONDS))
                clock.set(TimeUnit.MILLISECONDS.toNanos(10_000))
                store.expireIdle()
                try {
                    store.snapshot(12, id, emptyMap())
                    fail("a busy native session must reject a second operation")
                } catch (e: DroidGuardSessionException) {
                    assertEquals(503, e.code)
                }
                finishSnapshot.countDown()
                assertEquals("native-result", snapshot.get(5, TimeUnit.SECONDS))
                assertTrue("reaper must not close the busy handle", handle.isOpened())

                // After completing, the last-touched timestamp is renewed. Later idle
                // expiry must still retire a genuinely idle handle and its session ID.
                clock.set(TimeUnit.MILLISECONDS.toNanos(13_000))
                store.expireIdle()
                try {
                    store.snapshot(12, id, emptyMap())
                    fail("a now-idle session must expire")
                } catch (e: DroidGuardSessionException) {
                    assertEquals(404, e.code)
                }
            } finally {
                finishSnapshot.countDown()
                callers.shutdownNow()
                callers.awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }
}
