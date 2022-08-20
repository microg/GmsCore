/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.encodeAsCbor

class AuthenticatorMakeCredentialCommand(request: AuthenticatorMakeCredentialRequest) :
    Ctap2Command<AuthenticatorMakeCredentialRequest, AuthenticatorMakeCredentialResponse>(request) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorMakeCredentialResponse.decodeFromCbor(obj)
}

class AuthenticatorMakeCredentialRequest(
    val clientDataHash: ByteArray,
    val rp: PublicKeyCredentialRpEntity,
    val user: PublicKeyCredentialUserEntity,
    val pubKeyCredParams: List<PublicKeyCredentialParameters>,
    val excludeList: List<PublicKeyCredentialDescriptor> = emptyList(),
    val extensions: Map<String, CBORObject> = emptyMap(),
    val options: Options? = null,
    val pinAuth: ByteArray? = null,
    val pinProtocol: Int? = null
) : Ctap2Request(0x01) {
    companion object {
        class Options(
            val residentKey: Boolean = false,
            val userVerification: Boolean = false
        ) {
            fun encodeAsCBOR() = CBORObject.NewMap().apply {
                set("rk", residentKey.encodeAsCbor())
                set("uv", residentKey.encodeAsCbor())
            }
        }
    }

    fun encodeAsCBOR() = CBORObject.NewMap().apply {
        set(0x01, clientDataHash.encodeAsCbor())
        set(0x02, rp.encodeAsCbor())
        set(0x03, user.encodeAsCbor())
        set(0x04, pubKeyCredParams.encodeAsCbor { it.encodeAsCbor() })
        if (excludeList.isNotEmpty()) set(0x05, excludeList.encodeAsCbor { it.encodeAsCbor() })
        if (extensions.isNotEmpty()) set(0x06, extensions.encodeAsCbor { it })
        if (options != null) set(0x07, options.encodeAsCBOR())
        if (pinAuth != null) set(0x08, pinAuth.encodeAsCbor())
        if (pinProtocol != null) set(0x09, pinProtocol.encodeAsCbor())
    }
}

class AuthenticatorMakeCredentialResponse(
    val authData: ByteArray,
    val fmt: String,
    val attStmt: ByteArray
) : Ctap2Response {
    companion object {
        fun decodeFromCbor(obj: CBORObject) = AuthenticatorMakeCredentialResponse(
            authData = obj.get(0x01).GetByteString(),
            fmt = obj.get(0x02).AsString(),
            attStmt = obj.get(0x03).GetByteString()
        )
    }
}
