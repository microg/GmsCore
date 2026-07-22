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
import org.microg.gms.fido.core.protocol.encodeAsCbor
import org.microg.gms.utils.ToStringHelper

class AuthenticatorGetInfoCommand : Ctap2Command<AuthenticatorGetInfoRequest, AuthenticatorGetInfoResponse>(AuthenticatorGetInfoRequest()) {
    override fun decodeResponse(obj: CBORObject) = AuthenticatorGetInfoResponse.decodeFromCbor(obj)
}

class AuthenticatorGetInfoRequest : Ctap2Request(Ctap2CommandCode.AuthenticatorGetInfo)

class AuthenticatorGetInfoResponse(
    val versions: List<String>? = null,
    val extensions: List<String>? = null,
    val aaguid: ByteArray? = null,
    val options: Options? = null,
    val maxMsgSize: Int? = null,
    val pinUvAuthProtocols: List<Int>? = null,
    val maxCredentialCountInList: Int? = null,
    val maxCredentialIdLength: Int? = null,
    val transports: List<String>? = null,
    val algorithms: List<PublicKeyCredentialParameters>? = null,
    val maxSerializedLargeBlobArray: Int? = null,
    val forcePINChange: Boolean? = null,
    val minPINLength: Int? = null,
    val firmwareVersion: Int? = null,
    val maxCredBlobLength: Int? = null,
    val maxRPIDsForSetMinPINLength: Int? = null,
    val preferredPlatformUvAttempts: Int? = null,
    val uvModality: Int? = null,
    val certifications: Map<String, Int>? = null,
    val remainingDiscoverableCredentials: Int? = null,
    val vendorPrototypeConfigCommands: List<Int>? = null,
) : Ctap2Response() {

    override fun encodePayloadAsCbor(): CBORObject = CBORObject.NewMap().apply {
        versions?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x01, it) }
        extensions?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x02, it) }
        aaguid?.encodeAsCbor()?.let { set(0x03, it) }
        options?.encodeAsCbor()?.let { set(0x04, it) }
        maxMsgSize?.let { set(0x05, it.encodeAsCbor()) }
        pinUvAuthProtocols?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x06, it) }
        maxCredentialCountInList?.let { set(0x07, it.encodeAsCbor()) }
        maxCredentialIdLength?.let { set(0x08, it.encodeAsCbor()) }
        transports?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x09, it) }
        algorithms?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x0a, it) }
        maxSerializedLargeBlobArray?.let { set(0x0b, it.encodeAsCbor()) }
        forcePINChange?.let { set(0x0c, it.encodeAsCbor()) }
        minPINLength?.let { set(0x0d, it.encodeAsCbor()) }
        firmwareVersion?.let { set(0x0e, it.encodeAsCbor()) }
        maxCredBlobLength?.let { set(0x0f, it.encodeAsCbor()) }
        maxRPIDsForSetMinPINLength?.let { set(0x10, it.encodeAsCbor()) }
        preferredPlatformUvAttempts?.let { set(0x11, it.encodeAsCbor()) }
        uvModality?.let { set(0x12, it.encodeAsCbor()) }
        certifications?.let { map ->
            CBORObject.NewMap().apply {
                map.forEach { (key, value) ->
                    set(key.encodeAsCbor(), value.encodeAsCbor())
                }
            }
        }?.let { set(0x13, it) }
        remainingDiscoverableCredentials?.let { set(0x14, it.encodeAsCbor()) }
        vendorPrototypeConfigCommands?.encodeAsCbor { it.encodeAsCbor() }?.let { set(0x15, it) }
    }

    companion object {
        class Options(
            val platformDevice: Boolean? = null,
            val residentKey: Boolean? = null,
            val clientPin: Boolean? = null,
            val userPresence: Boolean? = null,
            val userVerification: Boolean? = null,
            val pinUvAuthToken: Boolean? = null,
            val noMcGaPermissionsWithClientPin: Boolean? = null,
            val largeBlobs: Boolean? = null,
            val enterpriseAttestation: Boolean? = null,
            val bioEnroll: Boolean? = null,
            val userVerificationMgmtPreview: Boolean? = null,
            val uvBioEnroll: Boolean? = null,
            val authenticatorConfigSupported: Boolean? = null,
            val uvAcfg: Boolean? = null,
            val credentialManagementSupported: Boolean? = null,
            val credentialMgmtPreview: Boolean? = null,
            val setMinPINLengthSupported: Boolean? = null,
            val makeCredUvNotRqd: Boolean? = null,
            val alwaysUv: Boolean? = null,
        ) {
            fun encodeAsCbor(): CBORObject = CBORObject.NewMap().apply {
                if (platformDevice != null) set("plat", platformDevice.encodeAsCbor())
                if (residentKey != null) set("rk", residentKey.encodeAsCbor())
                if (clientPin != null) set("clientPin", clientPin.encodeAsCbor())
                if (userPresence != null) set("up", userPresence.encodeAsCbor())
                if (userVerification != null) set("uv", userVerification.encodeAsCbor())
                if (pinUvAuthToken != null) set("pinUvAuthToken", pinUvAuthToken.encodeAsCbor())
                if (noMcGaPermissionsWithClientPin != null) set("noMcGaPermissionsWithClientPin", noMcGaPermissionsWithClientPin.encodeAsCbor())
                if (largeBlobs != null) set("largeBlobs", largeBlobs.encodeAsCbor())
                if (enterpriseAttestation != null) set("ep", enterpriseAttestation.encodeAsCbor())
                if (bioEnroll != null) set("bioEnroll", bioEnroll.encodeAsCbor())
                if (userVerificationMgmtPreview != null) set("userVerificationMgmtPreview", userVerificationMgmtPreview.encodeAsCbor())
                if (uvBioEnroll != null) set("uvBioEnroll", uvBioEnroll.encodeAsCbor())
                if (authenticatorConfigSupported != null) set("authnrCfg", authenticatorConfigSupported.encodeAsCbor())
                if (uvAcfg != null) set("uvAcfg", uvAcfg.encodeAsCbor())
                if (credentialManagementSupported != null) set("credMgmt", credentialManagementSupported.encodeAsCbor())
                if (credentialMgmtPreview != null) set("credentialMgmtPreview", credentialMgmtPreview.encodeAsCbor())
                if (setMinPINLengthSupported != null) set("setMinPINLength", setMinPINLengthSupported.encodeAsCbor())
                if (makeCredUvNotRqd != null) set("makeCredUvNotRqd", makeCredUvNotRqd.encodeAsCbor())
                if (alwaysUv != null) set("alwaysUv", alwaysUv.encodeAsCbor())
            }

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
