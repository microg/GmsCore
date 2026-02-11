/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import android.util.Base64
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialDescriptor
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialParameters
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialRpEntity
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialUserEntity
import org.microg.gms.fido.core.protocol.encodeAsCbor
import org.microg.gms.utils.toBase64

class AuthenticatorMakeCredentialCommand(request: AuthenticatorMakeCredentialRequest) :
    Ctap2Command<AuthenticatorMakeCredentialRequest, AuthenticatorMakeCredentialResponse>(request) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorMakeCredentialResponse.decodeFromCbor(obj)
    override val timeout: Long
        get() = 60000
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
) : Ctap2Request(COMMAND, CBORObject.NewMap().apply {
    set(0x01, clientDataHash.encodeAsCbor())
    set(0x02, rp.encodeAsCbor())
    set(0x03, user.encodeAsCbor())
    set(0x04, pubKeyCredParams.encodeAsCbor { it.encodeAsCbor() })
    if (excludeList.isNotEmpty()) set(0x05, excludeList.encodeAsCbor { it.encodeAsCbor() })
    if (extensions.isNotEmpty()) set(0x06, extensions.encodeAsCbor { it })
    if (options != null) set(0x07, options.encodeAsCbor())
    if (pinAuth != null) set(0x08, pinAuth.encodeAsCbor())
    if (pinProtocol != null) set(0x09, pinProtocol.encodeAsCbor())
}) {
    override fun toString() = "AuthenticatorMakeCredentialRequest(clientDataHash=0x${clientDataHash.toBase64(Base64.NO_WRAP)}, " +
            "rp=$rp,user=$user,pubKeyCredParams=[${pubKeyCredParams.joinToString()}]," +
            "excludeList=[${excludeList.joinToString()}],extensions=[${extensions.entries.joinToString()}]," +
            "options=$options,pinAuth=${pinAuth?.toBase64(Base64.NO_WRAP)},pinProtocol=$pinProtocol)"

    companion object {
        const val COMMAND: Byte = 0x01
        class Options(
            val residentKey: Boolean = false,
            val userVerification: Boolean = false
        ) {
            fun encodeAsCbor() = CBORObject.NewMap().apply {
                // Only encode non-default values
                if (residentKey) set("rk", residentKey.encodeAsCbor())
                if (userVerification) set("uv", userVerification.encodeAsCbor())
            }
        }

        fun decodeFromCbor(obj: CBORObject) = AuthenticatorMakeCredentialRequest(
            clientDataHash = obj.get(0x01).GetByteString(),
            rp = obj.get(0x02).decodeAsPublicKeyCredentialRpEntity(),
            user = obj.get(0x03).decodeAsPublicKeyCredentialUserEntity(),
            pubKeyCredParams = obj.get(0x04).values.map { it.decodeAsPublicKeyCredentialParameters() },
            excludeList = obj.get(0x05).values.map { it.decodeAsPublicKeyCredentialDescriptor() },
            options = obj.get(0x07)?.let { optObj ->
                Options(
                    residentKey = optObj["rk"]?.AsBoolean() ?: false,
                    userVerification = optObj["uv"]?.AsBoolean() ?: false,
                )
            })
    }
}

class AuthenticatorMakeCredentialResponse(
    val authData: ByteArray,
    val fmt: String,
    val attStmt: CBORObject
) : Ctap2Response {
    fun encodeAsCbor() = CBORObject.NewMap().apply {
        set(0x01, CBORObject.FromObject(fmt))
        set(0x02, CBORObject.FromObject(authData))
        set(0x03, attStmt)
    }

    companion object {
        fun decodeFromCbor(obj: CBORObject) = AuthenticatorMakeCredentialResponse(
            fmt = obj.get(0x01).AsString(),
            authData = obj.get(0x02).GetByteString(),
            attStmt = obj.get(0x03)
        )
    }
}
