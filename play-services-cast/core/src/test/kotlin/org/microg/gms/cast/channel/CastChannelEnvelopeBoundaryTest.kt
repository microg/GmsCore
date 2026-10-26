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

/**
 * Wire-level coverage for envelope fields that dominate the CastMessage body size.
 *
 * The public send tests intentionally keep these fields short. These cases pin the lower-level writer contract so
 * source/destination/namespace bytes (including protobuf length prefixes) cannot disappear from size accounting.
 */
class CastChannelEnvelopeBoundaryTest {
    private enum class EnvelopeField { SOURCE, DESTINATION, NAMESPACE }

    private data class BoundaryCase(
        val field: EnvelopeField,
        val prefix: String,
        val payloadType: CastMessage.PayloadType,
    )

    private val listener = object : CastChannel.Listener {
        override fun onMessage(message: CastMessage) = Unit
        override fun onClosed(error: IOException?) = Unit
    }

    @Test
    fun envelopeFieldsCountTowardSerializedBodyLimitForStringAndBinaryMessages() {
        val cases = listOf(
            BoundaryCase(EnvelopeField.SOURCE, "", CastMessage.PayloadType.STRING),
            BoundaryCase(EnvelopeField.SOURCE, "é€😀", CastMessage.PayloadType.BINARY),
            BoundaryCase(EnvelopeField.DESTINATION, "", CastMessage.PayloadType.BINARY),
            BoundaryCase(EnvelopeField.DESTINATION, "é€😀", CastMessage.PayloadType.STRING),
            BoundaryCase(EnvelopeField.NAMESPACE, "urn:x-cast:test:é€😀:", CastMessage.PayloadType.STRING),
            BoundaryCase(EnvelopeField.NAMESPACE, "é€😀", CastMessage.PayloadType.BINARY),
        )

        for (boundaryCase in cases) {
            val accepted = messageWithEncodedSize(MAX_PAYLOAD_SIZE, boundaryCase)
            assertEquals(MAX_PAYLOAD_SIZE, CastMessage.ADAPTER.encodedSize(accepted))

            val acceptedBytes = ByteArrayOutputStream()
            invokeWriteLocked(channel(acceptedBytes), accepted)

            val frame = DataInputStream(ByteArrayInputStream(acceptedBytes.toByteArray()))
            assertEquals(MAX_PAYLOAD_SIZE, frame.readInt())
            val body = ByteArray(MAX_PAYLOAD_SIZE)
            frame.readFully(body)
            assertEquals(0, frame.available())
            assertArrayEquals(accepted.encode(), body)

            val decoded = CastMessage.ADAPTER.decode(body)
            assertEquals(accepted.source_id, decoded.source_id)
            assertEquals(accepted.destination_id, decoded.destination_id)
            assertEquals(accepted.namespace, decoded.namespace)
            assertEquals(accepted.payload_type, decoded.payload_type)
            assertEquals(accepted.payload_utf8, decoded.payload_utf8)
            assertEquals(accepted.payload_binary, decoded.payload_binary)

            val rejected = messageWithEncodedSize(MAX_PAYLOAD_SIZE + 1, boundaryCase)
            assertEquals(MAX_PAYLOAD_SIZE + 1, CastMessage.ADAPTER.encodedSize(rejected))
            val rejectedBytes = ByteArrayOutputStream()

            val error = org.junit.Assert.assertThrows(IOException::class.java) {
                invokeWriteLocked(channel(rejectedBytes), rejected)
            }

            assertTrue(error is MessageTooLargeException)
            assertTrue(error.message.orEmpty().contains("serialized body"))
            assertEquals(0, rejectedBytes.size())
        }
    }

    @Test
    fun envelopeFieldLengthVarintsAreIncludedInEncodedSize() {
        for (field in EnvelopeField.entries) {
            assertVarintTransition(field, 127, 128)
            assertVarintTransition(field, 16_383, 16_384)
        }
    }

    private fun assertVarintTransition(field: EnvelopeField, beforeLength: Int, afterLength: Int) {
        val before = messageWithField(field, "x".repeat(beforeLength), CastMessage.PayloadType.STRING)
        val after = messageWithField(field, "x".repeat(afterLength), CastMessage.PayloadType.STRING)
        assertEquals(beforeLength, selectedField(before, field).toByteArray(Charsets.UTF_8).size)
        assertEquals(afterLength, selectedField(after, field).toByteArray(Charsets.UTF_8).size)

        // One new field byte plus one new byte in the protobuf length varint.
        assertEquals(
            2,
            CastMessage.ADAPTER.encodedSize(after) - CastMessage.ADAPTER.encodedSize(before),
        )
    }

    private fun messageWithEncodedSize(targetSize: Int, boundaryCase: BoundaryCase): CastMessage {
        var low = 0
        var high = targetSize
        while (low <= high) {
            val middle = (low + high) ushr 1
            val value = boundaryCase.prefix + "x".repeat(middle)
            val candidate = messageWithField(boundaryCase.field, value, boundaryCase.payloadType)
            when {
                CastMessage.ADAPTER.encodedSize(candidate) < targetSize -> low = middle + 1
                CastMessage.ADAPTER.encodedSize(candidate) > targetSize -> high = middle - 1
                else -> return candidate
            }
        }
        error("Could not construct a CastMessage with encoded body size $targetSize for ${boundaryCase.field}")
    }

    private fun messageWithField(
        field: EnvelopeField,
        value: String,
        payloadType: CastMessage.PayloadType,
    ): CastMessage {
        val source = if (field == EnvelopeField.SOURCE) value else "sender-test"
        val destination = if (field == EnvelopeField.DESTINATION) value else "receiver-test"
        val namespace = if (field == EnvelopeField.NAMESPACE) value else "urn:x-cast:test"
        return when (payloadType) {
            CastMessage.PayloadType.STRING -> CastMessage(
                CastMessage.ProtocolVersion.CASTV2_1_0,
                source,
                destination,
                namespace,
                payloadType,
                payload_utf8 = "ok",
            )
            CastMessage.PayloadType.BINARY -> CastMessage(
                CastMessage.ProtocolVersion.CASTV2_1_0,
                source,
                destination,
                namespace,
                payloadType,
                payload_binary = byteArrayOf(0x01, 0x02, 0x03).toByteString(),
            )
        }
    }

    private fun selectedField(message: CastMessage, field: EnvelopeField): String = when (field) {
        EnvelopeField.SOURCE -> message.source_id
        EnvelopeField.DESTINATION -> message.destination_id
        EnvelopeField.NAMESPACE -> message.namespace
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
}
