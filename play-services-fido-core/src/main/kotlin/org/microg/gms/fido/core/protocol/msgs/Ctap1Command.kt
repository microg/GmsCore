/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

abstract class Ctap1Command<Q : Ctap1Request, S : Ctap1Response>(val request: Q) {
    val commandByte: Byte
        get() = request.commandByte
}

abstract class Ctap1Request(
    val commandByte: Byte,
    val parameter1: Byte = 0,
    val parameter2: Byte = 0,
    val data: ByteArray
) {
    val payload = byteArrayOf(
        0,
        commandByte,
        parameter1,
        parameter2,
        (data.size shr 16).toByte(),
        (data.size shr 8).toByte(),
        data.size.toByte()
    ) + data
}

abstract class Ctap1Response(val statusCode: Byte) {
    open fun encode(): ByteArray = throw UnsupportedOperationException()
}
