/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RemoteDroidGuardInitializationTest {
    @Test
    fun snapshotWaitsEvenBeforeInitStartsAndOnlyThenAllowsLegacyFallback() {
        val initialization = RemoteDroidGuardInitialization()
        val executor = Executors.newSingleThreadExecutor()
        val awaiting = CountDownLatch(1)
        try {
            val snapshot = executor.submit(Callable {
                awaiting.countDown()
                initialization.await(2_000)
            })
            assertTrue(awaiting.await(1, TimeUnit.SECONDS))
            assertPending(snapshot)
            assertTrue(initialization.start())
            initialization.complete(null)
            assertNull(snapshot.get(1, TimeUnit.SECONDS))
        } finally {
            initialization.close(1)
            executor.shutdownNow()
        }
    }

    @Test
    fun snapshotsWaitForBeginAndUseItsSessionBeforeClose() {
        val initialization = RemoteDroidGuardInitialization()
        val server = RecordingServer()
        val executor = Executors.newFixedThreadPool(2)
        try {
            assertTrue(initialization.start())
            val begin = executor.submit {
                server.session.begin()
                initialization.complete(server.session)
            }
            assertTrue(server.beginStarted.await(1, TimeUnit.SECONDS))
            val awaiting = CountDownLatch(1)
            val snapshot = executor.submit(Callable {
                awaiting.countDown()
                initialization.await(2_000)!!.snapshot(mapOf("rpc" to "challenge"))
            })
            assertTrue(awaiting.await(1, TimeUnit.SECONDS))
            assertPending(snapshot)
            assertEquals(listOf("begin"), server.actions.toList())
            server.releaseBegin.countDown()
            begin.get(1, TimeUnit.SECONDS)
            assertEquals("c2VydmVy", snapshot.get(1, TimeUnit.SECONDS))
            initialization.await(2_000)!!.snapshot(mapOf("rpc" to "sign"))
            initialization.close(2_000)
            assertEquals(listOf("begin", "snapshot", "snapshot", "close"), server.actions.toList())
            assertEquals(listOf("rpc=challenge", "rpc=sign"), server.snapshotBodies.toList())
            assertTrue(server.sessionQueries.all { it.contains("sessionId=remote-1") })
        } finally {
            server.releaseBegin.countDown()
            initialization.close(1)
            executor.shutdownNow()
        }
    }

    @Test
    fun unfinishedInitializationTimesOutInsteadOfEnablingLegacyFallback() {
        val initialization = RemoteDroidGuardInitialization()
        try {
            initialization.await(1)
            fail("A pending init must not be treated as a legacy server")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("timed out"))
        }
        assertTrue(initialization.start())
        initialization.complete(null)
        assertNull(initialization.await(1))
        initialization.close(1)
    }

    @Test
    fun closeBeforeInitPreventsThatGenerationFromStarting() {
        val initialization = RemoteDroidGuardInitialization()
        initialization.close(1)
        assertFalse(initialization.start())
        assertClosed(initialization)

        // An explicit re-init uses a fresh completion, without reopening the old one.
        val replacement = RemoteDroidGuardInitialization()
        assertTrue(replacement.start())
        replacement.complete(null)
        assertNull(replacement.await(1))
        assertClosed(initialization)
        replacement.close(1)
    }

    @Test
    fun closeWhileBeginIsPendingClosesTheLateSessionExactlyOnce() {
        val initialization = RemoteDroidGuardInitialization()
        val server = RecordingServer()
        val executor = Executors.newSingleThreadExecutor()
        try {
            assertTrue(initialization.start())
            val begin = executor.submit {
                server.session.begin()
                initialization.complete(server.session)
            }
            assertTrue(server.beginStarted.await(1, TimeUnit.SECONDS))
            initialization.close(1)
            server.releaseBegin.countDown()
            begin.get(1, TimeUnit.SECONDS)
            assertClosed(initialization)
            assertFalse(initialization.start())
            initialization.close(1)
            assertEquals(listOf("begin", "close"), server.actions.toList())
        } finally {
            server.releaseBegin.countDown()
            initialization.close(1)
            executor.shutdownNow()
        }
    }

    private fun assertPending(future: Future<*>) {
        try {
            future.get(40, TimeUnit.MILLISECONDS)
            fail("Snapshot completed before initialization")
        } catch (expected: TimeoutException) {
            // Only the controlled begin/completion may release this call.
        }
    }

    private fun assertClosed(initialization: RemoteDroidGuardInitialization) {
        try {
            initialization.await(1)
            fail("A closed initialization must stay closed")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("closed"))
        }
    }

    private class RecordingServer {
        val beginStarted = CountDownLatch(1)
        val releaseBegin = CountDownLatch(1)
        val actions = Collections.synchronizedList(mutableListOf<String>())
        val sessionQueries = Collections.synchronizedList(mutableListOf<String>())
        val snapshotBodies = Collections.synchronizedList(mutableListOf<String>())
        val session = RemoteDroidGuardSession(
            RemoteDroidGuardHttpClient("http://example.test/droidguard", 2_000, ::open),
            mapOf("flow" to "play_integrity", "source" to "com.example.app")
        )

        private fun open(url: URL): HttpURLConnection = object : HttpURLConnection(url) {
            private val body = ByteArrayOutputStream()
            private val action = url.query.split('&').first { it.startsWith("action=") }.substringAfter('=')

            override fun getResponseCode(): Int {
                actions.add(action)
                if (action == "begin") {
                    beginStarted.countDown()
                    check(releaseBegin.await(2, TimeUnit.SECONDS)) { "Controlled begin was not released" }
                } else {
                    sessionQueries.add(url.query)
                    if (action == "snapshot") snapshotBodies.add(body.toString(Charsets.UTF_8))
                }
                return 200
            }

            override fun getInputStream() = ByteArrayInputStream(
                (if (action == "begin") "sessionId=remote-1" else "c2VydmVy").toByteArray(Charsets.UTF_8)
            )

            override fun getOutputStream() = body
            override fun connect() = Unit
            override fun disconnect() = Unit
            override fun usingProxy() = false
        }
    }
}
