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
