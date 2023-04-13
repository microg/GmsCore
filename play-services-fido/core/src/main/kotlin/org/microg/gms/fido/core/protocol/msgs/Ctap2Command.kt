/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import android.util.Base64
import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
import org.microg.gms.utils.toBase64
import java.io.ByteArrayInputStream
import java.io.InputStream

abstract class Ctap2Command<Q: Ctap2Request, R: Ctap2Response>(val request: Q) {
    val hasParameters: Boolean
        get() = request.parameters != null
    open val timeout: Long
        get() = 1000
    fun decodeResponse(bytes: ByteArray, offset: Int = 0): R =
        decodeResponse(ByteArrayInputStream(bytes, offset, bytes.size - offset))
    open fun decodeResponse(i: InputStream) = decodeResponse(CBORObject.Read(i))
    abstract fun decodeResponse(obj: CBORObject): R
}

interface Ctap2Response

abstract class Ctap2Request(val commandByte: Byte, val parameters: CBORObject? = null) {
    val payload: ByteArray = parameters?.EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical) ?: ByteArray(0)

    override fun toString(): String = "Ctap2Request(command=0x${commandByte.toString(16)}, " +
            "payload=${payload.toBase64(Base64.NO_WRAP)})"
}
