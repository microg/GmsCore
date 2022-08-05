/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import java.nio.ByteBuffer
import java.nio.ByteOrder

interface CtapHidPacket {
    val bytes: ByteArray
    val data: ByteArray
}

class CtapHidInitializationPacket(
    val channelIdentifier: Int,
    val commandId: Byte,
    val payloadLength: Short,
    override val data: ByteArray,
    val packetSize: Int
) : CtapHidPacket {

    override val bytes: ByteArray = ByteBuffer.allocate(packetSize).apply {
        order(ByteOrder.BIG_ENDIAN)
        position(0)
        putInt(channelIdentifier)
        put(commandId)
        putShort(payloadLength)
        put(data)
    }.array()

    init {
        if (data.size > packetSize - 7) throw IllegalArgumentException("Too much data for packet size")
        if (commandId >= 0) throw IllegalArgumentException("7-bit must be set on initialization packet")
    }

    companion object {
        fun decode(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).run {
            CtapHidInitializationPacket(
                int,
                get(),
                short,
                ByteArray(bytes.size - 7).also { get(it) },
                bytes.size
            )
        }
    }
}

class CtapHidContinuationPacket(
    val channelIdentifier: Int,
    val sequenceNumber: Byte,
    override val data: ByteArray,
    val packetSize: Int
) : CtapHidPacket {
    override val bytes: ByteArray = ByteBuffer.allocate(packetSize).apply {
        order(ByteOrder.BIG_ENDIAN)
        position(0)
        putInt(channelIdentifier)
        put(sequenceNumber)
        put(data)
    }.array()

    init {
        if (data.size > packetSize - 5) throw IllegalArgumentException("Too much data for packet size")
        if (sequenceNumber < 0) throw IllegalArgumentException("7-bit must not be set on continuation packet")
    }

    companion object {
        fun decode(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).run {
            CtapHidContinuationPacket(
                int,
                get(),
                ByteArray(bytes.size - 5).also { get(it) },
                bytes.size
            )
        }
    }
}
