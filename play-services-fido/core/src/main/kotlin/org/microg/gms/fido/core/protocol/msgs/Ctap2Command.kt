/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBOREncodeOptions
import com.upokecenter.cbor.CBORObject
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

abstract class Ctap2Response {
    abstract fun encodePayloadAsCbor(): CBORObject
    fun encodePayload(): ByteArray = encodePayloadAsCbor().EncodeToBytes()

    override fun toString(): String = "Ctap2Response(${encodePayloadAsCbor()})"
}

abstract class Ctap2Request(val commandCode: Ctap2CommandCode, val parameters: CBORObject? = null) {
    val commandByte: Byte
        get() = commandCode.byte

    open fun encodeParametersAsCbor() = parameters
    fun encodeParameters(): ByteArray = encodeParametersAsCbor()?.EncodeToBytes(CBOREncodeOptions.DefaultCtap2Canonical) ?: ByteArray(0)

    override fun toString(): String = "Ctap2Request(command=0x${commandByte.toString(16)}, parameters=$parameters)"
}

enum class Ctap2CommandCode(val byte: Byte) {
    AuthenticatorMakeCredential(0x01),
    AuthenticatorGetAssertion(0x02),
    AuthenticatorGetNextAssertion(0x08),
    AuthenticatorGetInfo(0x04),
    AuthenticatorClientPIN(0x06),
    AuthenticatorReset(0x07),
    AuthenticatorBioEnrollment(0x09),
    AuthenticatorCredentialManagement(0x0a),
    AuthenticatorSelection(0x0b),
    AuthenticatorLargeBlobs(0x0b),
    AuthenticatorConfig(0x0d),
}
