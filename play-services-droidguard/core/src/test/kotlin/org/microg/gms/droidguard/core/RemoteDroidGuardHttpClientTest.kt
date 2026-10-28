/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class RemoteDroidGuardHttpClientTest {
    @Test
    fun postsEncodedQueryAndFormToAppendedEndpointAndDisconnects() {
        lateinit var connection: RecordingConnection
        val client = RemoteDroidGuardHttpClient(
            "http://example.test/root?auth=x%2Fy",
            2_000,
            openConnection = { url ->
                RecordingConnection(url, 200, "sessionId=remote%2F1").also { connection = it }
            }
        )

        val result = client.post(
            "begin",
            mapOf("source" to "app name", "flow" to "integrity"),
            RemoteDroidGuardHttpClient.encodeForm(mapOf("token" to "a b", "step" to "next+step"))
        )

        assertEquals("sessionId=remote%2F1", result)
        assertEquals("http://example.test/root/begin?auth=x%2Fy&flow=integrity&source=app%20name", connection.url.toString())
        assertEquals("POST", connection.requestMethod)
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", connection.recordedRequestProperties["Content-Type"])
        assertEquals("token=a%20b&step=next%2Bstep", connection.requestBody.toString(Charsets.UTF_8))
        assertEquals(2_000, connection.connectTimeout)
        assertEquals(2_000, connection.readTimeout)
        assertTrue(connection.disconnected)
    }

    @Test
    fun rejectsNonSuccessResponsesAndStillDisconnects() {
        lateinit var connection: RecordingConnection
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { url -> RecordingConnection(url, 503, "").also { connection = it } }
        )

        try {
            client.post("snapshot", emptyMap(), null)
            throw AssertionError("Expected an HTTP error")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("503"))
            assertTrue(connection.disconnected)
            assertFalse(connection.doOutput)
        }
    }

    @Test
    fun rejectsResponseBodyExceedingLimitAndStillDisconnects() {
        lateinit var connection: RecordingConnection
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { url ->
                val oversizeResponse = "a".repeat(256 * 1024 + 1)
                RecordingConnection(url, 200, oversizeResponse).also { connection = it }
            }
        )

        try {
            client.post("snapshot", emptyMap(), null)
            throw AssertionError("Expected an IOException for exceeding limit")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("256 KiB limit", ignoreCase = true))
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun acceptsResponseBodyAtLimitAndStillDisconnects() {
        lateinit var connection: RecordingConnection
        val acceptedResponse = "a".repeat(256 * 1024)
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { url ->
                RecordingConnection(url, 200, acceptedResponse).also { connection = it }
            }
        )

        val result = client.post("snapshot", emptyMap(), null)

        assertEquals(acceptedResponse, result)
        assertTrue(connection.disconnected)
    }

    @Test
    fun rejectsRequestTargetExceedingAdapterLimitBeforeConnecting() {
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { throw AssertionError("The adapter request bound must reject before any connection") }
        )

        try {
            client.post("snapshot", mapOf("pad" to "p".repeat(8 * 1024)), null)
            throw AssertionError("Expected an IOException for an oversized request target")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("adapter limit"))
        }
    }

    @Test
    fun rejectsRequestBodyExceedingAdapterLimitBeforeConnecting() {
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { throw AssertionError("The adapter request bound must reject before any connection") }
        )

        try {
            client.post("snapshot", emptyMap(), ByteArray(32 * 1024 + 1))
            throw AssertionError("Expected an IOException for an oversized request body")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("adapter limit"))
        }
    }

    @Test
    fun rejectsQueryFieldsExceedingAdapterLimitBeforeConnecting() {
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { throw AssertionError("The adapter request bound must reject before any connection") }
        )

        try {
            // 129 fields stay far under the request-target byte limit but exceed
            // the adapter's max_num_fields=128 for the query string.
            client.post("begin", (0..128).associate { "f$it" to "v" }, null)
            throw AssertionError("Expected an IOException for too many query fields")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("adapter field limit"))
        }
    }

    @Test
    fun rejectsBodyFieldsExceedingAdapterLimitBeforeConnecting() {
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { throw AssertionError("The adapter request bound must reject before any connection") }
        )

        try {
            // 257 fields stay far under the body byte limit but exceed the
            // adapter's max_num_fields=256 for the form body.
            val oversized = RemoteDroidGuardHttpClient.encodeForm((0..256).associate { "f$it" to "v" })
            client.post("snapshot", emptyMap(), oversized)
            throw AssertionError("Expected an IOException for too many body fields")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty().contains("adapter field limit"))
        }
    }

    @Test
    fun acceptsRequestsAtAdapterLimits() {
        val connections = mutableListOf<RecordingConnection>()
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { url -> RecordingConnection(url, 200, "status=ok").also(connections::add) }
        )

        // Each adapter bound is inclusive: only counts/sizes above the limit are
        // rejected, so requests exactly at the limit must still reach the wire.
        assertEquals(
            "status=ok",
            client.post(
                "begin",
                (0..127).associate { "f$it" to "v" },
                RemoteDroidGuardHttpClient.encodeForm((0..255).associate { "d$it" to "v" })
            )
        )
        assertEquals("status=ok", client.post("snapshot", emptyMap(), ByteArray(32 * 1024) { 'a'.code.toByte() }))
        assertEquals(2, connections.size)
        assertTrue(connections.all { it.disconnected })
    }

    @Test
    fun acceptsRequestTargetAtAdapterLimit() {
        lateinit var connection: RecordingConnection
        val client = RemoteDroidGuardHttpClient(
            "http://example.test",
            2_000,
            openConnection = { url -> RecordingConnection(url, 200, "status=ok").also { connection = it } }
        )

        // The adapter rejects only request targets above 8192 bytes, so a
        // target exactly at the bound must still reach the wire.
        // "/snapshot" (9) + "?" (1) + "k=" (2) + value = 8192 total.
        val result = client.post("snapshot", mapOf("k" to "x".repeat(8_180)), null)

        assertEquals("status=ok", result)
        assertEquals(8 * 1024, connection.url.file.toByteArray(Charsets.UTF_8).size)
        assertTrue(connection.disconnected)
    }

    private class RecordingConnection(
        url: URL,
        private val responseCodeValue: Int,
        response: String
    ) : HttpURLConnection(url) {
        val recordedRequestProperties = mutableMapOf<String, String>()
        val body = ByteArrayOutputStream()
        val requestBody: ByteArrayOutputStream get() = body
        val disconnected get() = wasDisconnected
        private val responseBytes = response.toByteArray(Charsets.UTF_8)
        private var wasDisconnected = false

        override fun connect() = Unit
        override fun disconnect() { wasDisconnected = true }
        override fun usingProxy() = false
        override fun getResponseCode() = responseCodeValue
        override fun getInputStream() = ByteArrayInputStream(responseBytes)
        override fun getOutputStream() = body
        override fun setRequestProperty(key: String, value: String) { recordedRequestProperties[key] = value }
    }
}
