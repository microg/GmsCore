/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import org.junit.After
import org.junit.Assert.assertArrayEquals
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

/** Pins the public text-send contract to UTF-8 byte counts and the serialized CastMessage body limit. */
class CastDeviceSessionUtf8SendBoundaryTest {
    private data class Completion(val namespace: String, val requestId: Long, val status: Int)

    private val namespace = "urn:x-cast:test"
    private val completions = CopyOnWriteArrayList<Completion>()
    private val output = ByteArrayOutputStream()
    private lateinit var session: CastDeviceSession
    private lateinit var executor: ScheduledThreadPoolExecutor

    @Before
    fun setUp() {
        val callbackType = CastDeviceSession.Callbacks::class.java
        val callbacks = Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) { _, method, args ->
            when (method.name) {
                "onSendMessageSuccess" -> completions.add(
                    Completion(args!![0] as String, args[1] as Long, CastDeviceSession.STATUS_SUCCESS)
                )
                "onSendMessageFailure" -> completions.add(
                    Completion(args!![0] as String, args[1] as Long, args[2] as Int)
                )
            }
            null
        } as CastDeviceSession.Callbacks

        session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        executor = field(CastDeviceSession::class.java, "executor").get(session) as ScheduledThreadPoolExecutor
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session, "sender-under-test")
        field(CastChannel::class.java, "output").set(channel, DataOutputStream(output))
        field(CastDeviceSession::class.java, "channel").set(session, channel)
        field(CastDeviceSession::class.java, "application").set(
            session,
            ReceiverApplication("app-id", null, "session-id", "transport-id", null, null, listOf(namespace))
        )
    }

    @After
    fun tearDown() {
        session.disconnect()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
    }

    @Test
    fun mixedUtf8TextSendsRespectSerializedBodyBoundaryAndRecover() {
        val below = messageWithUtf8EncodedSize(MAX_PAYLOAD_SIZE - 1)
        val exact = messageWithUtf8EncodedSize(MAX_PAYLOAD_SIZE)
        val over = messageWithUtf8EncodedSize(MAX_PAYLOAD_SIZE + 1)

        assertMixedUtf8(below.payload_utf8!!)
        assertMixedUtf8(exact.payload_utf8!!)
        assertMixedUtf8(over.payload_utf8!!)
        // Keep the one-over case below the raw-payload cap so it exercises the serialized-envelope guard.
        assertTrue(over.payload_utf8!!.toByteArray(Charsets.UTF_8).size < MAX_PAYLOAD_SIZE)

        session.sendMessage(namespace, below.payload_utf8!!, 41L)
        session.sendMessage(namespace, exact.payload_utf8!!, 42L)
        session.sendMessage(namespace, over.payload_utf8!!, 43L)
        session.sendMessage(namespace, "after-rejection-😀", 44L)
        executor.submit {}.get(2, TimeUnit.SECONDS)

        assertEquals(
            listOf(
                Completion(namespace, 41L, CastDeviceSession.STATUS_SUCCESS),
                Completion(namespace, 42L, CastDeviceSession.STATUS_SUCCESS),
                Completion(namespace, 43L, CastDeviceSession.STATUS_MESSAGE_TOO_LARGE),
                Completion(namespace, 44L, CastDeviceSession.STATUS_SUCCESS),
            ),
            completions.toList()
        )

        val frames = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        assertBoundaryFrame(frames, below, MAX_PAYLOAD_SIZE - 1)
        assertBoundaryFrame(frames, exact, MAX_PAYLOAD_SIZE)
        val recovered = readFrame(frames)
        assertEquals("after-rejection-😀", recovered.payload_utf8)
        assertEquals(namespace, recovered.namespace)
        assertEquals("transport-id", recovered.destination_id)
        assertEquals(0, frames.available())
    }

    @Test
    fun rawUtf8OverLimitRejectsBeforeWriteAndNextTextSendStillSucceeds() {
        val rawOverLimit = "😀".repeat(MAX_PAYLOAD_SIZE / 4 + 1)
        assertTrue(rawOverLimit.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_SIZE)

        session.sendMessage(namespace, rawOverLimit, 51L)
        session.sendMessage(namespace, "ok-é-€-😀", 52L)
        executor.submit {}.get(2, TimeUnit.SECONDS)

        assertEquals(
            listOf(
                Completion(namespace, 51L, CastDeviceSession.STATUS_MESSAGE_TOO_LARGE),
                Completion(namespace, 52L, CastDeviceSession.STATUS_SUCCESS),
            ),
            completions.toList()
        )
        val frames = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        val recovered = readFrame(frames)
        assertEquals("ok-é-€-😀", recovered.payload_utf8)
        assertEquals(0, frames.available())
    }

    private fun messageWithUtf8EncodedSize(size: Int): CastMessage {
        val marker = "é€😀"
        var low = 0
        var high = size
        while (low <= high) {
            val middle = (low + high) ushr 1
            val candidate = CastMessage(
                CastMessage.ProtocolVersion.CASTV2_1_0,
                "sender-under-test",
                "transport-id",
                namespace,
                CastMessage.PayloadType.STRING,
                payload_utf8 = marker + "x".repeat(middle),
            )
            when {
                CastMessage.ADAPTER.encodedSize(candidate) < size -> low = middle + 1
                CastMessage.ADAPTER.encodedSize(candidate) > size -> high = middle - 1
                else -> return candidate
            }
        }
        error("Could not construct a UTF-8 CastMessage with encoded body size $size")
    }

    private fun assertMixedUtf8(payload: String) {
        assertTrue(payload.contains("é"))
        assertTrue(payload.contains("€"))
        assertTrue(payload.contains("😀"))
    }

    private fun assertBoundaryFrame(input: DataInputStream, expected: CastMessage, expectedSize: Int) {
        val length = input.readInt()
        assertEquals(expectedSize, length)
        val body = ByteArray(length)
        input.readFully(body)
        assertArrayEquals(expected.encode(), body)
        val decoded = CastMessage.ADAPTER.decode(body)
        assertEquals(expected.payload_utf8, decoded.payload_utf8)
        assertEquals(expected.namespace, decoded.namespace)
        assertEquals(expected.destination_id, decoded.destination_id)
    }

    private fun readFrame(input: DataInputStream): CastMessage {
        val body = ByteArray(input.readInt())
        input.readFully(body)
        return CastMessage.ADAPTER.decode(body)
    }

    private fun field(type: Class<*>, name: String) = type.getDeclaredField(name).apply { isAccessible = true }
}
