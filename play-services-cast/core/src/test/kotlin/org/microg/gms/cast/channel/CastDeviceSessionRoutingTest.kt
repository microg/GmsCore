/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.microg.gms.cast.proto.CastMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Exercises the real session executor and channel frames without opening a socket. */
class CastDeviceSessionRoutingTest {
    private val senderId = "sender-under-test"
    private val events = CopyOnWriteArrayList<String>()
    private val output = ByteArrayOutputStream()
    private lateinit var session: CastDeviceSession
    private lateinit var executor: ScheduledThreadPoolExecutor

    @Before
    fun setUp() {
        val callbackType = CastDeviceSession.Callbacks::class.java
        val callbacks = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, method, _ ->
            events.add(method.name)
            null
        } as CastDeviceSession.Callbacks
        session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        executor = field(CastDeviceSession::class.java, "executor").get(session) as ScheduledThreadPoolExecutor
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session, senderId)
        field(CastChannel::class.java, "output").set(channel, DataOutputStream(output))
        field(CastDeviceSession::class.java, "channel").set(session, channel)
    }

    @After
    fun tearDown() {
        session.disconnect()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
    }

    @Test
    fun receiverReplyForAnotherSenderDoesNotConsumeOurJoinRequest() {
        session.joinApplication("app-id", null)
        awaitIdle()
        val requestId = requestId()

        session.onMessage(receiverStatus("sender-other", requestId, running = true))
        awaitIdle()
        assertTrue("Another sender's reply must not change state or complete our request", events.isEmpty())

        session.onMessage(receiverStatus(senderId, requestId, running = true))
        awaitIdle()
        assertEquals(listOf("onDeviceStatusChanged", "onApplicationConnected"), events.toList())
    }

    @Test
    fun unrelatedStatusCannotDisconnectOurApplicationButBroadcastStatusCan() {
        session.joinApplication("app-id", null)
        awaitIdle()
        session.onMessage(receiverStatus(senderId, requestId(), running = true))
        awaitIdle()
        events.clear()

        session.onMessage(receiverStatus("sender-other", 0, running = false))
        awaitIdle()
        assertTrue("Another sender's status must not disconnect our application", events.isEmpty())

        session.onMessage(receiverStatus(BROADCAST_ID, 0, running = false))
        awaitIdle()
        assertEquals(listOf("onApplicationDisconnected", "onDeviceStatusChanged"), events.toList())
    }

    private fun requestId(): Long {
        val input = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        val body = ByteArray(input.readInt())
        input.readFully(body)
        val message = CastMessage.ADAPTER.decode(body)
        assertEquals(senderId, message.source_id)
        assertEquals(NAMESPACE_RECEIVER, message.namespace)
        return JSONObject(message.payload_utf8!!).getLong("requestId")
    }

    private fun receiverStatus(destination: String, requestId: Long, running: Boolean): CastMessage {
        val applications = if (running) {
            """[{"appId":"app-id","sessionId":"session-id","transportId":"transport-id"}]"""
        } else "[]"
        return CastMessage(
            CastMessage.ProtocolVersion.CASTV2_1_0,
            RECEIVER_ID,
            destination,
            NAMESPACE_RECEIVER,
            CastMessage.PayloadType.STRING,
            payload_utf8 = """{"type":"RECEIVER_STATUS","requestId":$requestId,"status":{"applications":$applications}}""",
        )
    }

    private fun awaitIdle() {
        executor.submit {}.get(2, TimeUnit.SECONDS)
    }

    private fun field(type: Class<*>, name: String) = type.getDeclaredField(name).apply { isAccessible = true }
}
