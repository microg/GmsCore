/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayDeque

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

    private class RecordingConnection(url: URL, response: String) : HttpURLConnection(url) {
        private val responseBytes = response.toByteArray(Charsets.UTF_8)
        val body = ByteArrayOutputStream()
        var disconnected = false
            private set

        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = 200
        override fun getInputStream() = ByteArrayInputStream(responseBytes)
        override fun getOutputStream() = body
    }
}
