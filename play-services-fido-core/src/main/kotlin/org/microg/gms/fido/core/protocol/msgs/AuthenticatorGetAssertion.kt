/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialDescriptor
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialUserEntity
import org.microg.gms.fido.core.protocol.encodeAsCbor

class AuthenticatorGetAssertionCommand(request: AuthenticatorGetAssertionRequest) :
    Ctap2Command<AuthenticatorGetAssertionRequest, AuthenticatorGetAssertionResponse>(request) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorGetAssertionResponse.decodeFromCbor(obj)
}

class AuthenticatorGetAssertionRequest(
    val rpId: String,
    val clientDataHash: ByteArray,
    val allowList: List<PublicKeyCredentialDescriptor> = emptyList(),
    val extensions: Map<String, CBORObject> = emptyMap(),
    val options: Options? = null,
    val pinAuth: ByteArray? = null,
    val pinProtocol: Int? = null
) : Ctap2Request(0x03) {
    companion object {
        class Options(
            val userPresence: Boolean = true,
            val userVerification: Boolean = false
        ) {
            fun encodeAsCBOR(): CBORObject = CBORObject.NewMap().apply {
                set("up", userPresence.encodeAsCbor())
                set("uv", userVerification.encodeAsCbor())
            }
        }
    }

    fun encodeAsCBOR() = CBORObject.NewMap().apply {
        set(0x01, rpId.encodeAsCbor())
        set(0x02, clientDataHash.encodeAsCbor())
        if (allowList.isNotEmpty()) set(0x03, allowList.encodeAsCbor { it.encodeAsCbor() })
        if (extensions.isNotEmpty()) set(0x04, extensions.encodeAsCbor { it })
        if (options != null) set(0x05, options.encodeAsCBOR())
        if (pinAuth != null) set(0x06, pinAuth.encodeAsCbor())
        if (pinProtocol != null) set(0x07, pinProtocol.encodeAsCbor())
    }
}

class AuthenticatorGetAssertionResponse(
    val credential: PublicKeyCredentialDescriptor?,
    val authData: ByteArray,
    val signature: ByteArray,
    val user: PublicKeyCredentialUserEntity?,
    val numberOfCredentials: Int?
) : Ctap2Response {

    companion object {
        fun decodeFromCbor(obj: CBORObject) = AuthenticatorGetAssertionResponse(
            credential = obj.get(0x01)?.decodeAsPublicKeyCredentialDescriptor(),
            authData = obj.get(0x02).GetByteString(),
            signature = obj.get(0x03).GetByteString(),
            user = obj.get(0x04)?.decodeAsPublicKeyCredentialUserEntity(),
            numberOfCredentials = obj.get(0x05)?.AsInt32Value()
        )
    }
}
