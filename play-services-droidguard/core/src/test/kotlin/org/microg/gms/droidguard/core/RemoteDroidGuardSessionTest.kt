/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class RemoteDroidGuardSessionTest {
    @Test
    fun reusesOneServerSessionForEachExistingSnapshotAndClosesIt() {
        val responses = ArrayDeque(listOf("sessionId=remote%2F1", "c2VydmVyLWNoYWxsZW5nZQ==", "c2VydmVyLXNpZw==", "status=ok"))
        val connections = mutableListOf<RecordingConnection>()
        val client = RemoteDroidGuardHttpClient("http://example.test/droidguard", 2_000) { url ->
            RecordingConnection(url, responses.removeFirst()).also(connections::add)
        }
        val session = RemoteDroidGuardSession(
            client,
            mapOf("flow" to "play_integrity", "source" to "com.example.app")
        )

        session.begin()
        assertEquals("c2VydmVyLWNoYWxsZW5nZQ==", session.snapshot(mapOf("rpc" to "challenge")))
        assertEquals("c2VydmVyLXNpZw==", session.snapshot(mapOf("rpc" to "sign")))
        session.close()

        assertEquals(4, connections.size)
        assertTrue(connections.all { it.disconnected })
        assertTrue(connections[0].url.query.contains("action=begin"))
        assertTrue(connections[1].url.query.contains("action=snapshot"))
        assertTrue(connections[1].url.query.contains("sessionId=remote%2F1"))
        assertEquals("rpc=challenge", connections[1].body.toString(Charsets.UTF_8))
        assertTrue(connections[2].url.query.contains("sessionId=remote%2F1"))
        assertEquals("rpc=sign", connections[2].body.toString(Charsets.UTF_8))
        assertTrue(connections[3].url.query.contains("action=close"))
        assertTrue(connections[3].url.query.contains("sessionId=remote%2F1"))
    }

    @Test
    fun refusesToUseSessionWhenBeginHasNoSessionId() {
        val client = RemoteDroidGuardHttpClient("http://example.test/droidguard", 2_000) { url ->
            RecordingConnection(url, "status=ok")
        }
        val session = RemoteDroidGuardSession(client, emptyMap())

        try {
            session.begin()
            throw AssertionError("Expected a missing session id to fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("session id"))
        }
    }

    @Test
    fun serializesSnapshotsForTheSameRemoteSession() {
        assertWaitsForSnapshot { it.snapshot(mapOf("rpc" to "second")) }
    }

    @Test
    fun waitsForSnapshotBeforeClosingTheRemoteSession() {
        assertWaitsForSnapshot { it.close() }
    }

    private fun assertWaitsForSnapshot(nextOperation: (RemoteDroidGuardSession) -> Unit) {
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val requests = AtomicInteger()
        val client = RemoteDroidGuardHttpClient("http://example.test/droidguard", 2_000) { url ->
            val beginning = url.query.contains("action=begin")
            RecordingConnection(url, if (beginning) "sessionId=one" else "c2ln") {
                if (!beginning) {
                    if (requests.getAndIncrement() == 0) {
                        firstEntered.countDown()
                        check(releaseFirst.await(5, TimeUnit.SECONDS)) { "Snapshot was not released" }
                    } else {
                        secondEntered.countDown()
                    }
                }
            }
        }
        val session = RemoteDroidGuardSession(client, emptyMap())
        val workers = Executors.newFixedThreadPool(2)
        try {
            session.begin()
            val first = workers.submit<String> { session.snapshot(mapOf("rpc" to "first")) }
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS))
            val second = workers.submit {
                secondStarted.countDown()
                nextOperation(session)
            }
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS))
            assertFalse("A request overtook the active snapshot", secondEntered.await(150, TimeUnit.MILLISECONDS))
            releaseFirst.countDown()
            assertEquals("c2ln", first.get(5, TimeUnit.SECONDS))
            second.get(5, TimeUnit.SECONDS)
            assertEquals(0L, secondEntered.count)
        } finally {
            releaseFirst.countDown()
            workers.shutdownNow()
            workers.awaitTermination(5, TimeUnit.SECONDS)
            session.close()
        }
    }

    private class RecordingConnection(
        url: URL,
        response: String,
        private val onResponse: () -> Unit = {}
    ) : HttpURLConnection(url) {
        private val responseBytes = response.toByteArray(Charsets.UTF_8)
        val body = ByteArrayOutputStream()
        var disconnected = false
            private set

        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode(): Int { onResponse(); return 200 }
        override fun getInputStream() = ByteArrayInputStream(responseBytes)
        override fun getOutputStream() = body
    }
}
