/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

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
                commandId,
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
}

class CtapHidKeepAliveMessage(val status: Byte) : CtapHidMessage(0x3b, byteArrayOf(status)) {
    companion object {
        const val STATUS_PROCESSING: Byte = 1
        const val STATUS_UPNEEDED: Byte = 2
    }
}
