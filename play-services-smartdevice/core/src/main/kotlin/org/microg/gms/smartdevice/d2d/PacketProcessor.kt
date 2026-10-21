/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import okio.ByteString.Companion.toByteString
import org.microg.gms.smartdevice.d2d.proto.Packet
import java.io.IOException
import java.util.TreeMap

class PacketProcessor(
    private val chunkSize: Int = D2D_DEFAULT_CHUNK_SIZE,
) {
    private var nextMessageId = 0
    private val pending = HashMap<Int, PendingMessage>()

    init {
        require(chunkSize > 0) { "chunkSize must be > 0" }
    }

    @Synchronized
    fun split(payload: ByteArray): List<ByteArray> {
        if (payload.isEmpty()) return emptyList()
        val messageId = ++nextMessageId
        val result = ArrayList<ByteArray>((payload.size + chunkSize - 1) / chunkSize)
        var packetIndex = 0
        var offset = 0
        while (offset < payload.size) {
            val end = minOf(offset + chunkSize, payload.size)
            result += encodePacket(
                Packet(
                    messageId = messageId,
                    packetIndex = packetIndex++,
                    totalLength = payload.size,
                    payload = payload.copyOfRange(offset, end).toByteString(),
                ),
            )
            offset = end
        }
        return result
    }

    @Synchronized
    @Throws(IOException::class)
    fun accept(wire: ByteArray): ByteArray? {
        val packet = decodePacket(wire)
        val messageId = packet.messageId ?: throw IOException("Packet message id missing")
        val packetIndex = packet.packetIndex ?: throw IOException("Packet index missing")
        val totalLength = packet.totalLength ?: throw IOException("Packet total length missing")
        val payload = packet.payload?.toByteArray() ?: throw IOException("Packet payload missing")
        if (totalLength <= 0) {
            throw IOException("Invalid packet total length $totalLength")
        }
        if (packetIndex < 0 || payload.isEmpty()) {
            throw IOException("Invalid packet index=$packetIndex size=${payload.size}")
        }

        val state = pending.getOrPut(messageId) { PendingMessage(totalLength) }
        if (state.totalLength != totalLength) {
            pending.remove(messageId)
            throw IOException("Packet total length changed for message $messageId")
        }
        state.packets[packetIndex]?.let { existing ->
            if (!existing.contentEquals(payload)) {
                pending.remove(messageId)
                throw IOException("Conflicting duplicate packet $packetIndex")
            }
            return null
        }
        state.packets[packetIndex] = payload
        state.receivedBytes += payload.size
        if (state.receivedBytes > state.totalLength) {
            pending.remove(messageId)
            throw IOException("Packet data exceeds total length for message $messageId")
        }
        if (state.receivedBytes != state.totalLength) return null

        val complete = ByteArray(state.totalLength)
        var offset = 0
        var expectedIndex = 0
        for ((index, bytes) in state.packets) {
            if (index != expectedIndex) {
                pending.remove(messageId)
                throw IOException("Missing packet index $expectedIndex for message $messageId")
            }
            expectedIndex++
            if (bytes.size > complete.size - offset) {
                pending.remove(messageId)
                throw IOException("Packet data overflows message $messageId")
            }
            bytes.copyInto(complete, offset)
            offset += bytes.size
        }
        if (offset != complete.size) {
            pending.remove(messageId)
            throw IOException("Packet data is incomplete for message $messageId")
        }
        pending.remove(messageId)
        return complete
    }

    internal fun decodePacket(wire: ByteArray): Packet {
        if (wire.isEmpty()) throw IOException("Empty packet")
        return Packet.ADAPTER.decode(wire)
    }

    private fun encodePacket(packet: Packet): ByteArray = Packet.ADAPTER.encode(packet)

    private data class PendingMessage(
        val totalLength: Int,
        val packets: TreeMap<Int, ByteArray> = TreeMap(),
        var receivedBytes: Int = 0,
    )
}
