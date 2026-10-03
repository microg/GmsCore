/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.cast.proto.CastMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException

/** Wire-level tests for the serialized CastMessage body limit and four-byte frame prefix. */
class CastChannelFramingTest {
    private val destination = "receiver-test"
    private val namespace = "urn:x-cast:test"
    private val listener = object : CastChannel.Listener {
        override fun onMessage(message: CastMessage) = Unit
        override fun onClosed(error: IOException?) = Unit
    }

    @Test
    fun writeLocked_acceptsExactly65536EncodedBodyBytes() {
        val message = stringMessageWithEncodedSize(MAX_PAYLOAD_SIZE)
        val encoded = message.encode()
        assertEquals(MAX_PAYLOAD_SIZE, encoded.size)

        val bytes = ByteArrayOutputStream()
        invokeWriteLocked(channel(bytes), message)

        val frame = DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
        assertEquals(MAX_PAYLOAD_SIZE, frame.readInt())
        val body = ByteArray(MAX_PAYLOAD_SIZE)
        frame.readFully(body)
        assertEquals(MAX_PAYLOAD_SIZE, body.size)
        assertArrayEquals(encoded, body)
        assertEquals(message, CastMessage.ADAPTER.decode(body))
        assertEquals(0, frame.available())
    }

    @Test
    fun writeLocked_rejects65537EncodedBodyBytesBeforeWritingPrefix() {
        val message = stringMessageWithEncodedSize(MAX_PAYLOAD_SIZE + 1)
        assertEquals(MAX_PAYLOAD_SIZE + 1, message.encode().size)
        val bytes = ByteArrayOutputStream()

        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            invokeWriteLocked(channel(bytes), message)
        }

        assertTrue(error is MessageTooLargeException)
        assertEquals(0, bytes.size())
    }

    @Test
    fun sendRejectsPayloadAtRawLimitWhenStringEnvelopePushesBodyOverLimit() {
        val bytes = ByteArrayOutputStream()
        val channel = channel(bytes)
        val payload = "x".repeat(MAX_PAYLOAD_SIZE)
        assertEquals(MAX_PAYLOAD_SIZE, payload.toByteArray(Charsets.UTF_8).size)
        assertTrue(stringMessage(payload).encode().size > MAX_PAYLOAD_SIZE)

        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            channel.send(destination, namespace, payload)
        }

        assertTrue(error is MessageTooLargeException)
        assertEquals(0, bytes.size())
    }

    @Test
    fun sendRejectsPayloadAtRawLimitWhenBinaryEnvelopePushesBodyOverLimit() {
        val bytes = ByteArrayOutputStream()
        val channel = channel(bytes)
        val payload = ByteArray(MAX_PAYLOAD_SIZE) { 0x5a }
        assertTrue(binaryMessage(payload).encode().size > MAX_PAYLOAD_SIZE)

        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            channel.send(destination, namespace, payload)
        }

        assertTrue(error is MessageTooLargeException)
        assertEquals(0, bytes.size())
    }

    @Test
    fun readMessageAcceptsExactly65536EncodedBodyBytes() {
        val message = stringMessageWithEncodedSize(MAX_PAYLOAD_SIZE)
        val encoded = message.encode()
        assertEquals(MAX_PAYLOAD_SIZE, encoded.size)
        val framed = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(encoded.size)
                write(encoded)
            }
        }

        val decoded = invokeReadMessage(channel(ByteArrayOutputStream()), framed.toByteArray())

        assertEquals(message, decoded)
        assertEquals(message.payload_utf8, decoded.payload_utf8)
    }

    @Test
    fun readMessageRejects65537PrefixBeforeAttemptingBodyRead() {
        val prefixOnly = ByteArrayOutputStream().also { DataOutputStream(it).writeInt(MAX_PAYLOAD_SIZE + 1) }
        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            invokeReadMessage(channel(ByteArrayOutputStream()), prefixOnly.toByteArray())
        }

        assertEquals("Invalid Cast frame length ${MAX_PAYLOAD_SIZE + 1}", error.message)
    }

    private fun channel(bytes: ByteArrayOutputStream): CastChannel {
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, listener)
        val output = CastChannel::class.java.getDeclaredField("output").apply { isAccessible = true }
        output.set(channel, DataOutputStream(bytes))
        return channel
    }

    private fun invokeWriteLocked(channel: CastChannel, message: CastMessage) {
        val method = CastChannel::class.java.getDeclaredMethod("writeLocked", CastMessage::class.java)
            .apply { isAccessible = true }
        try {
            method.invoke(channel, message)
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    }

    private fun invokeReadMessage(channel: CastChannel, framed: ByteArray): CastMessage {
        val method = CastChannel::class.java.getDeclaredMethod("readMessage", DataInputStream::class.java)
            .apply { isAccessible = true }
        try {
            return method.invoke(channel, DataInputStream(ByteArrayInputStream(framed))) as CastMessage
        } catch (error: InvocationTargetException) {
            throw error.targetException
        }
    }

    private fun stringMessage(payload: String) = CastMessage(
        CastMessage.ProtocolVersion.CASTV2_1_0,
        "sender-0",
        destination,
        namespace,
        CastMessage.PayloadType.STRING,
        payload_utf8 = payload,
    )

    private fun binaryMessage(payload: ByteArray) = CastMessage(
        CastMessage.ProtocolVersion.CASTV2_1_0,
        "sender-0",
        destination,
        namespace,
        CastMessage.PayloadType.BINARY,
        payload_binary = payload.toByteString(),
    )

    private fun stringMessageWithEncodedSize(size: Int): CastMessage {
        var low = 0
        var high = size
        while (low <= high) {
            val middle = (low + high) ushr 1
            val candidate = stringMessage("x".repeat(middle))
            when {
                candidate.encode().size < size -> low = middle + 1
                candidate.encode().size > size -> high = middle - 1
                else -> return candidate
            }
        }
        error("Could not construct a CastMessage with encoded body size $size")
    }
}
