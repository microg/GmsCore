/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.provider

import android.util.Base64
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensionsClientOutputs
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensionsCredPropsOutputs
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensionsPrfOutputs
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.FidoAppIdExtension
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import com.google.android.gms.fido.fido2.api.common.TokenBinding
import com.google.android.gms.fido.fido2.api.common.TokenBinding.TokenBindingStatus
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import com.google.android.gms.fido.fido2.api.common.UvmEntries
import com.google.android.gms.fido.fido2.api.common.UvmEntry
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.fido.core.protocol.AttestationObject
import org.microg.gms.fido.core.protocol.AuthenticatorData
import org.microg.gms.fido.core.protocol.CoseKey

const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
fun String.decodeBase64Url(): ByteArray = Base64.decode(this, BASE64_URL_FLAGS)
fun ByteArray.encodeBase64Url(): String = Base64.encodeToString(this, BASE64_URL_FLAGS)

fun PublicKeyCredential.toJson() = toJsonObject().toString()

fun PublicKeyCredential.toJsonObject() = JSONObject().apply {
    val response = response
    val id = id
    val rawId = rawId
    val clientExtensionResults = clientExtensionResults

    if (id != null) put("id", id)
    if (rawId != null && rawId.isNotEmpty()) put("rawId", rawId.encodeBase64Url())
    when (response) {
        is AuthenticatorAttestationResponse -> put("response", response.toJsonObject())
        is AuthenticatorAssertionResponse -> put("response", response.toJsonObject())
        is AuthenticatorErrorResponse -> put("error", response.toJsonObject())
    }
    if (authenticatorAttachment != null) put("authenticatorAttachment", authenticatorAttachment)
    put("clientExtensionResults", clientExtensionResults?.toJsonObject() ?: JSONObject())
    put("type", type)
}

fun AuthenticatorAttestationResponse.toJsonObject() = JSONObject().apply {
    val decodedAttestationObject = AttestationObject.decode(attestationObject)
    val decodedAuthenticatorData = AuthenticatorData.decode(decodedAttestationObject.authData)
    val publicKey = decodedAuthenticatorData.attestedCredentialData?.publicKey?.let { CoseKey.decode(it) }
        ?: throw IllegalArgumentException("Missing publicKey")

    put("clientDataJSON", clientDataJSON.encodeBase64Url())
    put("authenticatorData", decodedAttestationObject.authData.encodeBase64Url())
    put("transports", JSONArray(transports.asList().map { if (it == Transport.HYBRID.toString()) "hybrid" else it }))
    put("publicKey", publicKey.asCryptoKey()?.encoded?.encodeBase64Url())
    put("publicKeyAlgorithm", publicKey.algorithm.algoValue.toLong())
    put("attestationObject", attestationObject.encodeBase64Url())
}

fun AuthenticatorAssertionResponse.toJsonObject() = JSONObject().apply {
    val userHandle = userHandle

    put("clientDataJSON", clientDataJSON.encodeBase64Url())
    put("authenticatorData", authenticatorData.encodeBase64Url())
    put("signature", signature.encodeBase64Url())
    if (userHandle != null) put("userHandle", userHandle.encodeBase64Url())
}

fun AuthenticatorErrorResponse.toJsonObject() = JSONObject().apply {
    val errorMessage = errorMessage

    put("code", errorCodeAsInt)
    if (errorMessage != null) put("message", errorMessage)
}

fun AuthenticationExtensionsClientOutputs.toJsonObject() = JSONObject().apply {
    val uvmEntries = uvmEntries
    val credProps = credProps
    val prf = prf
    val txAuthSimple = txAuthSimple

    if (uvmEntries != null) put("uvm", uvmEntries.toJsonArray())
    if (credProps != null) put("credProps", credProps.toJsonObject())
    if (prf != null) put("prf", prf.toJsonObject())
    if (txAuthSimple != null) put("txAuthSimple", txAuthSimple)
}

fun UvmEntries.toJsonArray() = JSONArray().apply {
    val uvmEntryList = uvmEntryList
    if (uvmEntryList != null) {
        for (uvmEntry in uvmEntryList) {
            put(uvmEntry.toJsonArray())
        }
    }
}

fun UvmEntry.toJsonArray() = JSONArray().apply {
    put(userVerificationMethod)
    put(keyProtectionType)
    put(matcherProtectionType)
}

fun AuthenticationExtensionsCredPropsOutputs.toJsonObject() = JSONObject().apply {
    put("rk", isDiscoverableCredential)
}

fun AuthenticationExtensionsPrfOutputs.toJsonObject() = JSONObject().apply {
    val first = first
    val second = second

    if (isEnabled) put("enabled", true)
    if (first != null) put("first", first.encodeBase64Url())
    if (second != null) put("second", second.encodeBase64Url())
}

fun JSONObject.parsePublicKeyCredentialRequestOptions(): PublicKeyCredentialRequestOptions {
    val builder = PublicKeyCredentialRequestOptions.Builder()
    builder.setChallenge(getString("challenge").decodeBase64Url())
    if (has("timeout")) {
        builder.setTimeoutSeconds(getDouble("timeout") / 1000.0)
    } else if (has("timeoutSeconds")) {
        builder.setTimeoutSeconds(getDouble("timeoutSeconds"))
    }
    builder.setRpId(getString("rpId"))

    val allowCredentials = when {
        has("allowList") -> getJSONArray("allowList")
        has("allowCredentials") -> getJSONArray("allowCredentials")
        else -> null
    }
    if (allowCredentials != null) {
        val allowList = arrayListOf<PublicKeyCredentialDescriptor>()
        for (i in 0..<allowCredentials.length()) {
            allowList.add(allowCredentials.getJSONObject(i).parsePublicKeyCredentialDescriptor())
        }
        builder.setAllowList(allowList)
    }
    if (has("requestId")) {
        builder.setRequestId(getInt("requestId"))
    }
    if (has("tokenBinding")) {
        builder.setTokenBinding(getJSONObject("tokenBinding").parseTokenBinding())
    }
    if (has("userVerification")) {
        builder.setRequireUserVerification(UserVerificationRequirement.fromString(getString("userVerification")))
    }
    if (has("authenticationExtensions")) {
        builder.setAuthenticationExtensions(getJSONObject("authenticationExtensions").parseAuthenticationExtensions())
    } else if (has("extensions")) {
        builder.setAuthenticationExtensions(getJSONObject("extensions").parseAuthenticationExtensions())
    }
    if (has("longRequestId")) {
        builder.setLongRequestId(getLong("longRequestId"))
    }
    return builder.build()
}

fun JSONObject.parseTokenBinding() = when (TokenBindingStatus.fromString(getString("status"))) {
    TokenBindingStatus.SUPPORTED -> TokenBinding.SUPPORTED
    TokenBindingStatus.NOT_SUPPORTED -> TokenBinding.NOT_SUPPORTED
    TokenBindingStatus.PRESENT -> TokenBinding(getString("id"))
}

fun JSONObject.parseAuthenticationExtensions(): AuthenticationExtensions {
    val builder = AuthenticationExtensions.Builder()
    if (has("fidoAppIdExtension")) builder.setFido2Extension(FidoAppIdExtension(getJSONObject("fidoAppIdExtension").getString("appId")))
    if (has("appid")) builder.setFido2Extension(FidoAppIdExtension(getString("appId")))
    // TODO: Add support for other extensions
    return builder.build()
}

fun JSONObject.parsePublicKeyCredentialDescriptor() = PublicKeyCredentialDescriptor(
    getString("type"),
    getString("id").decodeBase64Url(),
    optJSONArray("transports")?.let { Transport.parseTransports(it) }
)

fun JSONObject.parsePublicKeyCredentialCreationOptions(): PublicKeyCredentialCreationOptions {
    val builder = PublicKeyCredentialCreationOptions.Builder()
    builder.setRp(getJSONObject("rp").parsePublicKeyCredentialRpEntity())
    builder.setUser(getJSONObject("user").parsePublicKeyCredentialUserEntity())
    builder.setChallenge(getString("challenge").decodeBase64Url())
    val pubKeyCredParams = getJSONArray("pubKeyCredParams")
    val parameters = arrayListOf<PublicKeyCredentialParameters>()
    for (i in 0..<pubKeyCredParams.length()) {
        parameters.add(pubKeyCredParams.getJSONObject(i).parsePublicKeyCredentialParameters())
    }
    builder.setParameters(parameters)
    if (has("timeout")) {
        builder.setTimeoutSeconds(getDouble("timeout") / 1000.0)
    }
    if (has("excludeCredentials")) {
        val excludeCredentials = getJSONArray("excludeCredentials")
        val excludeList = arrayListOf<PublicKeyCredentialDescriptor>()
        for (i in 0..<excludeCredentials.length()) {
            excludeList.add(excludeCredentials.getJSONObject(i).parsePublicKeyCredentialDescriptor())
        }
        builder.setExcludeList(excludeList)
    }
    if (has("authenticatorSelection")) {
        builder.setAuthenticatorSelection(getJSONObject("authenticatorSelection").parseAuthenticatorSelectionCriteria())
    }
    if (has("extensions")) {
        builder.setAuthenticationExtensions(getJSONObject("extensions").parseAuthenticationExtensions())
    }
    if (has("attestation")) {
        try {
            builder.setAttestationConveyancePreference(AttestationConveyancePreference.fromString(getString("attestation")))
        } catch (e: Exception) {
            builder.setAttestationConveyancePreference(AttestationConveyancePreference.NONE)
        }
    }
    return builder.build()
}

fun JSONObject.parsePublicKeyCredentialRpEntity() = PublicKeyCredentialRpEntity(
    getString("id"),
    getString("name"),
    if (has("icon")) optString("icon") else null
)

fun JSONObject.parsePublicKeyCredentialUserEntity() = PublicKeyCredentialUserEntity(
    getString("id").decodeBase64Url(),
    getString("name"),
    if (has("icon")) optString("icon") else null,
    optString("displayName")
)

fun JSONObject.parsePublicKeyCredentialParameters() = PublicKeyCredentialParameters(
    getString("type"),
    getInt("alg")
)

fun JSONObject.parseAuthenticatorSelectionCriteria() = AuthenticatorSelectionCriteria.Builder()
    .setAttachment(if (has("authenticatorAttachment")) optString("authenticatorAttachment").let { Attachment.fromString(it) } else null)
    .setRequireResidentKey(if (has("requireResidentKey")) optBoolean("requireResidentKey") else null)
    .setRequireUserVerification(if (has("userVerification")) optString("userVerification").let { UserVerificationRequirement.fromString(it) } else null)
    .setResidentKeyRequirement(if (has("residentKey")) optString("residentKey").let { ResidentKeyRequirement.fromString(it) } else null)
    .build()
