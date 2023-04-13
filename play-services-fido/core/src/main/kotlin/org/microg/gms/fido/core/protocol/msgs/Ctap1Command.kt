/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import android.util.Base64
import org.microg.gms.utils.toBase64
import java.io.ByteArrayInputStream
import java.io.InputStream

abstract class Ctap1Command<Q : Ctap1Request, S : Ctap1Response>(val request: Q) {
    val commandByte: Byte
        get() = request.commandByte
    fun decodeResponse(statusCode: Short, bytes: ByteArray, offset: Int = 0): S =
        decodeResponse(statusCode, ByteArrayInputStream(bytes, offset, bytes.size - offset))
    abstract fun decodeResponse(statusCode: Short, i: InputStream): S
}

abstract class Ctap1Request(
    val commandByte: Byte,
    val p1: Byte = 0,
    val p2: Byte = 0,
    val data: ByteArray
) {
    val apdu = encodeCommandApdu(0, commandByte, p1, p2, data, extended = true)

    override fun toString(): String = "Ctap1Request(command=0x${commandByte.toString(16)}, " +
            "p1=0x${p1.toString(16)}, " +
            "p2=0x${p2.toString(16)}, " +
            "data=${data.toBase64(Base64.NO_WRAP)})"
}

abstract class Ctap1Response(val statusCode: Short) {
    open fun encode(): ByteArray = throw UnsupportedOperationException()
}
