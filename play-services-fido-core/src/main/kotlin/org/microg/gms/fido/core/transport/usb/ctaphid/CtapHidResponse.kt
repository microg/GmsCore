/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import java.nio.ByteBuffer
import java.nio.ByteOrder

abstract class CtapHidResponse(commandId: Byte, data: ByteArray) : CtapHidMessage(commandId, data)

class CtapHidPingResponse(data: ByteArray) : CtapHidResponse(0x01, data)

class CtapHidMessageResponse(val statusCode: Byte, val payload: ByteArray) :
    CtapHidResponse(0x03, byteArrayOf(statusCode) + payload)

class CtapHidInitResponse(
    val nonce: ByteArray,
    val channelId: Int,
    val protocolVersion: Byte,
    val majorDeviceVersion: Byte,
    val minorDeviceVersion: Byte,
    val buildDeviceVersion: Byte,
    val capabilities: Byte
) : CtapHidResponse(0x06, ByteBuffer.allocate(17).apply {
    order(ByteOrder.BIG_ENDIAN)
    position(0)
    put(nonce)
    putInt(channelId)
    put(protocolVersion)
    put(majorDeviceVersion)
    put(minorDeviceVersion)
    put(buildDeviceVersion)
    put(capabilities)
}.array()) {
    val deviceVersion: String = "$majorDeviceVersion.$minorDeviceVersion.$buildDeviceVersion"

    companion object {
        const val CAPABILITY_WINK: Byte = 0x01
        const val CAPABILITY_CBOR: Byte = 0x04
        const val CAPABILITY_NMSG: Byte = 0x08
    }
}

class CtapHidCborResponse(val statusCode: Byte, val payload: ByteArray) :
    CtapHidResponse(0x10, byteArrayOf(statusCode) + payload)

class CtapHidErrorResponse(val errorCode: Byte) : CtapHidResponse(0x3f, byteArrayOf(errorCode)) {
    companion object {
        const val ERR_INVALID_CMD: Byte = 0x01
        const val ERR_INVALID_PAR: Byte = 0x02
        const val ERR_INVALID_LEN: Byte = 0x03
        const val ERR_INVALID_SEQ: Byte = 0x04
        const val ERR_MSG_TIMEOUT: Byte = 0x05
        const val ERR_CHANNEL_BUSY: Byte = 0x06
        const val ERR_LOCK_REQUIRED: Byte = 0x0A
        const val ERR_INVALID_CHANNEL: Byte = 0x0B
        const val ERR_OTHER: Byte = 0x7F
    }
}
