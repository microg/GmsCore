/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.AsInt32Sequence
import org.microg.gms.fido.core.protocol.AsStringSequence
import org.microg.gms.utils.ToStringHelper

class AuthenticatorGetInfoCommand : Ctap2Command<AuthenticatorGetInfoRequest, AuthenticatorGetInfoResponse>(AuthenticatorGetInfoRequest()) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorGetInfoResponse.decodeFromCbor(obj)
}

class AuthenticatorGetInfoRequest : Ctap2Request(0x04)

class AuthenticatorGetInfoResponse(
    val versions: List<String>,
    val extensions: List<String>,
    val aaguid: ByteArray,
    val options: Options,
    val maxMsgSize: Int?,
    val pinProtocols: List<Int>
) : Ctap2Response {

    companion object {
        class Options(
            val platformDevice: Boolean,
            val residentKey: Boolean,
            val clientPin: Boolean?,
            val userPresence: Boolean,
            val userVerification: Boolean?,
            val pinUvAuthToken: Boolean?,
            val noMcGaPermissionsWithClientPin: Boolean,
            val largeBlobs: Boolean?,
            val enterpriseAttestation: Boolean?,
            val bioEnroll: Boolean?,
            val userVerificationMgmtPreview: Boolean?,
            val uvBioEnroll: Boolean?,
            val authenticatorConfigSupported: Boolean?,
            val uvAcfg: Boolean?,
            val credentialManagementSupported: Boolean?,
            val credentialMgmtPreview: Boolean?,
            val setMinPINLengthSupported: Boolean?,
            val makeCredUvNotRqd: Boolean,
            val alwaysUv: Boolean?,
        ) {
            companion object {
                fun decodeFromCbor(map: CBORObject?) = Options(
                    platformDevice = map?.get("plat")?.AsBoolean() == true,
                    residentKey = map?.get("rk")?.AsBoolean() == true,
                    clientPin = map?.get("clientPin")?.AsBoolean(),
                    userPresence = map?.get("up")?.AsBoolean() != false,
                    userVerification = map?.get("uv")?.AsBoolean(),
                    pinUvAuthToken = map?.get("pinUvAuthToken")?.AsBoolean(),
                    noMcGaPermissionsWithClientPin = map?.get("noMcGaPermissionsWithClientPin")?.AsBoolean() == true,
                    largeBlobs = map?.get("largeBlobs")?.AsBoolean(),
                    enterpriseAttestation = map?.get("ep")?.AsBoolean(),
                    bioEnroll = map?.get("bioEnroll")?.AsBoolean(),
                    userVerificationMgmtPreview = map?.get("userVerificationMgmtPreview")?.AsBoolean(),
                    uvBioEnroll = map?.get("uvBioEnroll")?.AsBoolean(),
                    authenticatorConfigSupported = map?.get("authnrCfg")?.AsBoolean(),
                    uvAcfg = map?.get("uvAcfg")?.AsBoolean(),
                    credentialManagementSupported = map?.get("credMgmt")?.AsBoolean(),
                    credentialMgmtPreview = map?.get("credentialMgmtPreview")?.AsBoolean(),
                    setMinPINLengthSupported = map?.get("setMinPINLength")?.AsBoolean(),
                    makeCredUvNotRqd = map?.get("makeCredUvNotRqd")?.AsBoolean() == true,
                    alwaysUv = map?.get("alwaysUv")?.AsBoolean(),
                )
            }

            override fun toString(): String {
                return ToStringHelper.name("Options")
                    .field("platformDevice", platformDevice)
                    .field("residentKey", residentKey)
                    .field("clientPin", clientPin)
                    .field("userPresence", userPresence)
                    .field("userVerification", userVerification)
                    .field("pinUvAuthToken", pinUvAuthToken)
                    .field("noMcGaPermissionsWithClientPin", noMcGaPermissionsWithClientPin)
                    .field("largeBlobs", largeBlobs)
                    .field("enterpriseAttestation", enterpriseAttestation)
                    .field("bioEnroll", bioEnroll)
                    .field("userVerificationMgmtPreview", userVerificationMgmtPreview)
                    .field("uvBioEnroll", uvBioEnroll)
                    .field("authenticatorConfigSupported", authenticatorConfigSupported)
                    .field("uvAcfg", uvAcfg)
                    .field("credentialManagementSupported", credentialManagementSupported)
                    .field("credentialMgmtPreview", credentialMgmtPreview)
                    .field("setMinPINLengthSupported", setMinPINLengthSupported)
                    .field("makeCredUvNotRqd", makeCredUvNotRqd)
                    .field("alwaysUv", alwaysUv)
                    .end()
            }
        }

        fun decodeFromCbor(obj: CBORObject) = AuthenticatorGetInfoResponse(
            versions = obj.get(1)?.AsStringSequence()?.toList().orEmpty(),
            extensions = obj.get(2)?.AsStringSequence()?.toList().orEmpty(),
            aaguid = obj.get(3)?.GetByteString()
                ?: throw IllegalArgumentException("Not a valid response for authenticatorGetInfo"),
            options = Options.decodeFromCbor(obj.get(4)),
            maxMsgSize = obj.get(5)?.AsInt32Value(),
            pinProtocols = obj.get(6)?.AsInt32Sequence()?.toList().orEmpty()
        )
    }

    override fun toString(): String {
        return "AuthenticatorGetInfoResponse(versions=$versions, extensions=$extensions, aaguid=${aaguid.contentToString()}, options=$options, maxMsgSize=$maxMsgSize, pinProtocols=$pinProtocols)"
    }
}
