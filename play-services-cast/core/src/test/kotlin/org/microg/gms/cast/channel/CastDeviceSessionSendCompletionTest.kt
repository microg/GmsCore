/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import okio.ByteString.Companion.toByteString
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

/** Runs the real send paths and executor admission against an in-memory Cast channel. */
class CastDeviceSessionSendCompletionTest {
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
                "onSendMessageSuccess" -> completions.add(Completion(args!![0] as String, args[1] as Long, CastDeviceSession.STATUS_SUCCESS))
                "onSendMessageFailure" -> completions.add(Completion(args!![0] as String, args[1] as Long, args[2] as Int))
            }
            null
        } as CastDeviceSession.Callbacks
        session = CastDeviceSession("127.0.0.1", CastChannel.DEFAULT_PORT, callbacks)
        executor = field(CastDeviceSession::class.java, "executor").get(session) as ScheduledThreadPoolExecutor
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, session, "sender-under-test")
        field(CastChannel::class.java, "output").set(channel, DataOutputStream(output))
        field(CastDeviceSession::class.java, "channel").set(session, channel)
        field(CastDeviceSession::class.java, "application").set(session,
            ReceiverApplication("app-id", null, "session-id", "transport-id", null, null, listOf(namespace)))
    }

    @After
    fun tearDown() {
        session.disconnect()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
    }

    @Test
    fun acceptedTextAndBinarySendsWriteFramesAndCompleteOnceWithOriginalIds() {
        val binary = byteArrayOf(0, 1, -1)
        session.sendMessage(namespace, "payload", 42L)
        session.sendBinaryMessage(namespace, binary, Long.MAX_VALUE)
        executor.submit {}.get(2, TimeUnit.SECONDS)

        assertEquals(listOf(Completion(namespace, 42L, 0), Completion(namespace, Long.MAX_VALUE, 0)), completions.toList())
        val frames = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        val text = readFrame(frames)
        val bytes = readFrame(frames)
        assertEquals("transport-id", text.destination_id)
        assertEquals(namespace, text.namespace)
        assertEquals("payload", text.payload_utf8)
        assertEquals(CastMessage.PayloadType.BINARY, bytes.payload_type)
        assertArrayEquals(binary, bytes.payload_binary!!.toByteArray())
        assertEquals(0, frames.available())
    }

    @Test
    fun textAndBinarySendsAcceptExactly65536EncodedBodyBytes() {
        val text = messageWithEncodedSize(MAX_PAYLOAD_SIZE, CastMessage.PayloadType.STRING)
        val binary = messageWithEncodedSize(MAX_PAYLOAD_SIZE, CastMessage.PayloadType.BINARY)
        session.sendMessage(namespace, text.payload_utf8!!, 42L)
        session.sendBinaryMessage(namespace, binary.payload_binary!!.toByteArray(), Long.MAX_VALUE)
        executor.submit {}.get(2, TimeUnit.SECONDS)

        assertEquals(listOf(Completion(namespace, 42L, 0), Completion(namespace, Long.MAX_VALUE, 0)), completions.toList())
        val frames = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        assertBoundaryFrame(frames, text)
        assertBoundaryFrame(frames, binary)
        assertEquals(0, frames.available())
    }

    @Test
    fun textAndBinarySendsReject65537EncodedBodyBytesWithoutCorruptingNextSend() {
        val text = messageWithEncodedSize(MAX_PAYLOAD_SIZE + 1, CastMessage.PayloadType.STRING)
        val binary = messageWithEncodedSize(MAX_PAYLOAD_SIZE + 1, CastMessage.PayloadType.BINARY)
        // These pass the raw-payload checks and reach the encoded-envelope limit.
        assertTrue(text.payload_utf8!!.toByteArray(Charsets.UTF_8).size < MAX_PAYLOAD_SIZE)
        assertTrue(binary.payload_binary!!.size < MAX_PAYLOAD_SIZE)
        session.sendMessage(namespace, text.payload_utf8!!, 42L)
        session.sendBinaryMessage(namespace, binary.payload_binary!!.toByteArray(), Long.MAX_VALUE)
        executor.submit {}.get(2, TimeUnit.SECONDS)

        assertEquals(listOf(Completion(namespace, 42L, 2006), Completion(namespace, Long.MAX_VALUE, 2006)), completions.toList())
        assertEquals(0, output.size())

        val smallBinary = byteArrayOf(3, 2, 1)
        session.sendMessage(namespace, "after-rejection", 43L)
        session.sendBinaryMessage(namespace, smallBinary, 44L)
        executor.submit {}.get(2, TimeUnit.SECONDS)
        assertEquals(listOf(
            Completion(namespace, 42L, 2006), Completion(namespace, Long.MAX_VALUE, 2006),
            Completion(namespace, 43L, 0), Completion(namespace, 44L, 0),
        ), completions.toList())
        val frames = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        assertEquals("after-rejection", readFrame(frames).payload_utf8)
        assertArrayEquals(smallBinary, readFrame(frames).payload_binary!!.toByteArray())
        assertEquals(0, frames.available())
    }

    @Test
    fun textAndBinarySendsAfterDisconnectEachFailOnceWithOriginalIds() {
        session.disconnect()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))

        session.sendMessage(namespace, "payload", 42L)
        session.sendBinaryMessage(namespace, byteArrayOf(1), Long.MAX_VALUE)

        assertEquals(listOf(failure(42L), failure(Long.MAX_VALUE)), completions.toList())
        assertEquals(0, output.size())
    }

    @Test
    fun textSendFailsOnceWhenShutdownWinsTheEnqueueRace() {
        shutDownDuringNextEnqueue()
        session.sendMessage(namespace, "payload", 42L)
        assertEquals(listOf(failure(42L)), completions.toList())
        assertEquals(0, output.size())
    }

    @Test
    fun binarySendFailsOnceWhenShutdownWinsTheEnqueueRace() {
        shutDownDuringNextEnqueue()
        session.sendBinaryMessage(namespace, byteArrayOf(1), Long.MAX_VALUE)
        assertEquals(listOf(failure(Long.MAX_VALUE)), completions.toList())
        assertEquals(0, output.size())
    }

    private fun shutDownDuringNextEnqueue() {
        executor.shutdown()
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS))
        // isShutdown is false when post checks it. The real executor then rejects execute after shutdown.
        executor = object : ScheduledThreadPoolExecutor(1) {
            override fun execute(command: Runnable) {
                shutdown()
                super.execute(command)
            }
        }
        field(CastDeviceSession::class.java, "executor").set(session, executor)
    }

    private fun failure(requestId: Long) = Completion(namespace, requestId, CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING)

    private fun assertBoundaryFrame(input: DataInputStream, expected: CastMessage) {
        val length = input.readInt()
        assertEquals(MAX_PAYLOAD_SIZE, length)
        val body = ByteArray(length)
        input.readFully(body)
        assertArrayEquals(expected.encode(), body)
        assertEquals(expected, CastMessage.ADAPTER.decode(body))
    }

    private fun messageWithEncodedSize(size: Int, payloadType: CastMessage.PayloadType): CastMessage {
        var low = 0
        var high = size
        while (low <= high) {
            val middle = (low + high) ushr 1
            val candidate = CastMessage(
                CastMessage.ProtocolVersion.CASTV2_1_0, "sender-under-test", "transport-id", namespace, payloadType,
                payload_utf8 = if (payloadType == CastMessage.PayloadType.STRING) "x".repeat(middle) else null,
                payload_binary = if (payloadType == CastMessage.PayloadType.BINARY) ByteArray(middle) { 0x5a }.toByteString() else null,
            )
            when {
                candidate.encode().size < size -> low = middle + 1
                candidate.encode().size > size -> high = middle - 1
                else -> return candidate
            }
        }
        error("Could not construct a CastMessage with encoded body size " + size)
    }

    private fun readFrame(input: DataInputStream): CastMessage {
        val body = ByteArray(input.readInt())
        input.readFully(body)
        return CastMessage.ADAPTER.decode(body)
    }

    private fun field(type: Class<*>, name: String) = type.getDeclaredField(name).apply { isAccessible = true }
}
