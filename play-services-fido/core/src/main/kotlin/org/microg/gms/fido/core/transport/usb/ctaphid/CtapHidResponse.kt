/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.usb.ctaphid

import android.util.Base64
import org.microg.gms.fido.core.protocol.msgs.decodeResponseApdu
import org.microg.gms.utils.toBase64
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class CtapHidResponse(commandId: Byte, data: ByteArray) : CtapHidMessage(commandId, data) {
    override fun toString(): String =
        "CtapHidResponse(commandId=0x${commandId.toString(16)}, data=${data.toBase64(Base64.NO_WRAP)})"

    companion object {
        val responseTypes = mapOf(
            CtapHidPingResponse.COMMAND_ID to CtapHidPingResponse::parse,
            CtapHidMessageResponse.COMMAND_ID to CtapHidMessageResponse::parse,
            CtapHidInitResponse.COMMAND_ID to CtapHidInitResponse::parse,
            CtapHidWinkResponse.COMMAND_ID to CtapHidWinkResponse::parse,
            CtapHidCborResponse.COMMAND_ID to CtapHidCborResponse::parse,
            CtapHidErrorResponse.COMMAND_ID to CtapHidErrorResponse::parse,
        )

        fun parse(message: CtapHidMessage): CtapHidResponse =
            responseTypes[message.commandId]?.invoke(message) ?: CtapHidResponse(message.commandId, message.data)
    }
}

class CtapHidPingResponse(data: ByteArray) : CtapHidResponse(COMMAND_ID, data) {
    override fun toString(): String = "CtapHidPingResponse(data=${data.toBase64(Base64.NO_WRAP)})"

    companion object {
        const val COMMAND_ID: Byte = 0x01

        fun parse(message: CtapHidMessage): CtapHidPingResponse {
            require(message.commandId == COMMAND_ID)
            return CtapHidPingResponse(message.data)
        }
    }
}

class CtapHidMessageResponse(val statusCode: Short, val payload: ByteArray) :
    CtapHidResponse(COMMAND_ID, byteArrayOf((statusCode.toInt() shr 8).toByte(), statusCode.toByte()) + payload) {
    override fun toString(): String =
        "CtapHidMessageResponse(statusCode=0x${statusCode.toString(16)}, payload=${payload.toBase64(Base64.NO_WRAP)})"

    companion object {
        const val COMMAND_ID: Byte = 0x03

        fun parse(message: CtapHidMessage): CtapHidMessageResponse {
            require(message.commandId == COMMAND_ID)
            val (statusCode, payload) = decodeResponseApdu(message.data)
            return CtapHidMessageResponse(statusCode, payload)
        }
    }
}

class CtapHidInitResponse(
    val nonce: ByteArray,
    val channelId: Int,
    val protocolVersion: Byte,
    val majorDeviceVersion: Byte,
    val minorDeviceVersion: Byte,
    val buildDeviceVersion: Byte,
    val capabilities: Byte
) : CtapHidResponse(COMMAND_ID, ByteBuffer.allocate(17).apply {
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

    override fun toString(): String = "CtapHidInitResponse(nonce=0x${nonce.toBase64(Base64.NO_WRAP)}, " +
            "channelId=0x${channelId.toString(16)}, " +
            "protocolVersion=0x${protocolVersion.toString(16)}, " +
            "version=$deviceVersion, " +
            "capabilities=0x${capabilities.toString(16)})"

    companion object {
        const val COMMAND_ID: Byte = 0x06

        const val CAPABILITY_WINK: Byte = 0x01
        const val CAPABILITY_CBOR: Byte = 0x04
        const val CAPABILITY_NMSG: Byte = 0x08

        fun parse(message: CtapHidMessage): CtapHidInitResponse {
            require(message.commandId == COMMAND_ID)
            require(message.data.size == 17)
            return ByteBuffer.wrap(message.data).order(ByteOrder.BIG_ENDIAN).run {
                CtapHidInitResponse(
                    ByteArray(8).also { get(it) },
                    int,
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
        }
    }
}

class CtapHidWinkResponse : CtapHidResponse(COMMAND_ID, ByteArray(0)) {
    override fun toString(): String = "CtapHidWinkResponse()"

    companion object {
        const val COMMAND_ID: Byte = 0x08

        fun parse(message: CtapHidMessage): CtapHidWinkResponse {
            require(message.commandId == COMMAND_ID)
            require(message.data.isEmpty())
            return CtapHidWinkResponse()
        }
    }
}

class CtapHidCborResponse(val statusCode: Byte, val payload: ByteArray) :
    CtapHidResponse(COMMAND_ID, byteArrayOf(statusCode) + payload) {
    override fun toString(): String =
        "CtapHidCborResponse(statusCode=0x${statusCode.toString(16)}, payload=${payload.toBase64(Base64.NO_WRAP)})"

    companion object {
        const val COMMAND_ID: Byte = 0x10

        fun parse(message: CtapHidMessage): CtapHidCborResponse {
            require(message.commandId == COMMAND_ID)
            return CtapHidCborResponse(message.data[0], message.data.sliceArray(1 until message.data.size))
        }
    }
}

class CtapHidErrorResponse(val errorCode: Byte) : CtapHidResponse(COMMAND_ID, byteArrayOf(errorCode)) {
    override fun toString(): String = "CtapHidErrorResponse(errorCode=0x${errorCode.toString(16)})"

    companion object {
        const val COMMAND_ID: Byte = 0x3f
        const val ERR_INVALID_CMD: Byte = 0x01
        const val ERR_INVALID_PAR: Byte = 0x02
        const val ERR_INVALID_LEN: Byte = 0x03
        const val ERR_INVALID_SEQ: Byte = 0x04
        const val ERR_MSG_TIMEOUT: Byte = 0x05
        const val ERR_CHANNEL_BUSY: Byte = 0x06
        const val ERR_LOCK_REQUIRED: Byte = 0x0A
        const val ERR_INVALID_CHANNEL: Byte = 0x0B
        const val ERR_OTHER: Byte = 0x7F

        fun parse(message: CtapHidMessage): CtapHidErrorResponse {
            require(message.commandId == COMMAND_ID)
            require(message.data.size == 1)
            return CtapHidErrorResponse(message.data[0])
        }
    }
}
