/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.cast.proto.CastMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CastDeviceSessionLaunchOptionsTest {
    @Test
    fun legacyLaunchStaysWebOnlyWithoutCredentials() {
        Harness().use { harness ->
            harness.session.launchApplication(APP_ID, true, null)
            val request = harness.takeRequest("LAUNCH")
            assertEquals(listOf("WEB"), request.appTypes())
            assertFalse(request.has("language"))
            assertFalse(request.has("appParams"))
        }
    }

    @Test
    fun androidTvLaunchCarriesOpaqueCredentialsInTheLaunchChecker() {
        Harness().use { harness ->
            val credentials = "opaque \"quoted\" \\ value\nwith unicode \u2603"
            harness.session.launchApplication(APP_ID, true, "en-GB", true, credentials, "custom-sender")
            val request = harness.takeRequest("LAUNCH")
            assertEquals(listOf("WEB", "ANDROID_TV"), request.appTypes())
            assertEquals("en-GB", request.getString("language"))
            assertEquals(credentials, request.credentialsData().getString("credentials"))
            assertEquals("custom-sender", request.credentialsData().getString("credentialsType"))
            assertFalse(request.has("credentials"))
            assertFalse(request.has("androidReceiverCompatible"))
        }
    }

    @Test
    fun prelaunchStatusFallbackKeepsOptionsOffStatusAndOnLaunch() {
        Harness().use { harness ->
            harness.session.launchApplication(APP_ID, false, "fr", true, "sender-token", "android")
            val statusRequest = harness.takeRequest("GET_STATUS")
            assertFalse(statusRequest.has("appParams"))
            assertFalse(statusRequest.has("supportedAppTypes"))
            harness.receive(JSONObject().put("type", "RECEIVER_STATUS").put("requestId", statusRequest.getLong("requestId"))
                .put("status", JSONObject().put("applications", JSONArray())))
            val launch = harness.takeRequest("LAUNCH")
            assertEquals(listOf("WEB", "ANDROID_TV"), launch.appTypes())
            assertEquals("fr", launch.getString("language"))
            assertEquals("sender-token", launch.credentialsData().getString("credentials"))
            assertEquals("android", launch.credentialsData().getString("credentialsType"))
        }
    }

    @Test
    fun credentialFieldsPreserveEmptyStringsAndOmitNulls() {
        for ((credentials, type) in listOf("" to "", null to "custom", "opaque" to null)) {
            Harness().use { harness ->
                harness.session.launchApplication(APP_ID, true, null, false, credentials, type)
                val request = harness.takeRequest("LAUNCH")
                assertEquals(listOf("WEB"), request.appTypes())
                val data = request.credentialsData()
                assertEquals(credentials != null, data.has("credentials"))
                assertEquals(type != null, data.has("credentialsType"))
                if (credentials != null) assertEquals(credentials, data.getString("credentials"))
                if (type != null) assertEquals(type, data.getString("credentialsType"))
            }
        }
    }

    @Test
    fun laterLaunchDoesNotReuseEarlierCredentialsOrAppTypes() {
        Harness().use { harness ->
            harness.session.launchApplication(APP_ID, true, null, true, "first-token", "android")
            val first = harness.takeRequest("LAUNCH")
            assertEquals("first-token", first.credentialsData().getString("credentials"))
            harness.session.launchApplication(APP_ID, true, "de")
            val next = harness.takeRequest("LAUNCH")
            assertEquals(listOf("WEB"), next.appTypes())
            assertFalse(next.has("appParams"))
            assertEquals("de", next.getString("language"))
            assertTrue(next.getLong("requestId") > first.getLong("requestId"))
        }
    }

    private class Harness : AutoCloseable {
        val session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, EmptyCallbacks())
        private val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session)
        private val output = ByteArrayOutputStream()
        private val executor = CastDeviceSession::class.java.getDeclaredField("executor").apply { isAccessible = true }
            .get(session) as ScheduledThreadPoolExecutor

        init {
            CastDeviceSession::class.java.getDeclaredField("channel").apply { isAccessible = true }.set(session, channel)
            CastChannel::class.java.getDeclaredField("output").apply { isAccessible = true }.set(channel, DataOutputStream(output))
        }

        fun receive(payload: JSONObject) {
            session.onMessage(CastMessage(
                CastMessage.ProtocolVersion.CASTV2_1_0, RECEIVER_ID, channel.senderId, NAMESPACE_RECEIVER,
                CastMessage.PayloadType.STRING, payload_utf8 = payload.toString(),
            ))
            awaitIdle()
        }

        fun takeRequest(type: String): JSONObject {
            awaitIdle()
            val messages = ArrayList<CastMessage>()
            DataInputStream(ByteArrayInputStream(output.toByteArray())).use { stream ->
                while (stream.available() > 0) {
                    val body = ByteArray(stream.readInt())
                    stream.readFully(body)
                    messages.add(CastMessage.ADAPTER.decode(body))
                }
            }
            output.reset()
            val requestMessage = messages.single { it.namespace == NAMESPACE_RECEIVER }
            assertEquals(RECEIVER_ID, requestMessage.destination_id)
            val request = JSONObject(requestMessage.payload_utf8!!)
            assertEquals(type, request.getString("type"))
            if (type == "LAUNCH") assertEquals(APP_ID, request.getString("appId"))
            return request
        }

        private fun awaitIdle() {
            executor.submit {}.get(2, TimeUnit.SECONDS)
        }

        override fun close() {
            session.disconnect()
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        }
    }

    private class EmptyCallbacks : CastDeviceSession.Callbacks {
        override fun onConnected() {}
        override fun onConnectionFailed(statusCode: Int) {}
        override fun onDisconnected(statusCode: Int) {}
        override fun onDeviceStatusChanged(status: ReceiverStatus) {}
        override fun onApplicationConnected(application: ReceiverApplication, wasLaunched: Boolean) {}
        override fun onApplicationConnectionFailed(statusCode: Int) {}
        override fun onApplicationStatusChanged(statusText: String?) {}
        override fun onApplicationDisconnected(statusCode: Int) {}
        override fun onStopApplicationResult(statusCode: Int) {}
        override fun onLeaveApplicationResult(statusCode: Int) {}
        override fun onTextMessage(namespace: String, message: String) {}
        override fun onBinaryMessage(namespace: String, data: ByteArray) {}
        override fun onSendMessageSuccess(namespace: String, requestId: Long) {}
        override fun onSendMessageFailure(namespace: String, requestId: Long, statusCode: Int) {}
    }

    private fun JSONObject.appTypes(): List<String> = getJSONArray("supportedAppTypes").let { types ->
        (0 until types.length()).map { types.getString(it) }
    }

    private fun JSONObject.credentialsData(): JSONObject =
        getJSONObject("appParams").getJSONObject("launchCheckerParams").getJSONObject("credentialsData")

    companion object {
        private const val APP_ID = "app-id"
    }
}
