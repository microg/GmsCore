/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBORObject
import java.io.ByteArrayInputStream
import java.io.InputStream

abstract class Ctap2Command<Q: Ctap2Request, R: Ctap2Response>(val request: Q) {
    val hasParameters: Boolean
        get() = request.parameters != null
    fun decodeResponse(bytes: ByteArray, offset: Int = 0): R =
        decodeResponse(ByteArrayInputStream(bytes, offset, bytes.size - offset))
    open fun decodeResponse(i: InputStream) = decodeResponse(CBORObject.Read(i))
    abstract fun decodeResponse(obj: CBORObject): R
}

interface Ctap2Response

abstract class Ctap2Request(val commandByte: Byte, val parameters: CBORObject? = null) {
    val payload: ByteArray = parameters?.EncodeToBytes() ?: ByteArray(0)
}
