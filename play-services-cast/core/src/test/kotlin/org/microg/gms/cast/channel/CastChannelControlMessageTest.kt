/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.microg.gms.cast.proto.CastMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Exercise control dispatch with the same encoded frames consumed by the channel reader. */
class CastChannelControlMessageTest {
    @Test
    fun pingTypeRespondsIncludingJsonEscapes() {
        val result = readFrames(
            control(NAMESPACE_HEARTBEAT, """{"type":"PING"}"""),
            control(NAMESPACE_HEARTBEAT, """{"type":"P\u0049NG"}"""),
        )

        assertEquals(2, result.sent.size)
        for (message in result.sent) {
            assertEquals(NAMESPACE_HEARTBEAT, message.namespace)
            assertEquals(RECEIVER_ID, message.destination_id)
            assertEquals("""{"type":"PONG"}""", message.payload_utf8)
        }
    }

    @Test
    fun heartbeatMetadataDoesNotBecomeAPing() {
        val result = readFrames(
            control(NAMESPACE_HEARTBEAT, """{"type":"PONG","note":"PING"}"""),
            control(NAMESPACE_HEARTBEAT, """{"details":{"type":"PING"}}"""),
        )

        assertTrue(result.sent.isEmpty())
    }

    @Test
    fun closeTypeClosesOnlyTheNamedTransportIncludingJsonEscapes() {
        for (payload in listOf("""{"type":"CLOSE"}""", """{"type":"CL\u004fSE"}""")) {
            val result = readFrames(control(NAMESPACE_CONNECTION, payload, APPLICATION_ID))
            assertEquals(listOf(APPLICATION_ID), result.closedTransports)
        }
    }

    @Test
    fun connectionMetadataDoesNotCloseAnApplicationOrReceiver() {
        val applicationMessage = control("urn:x-cast:example", "message", APPLICATION_ID)
        val result = readFrames(
            control(NAMESPACE_CONNECTION, """{"type":"CONNECTED","note":"CLOSE"}""", APPLICATION_ID),
            control(NAMESPACE_CONNECTION, """{"details":{"type":"CLOSE"}}"""),
            applicationMessage,
        )

        assertTrue(result.closedTransports.isEmpty())
        assertEquals(listOf(applicationMessage), result.received)
    }

    @Test
    fun malformedAndBinaryControlPayloadsDoNotDispatchCommands() {
        val binaryPing = control(NAMESPACE_HEARTBEAT, """{"type":"PING"}""").copy(
            payload_type = CastMessage.PayloadType.BINARY,
            payload_binary = byteArrayOf(1).toByteString(),
        )
        val binaryClose = control(NAMESPACE_CONNECTION, """{"type":"CLOSE"}""", APPLICATION_ID).copy(
            payload_type = CastMessage.PayloadType.BINARY,
            payload_binary = byteArrayOf(1).toByteString(),
        )
        val result = readFrames(
            control(NAMESPACE_HEARTBEAT, "{\"type\":\"PING\""),
            control(NAMESPACE_CONNECTION, "{\"type\":\"CLOSE\"", APPLICATION_ID),
            binaryPing,
            binaryClose,
        )

        assertTrue(result.sent.isEmpty())
        assertTrue(result.closedTransports.isEmpty())
    }

    @Test
    fun receiverCloseStillEndsTheReadLoop() {
        val result = readFrames(
            control(NAMESPACE_CONNECTION, """{"type":"CLOSE"}"""),
            control("urn:x-cast:example", "not delivered", APPLICATION_ID),
        )

        assertTrue(result.received.isEmpty())
        assertEquals("Receiver closed the connection", result.closeError?.message)
    }

    @Test
    fun closeForAnotherSenderDoesNotCloseOurTransportOrReceiver() {
        val applicationMessage = control("urn:x-cast:example", "still connected", APPLICATION_ID)
        for (source in listOf(APPLICATION_ID, RECEIVER_ID)) {
            val result = readFrames(
                control(NAMESPACE_CONNECTION, """{"type":"CLOSE"}""", source).copy(destination_id = "sender-other"),
                applicationMessage,
            )

            assertTrue("Another sender's CLOSE must not remove our transport", result.closedTransports.isEmpty())
            assertEquals("Another sender's CLOSE must not stop our reader", listOf(applicationMessage), result.received)
        }

        val broadcastApplication = readFrames(
            control(NAMESPACE_CONNECTION, """{"type":"CLOSE"}""", APPLICATION_ID).copy(destination_id = BROADCAST_ID),
        )
        assertEquals(listOf(APPLICATION_ID), broadcastApplication.closedTransports)

        val broadcastReceiver = readFrames(
            control(NAMESPACE_CONNECTION, """{"type":"CLOSE"}""").copy(destination_id = BROADCAST_ID),
            applicationMessage,
        )
        assertTrue(broadcastReceiver.received.isEmpty())
        assertEquals("Receiver closed the connection", broadcastReceiver.closeError?.message)
    }

    @Test
    fun heartbeatOnlyAnswersMessagesForThisSenderOrBroadcast() {
        val result = readFrames(
            control(NAMESPACE_HEARTBEAT, """{"type":"PING"}""").copy(destination_id = "sender-other"),
            control(NAMESPACE_HEARTBEAT, """{"type":"PING"}"""),
            control(NAMESPACE_HEARTBEAT, """{"type":"PING"}""").copy(destination_id = BROADCAST_ID),
        )

        assertEquals("Only addressed and broadcast PINGs receive replies", 2, result.sent.size)
        for (message in result.sent) {
            assertEquals(NAMESPACE_HEARTBEAT, message.namespace)
            assertEquals(RECEIVER_ID, message.destination_id)
            assertEquals("""{"type":"PONG"}""", message.payload_utf8)
        }
    }

    private class ReadResult {
        val sent = ArrayList<CastMessage>()
        val received = ArrayList<CastMessage>()
        val closedTransports = ArrayList<String>()
        var closeError: IOException? = null
    }

    private fun readFrames(vararg messages: CastMessage): ReadResult {
        val result = ReadResult()
        val channel = CastChannel("127.0.0.1", CastChannel.DEFAULT_PORT, object : CastChannel.Listener {
            override fun onMessage(message: CastMessage) { result.received.add(message) }
            override fun onTransportClosed(transportId: String) { result.closedTransports.add(transportId) }
            override fun onClosed(error: IOException?) { result.closeError = error }
        })
        val output = ByteArrayOutputStream()
        CastChannel::class.java.getDeclaredField("output").apply { isAccessible = true }
            .set(channel, DataOutputStream(output))
        channel.connectTransport(APPLICATION_ID)
        output.reset()

        val framed = ByteArrayOutputStream()
        DataOutputStream(framed).use { stream ->
            for (message in messages) {
                val body = message.encode()
                stream.writeInt(body.size)
                stream.write(body)
            }
        }
        CastChannel::class.java.getDeclaredMethod("readLoop", DataInputStream::class.java)
            .apply { isAccessible = true }
            .invoke(channel, DataInputStream(ByteArrayInputStream(framed.toByteArray())))

        DataInputStream(ByteArrayInputStream(output.toByteArray())).use { stream ->
            while (stream.available() > 0) {
                val body = ByteArray(stream.readInt())
                stream.readFully(body)
                result.sent.add(CastMessage.ADAPTER.decode(body))
            }
        }
        return result
    }

    private fun control(namespace: String, payload: String, source: String = RECEIVER_ID) = CastMessage(
        CastMessage.ProtocolVersion.CASTV2_1_0,
        source,
        "sender-0",
        namespace,
        CastMessage.PayloadType.STRING,
        payload_utf8 = payload,
    )

    companion object {
        private const val APPLICATION_ID = "application-1"
    }
}
