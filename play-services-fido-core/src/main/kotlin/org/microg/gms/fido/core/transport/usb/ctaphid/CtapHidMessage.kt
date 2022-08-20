/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import android.util.Base64
import org.microg.gms.utils.toBase64
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.min

open class CtapHidMessage(val commandId: Byte, val data: ByteArray = ByteArray(0)) {
    init {
        if (data.size > Short.MAX_VALUE) throw IllegalArgumentException("Request too large")
    }

    fun encodePackets(channelIdentifier: Int, packetSize: Int): List<CtapHidPacket> {
        val packets = arrayListOf<CtapHidPacket>()
        val initializationDataSize = packetSize - 7
        val continuationDataSize = packetSize - 5
        var position = 0
        var nextPosition = min(data.size, initializationDataSize)
        packets.add(
            CtapHidInitializationPacket(
                channelIdentifier,
                commandId or 0x80.toByte(),
                data.size.toShort(),
                data.sliceArray(position until nextPosition),
                packetSize
            )
        )
        var sequenceNumber: Byte = 0
        while (nextPosition < data.size) {
            position = nextPosition
            nextPosition = min(data.size, position + continuationDataSize)
            packets.add(
                CtapHidContinuationPacket(
                    channelIdentifier,
                    sequenceNumber++,
                    data.sliceArray(position until nextPosition),
                    packetSize
                )
            )
        }
        return packets
    }

    override fun toString(): String =
        "CtapHidMessage(commandId=0x${commandId.toString(16)}, data=${data.toBase64(Base64.NO_WRAP)})"

    companion object {
        fun decode(packets: List<CtapHidPacket>): CtapHidMessage {
            val initializationPacket = packets.first() as? CtapHidInitializationPacket
                ?: throw IllegalArgumentException("First packet must ba an initialization packet")
            val data = packets.map { it.data }.fold(ByteArray(0)) { a, b -> a + b }
                .sliceArray(0 until initializationPacket.payloadLength)
            return CtapHidMessage(initializationPacket.commandId and 0x7f, data)
        }
    }
}

class CtapHidKeepAliveMessage(val status: Byte) : CtapHidMessage(COMMAND_ID, byteArrayOf(status)) {
    companion object {
        const val STATUS_PROCESSING: Byte = 1
        const val STATUS_UPNEEDED: Byte = 2
        const val COMMAND_ID: Byte = 0x3b
    }
}
