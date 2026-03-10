/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.protocol.msgs

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.upokecenter.cbor.CBORObject
import org.microg.gms.fido.core.protocol.AsInt32Sequence
import org.microg.gms.fido.core.protocol.AsStringSequence
import org.microg.gms.fido.core.protocol.decodeAsPublicKeyCredentialParameters
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
    val pinUvAuthProtocols: List<Int>,
    val maxCredentialCountInList: Int?,
    val maxCredentialIdLength: Int?,
    val transports: List<String>?,
    val algorithms: List<PublicKeyCredentialParameters>?,
    val maxSerializedLargeBlobArray: Int?,
    val forcePINChange: Boolean,
    val minPINLength: Int?,
    val firmwareVersion: Int?,
    val maxCredBlobLength: Int?,
    val maxRPIDsForSetMinPINLength: Int?,
    val preferredPlatformUvAttempts: Int?,
    val uvModality: Int?,
    val certifications: Map<String, Int>?,
    val remainingDiscoverableCredentials: Int?,
    val vendorPrototypeConfigCommands: List<Int>?,
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
            pinUvAuthProtocols = obj.get(6)?.AsInt32Sequence()?.toList().orEmpty(),
            maxCredentialCountInList = obj.get(7)?.AsInt32Value(),
            maxCredentialIdLength = obj.get(8)?.AsInt32Value(),
            transports = obj.get(9)?.AsStringSequence()?.toList(),
            algorithms = runCatching { obj.get(10)?.values?.map { it.decodeAsPublicKeyCredentialParameters() } }.getOrNull(),
            maxSerializedLargeBlobArray = obj.get(11)?.AsInt32Value(),
            forcePINChange = obj.get(12)?.AsBoolean() == true,
            minPINLength = obj.get(13)?.AsInt32Value(),
            firmwareVersion = obj.get(14)?.AsInt32Value(),
            maxCredBlobLength = obj.get(15)?.AsInt32Value(),
            maxRPIDsForSetMinPINLength = obj.get(16)?.AsInt32Value(),
            preferredPlatformUvAttempts = obj.get(17)?.AsInt32Value(),
            uvModality = obj.get(18)?.AsInt32Value(),
            certifications = obj.get(19)?.entries?.mapNotNull { runCatching { it.key.AsString() to it.value.AsInt32Value() }.getOrNull() }?.toMap(),
            remainingDiscoverableCredentials = obj.get(20)?.AsInt32Value(),
            vendorPrototypeConfigCommands = obj.get(21)?.AsInt32Sequence()?.toList(),
        )
    }

    override fun toString(): String {
        return ToStringHelper.name("AuthenticatorGetInfoResponse")
            .field("versions", versions)
            .field("extensions", extensions)
            .field("aaguid", aaguid)
            .field("options", options)
            .field("maxMsgSize", maxMsgSize)
            .field("pinUvAuthProtocols", pinUvAuthProtocols)
            .field("maxCredentialCountInList", maxCredentialCountInList)
            .field("maxCredentialIdLength", maxCredentialIdLength)
            .field("transports", transports)
            .field("algorithms", algorithms)
            .field("maxSerializedLargeBlobArray", maxSerializedLargeBlobArray)
            .field("forcePINChange", forcePINChange)
            .field("minPINLength", minPINLength)
            .field("firmwareVersion", firmwareVersion)
            .field("maxCredBlobLength", maxCredBlobLength)
            .field("maxRPIDsForSetMinPINLength", maxRPIDsForSetMinPINLength)
            .field("preferredPlatformUvAttempts", preferredPlatformUvAttempts)
            .field("uvModality", uvModality)
            .field("certifications", certifications)
            .field("remainingDiscoverableCredentials", remainingDiscoverableCredentials)
            .field("vendorPrototypeConfigCommands", vendorPrototypeConfigCommands)
            .end()
    }
}
