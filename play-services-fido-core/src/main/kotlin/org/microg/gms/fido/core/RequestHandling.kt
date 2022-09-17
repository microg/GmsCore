/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ErrorCode.*
import com.google.common.net.InternetDomainName
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.microg.gms.fido.core.RequestOptionsType.REGISTER
import org.microg.gms.fido.core.RequestOptionsType.SIGN
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.utils.toBase64
import java.security.MessageDigest

class RequestHandlingException(val errorCode: ErrorCode, message: String? = null) : Exception(message)

enum class RequestOptionsType { REGISTER, SIGN }

val RequestOptions.registerOptions: PublicKeyCredentialCreationOptions
    get() = when (this) {
        is BrowserPublicKeyCredentialCreationOptions -> publicKeyCredentialCreationOptions
        is PublicKeyCredentialCreationOptions -> this
        else -> throw RequestHandlingException(DATA_ERR, "The request options are not valid")
    }

val RequestOptions.signOptions: PublicKeyCredentialRequestOptions
    get() = when (this) {
        is BrowserPublicKeyCredentialRequestOptions -> publicKeyCredentialRequestOptions
        is PublicKeyCredentialRequestOptions -> this
        else -> throw RequestHandlingException(DATA_ERR, "The request options are not valid")
    }

val RequestOptions.type: RequestOptionsType
    get() = when (this) {
        is PublicKeyCredentialCreationOptions, is BrowserPublicKeyCredentialCreationOptions -> REGISTER
        is PublicKeyCredentialRequestOptions, is BrowserPublicKeyCredentialRequestOptions -> RequestOptionsType.SIGN
        else -> throw RequestHandlingException(INVALID_STATE_ERR)
    }

val RequestOptions.webAuthnType: String
    get() = when (type) {
        REGISTER -> "webauthn.create"
        SIGN -> "webauthn.get"
    }

val RequestOptions.challenge: ByteArray
    get() = when (type) {
        REGISTER -> registerOptions.challenge
        SIGN -> signOptions.challenge
    }

val RequestOptions.rpId: String
    get() = when (type) {
        REGISTER -> registerOptions.rp.id
        SIGN -> signOptions.rpId
    }

val PublicKeyCredentialCreationOptions.skipAttestation: Boolean
    get() = attestationConveyancePreference in setOf(AttestationConveyancePreference.NONE, null)

fun RequestOptions.checkIsValid(context: Context) {
    if (type == REGISTER) {
        if (registerOptions.authenticatorSelection.requireResidentKey == true) {
            throw RequestHandlingException(
                NOT_SUPPORTED_ERR,
                "Resident credentials or empty 'allowCredentials' lists are not supported  at this time."
            )
        }
    }
    if (type == SIGN) {
        if (signOptions.allowList.isNullOrEmpty()) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Request doesn't have a valid list of allowed credentials.")
        }
    }
    if (authenticationExtensions?.fidoAppIdExtension?.appId != null) {
        val appId = authenticationExtensions.fidoAppIdExtension.appId
        if (!appId.startsWith("https://")) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "FIDO AppId must start with https://")
        }
        val uri = Uri.parse(appId)
        if (uri.host.isNullOrEmpty()) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "FIDO AppId must have a valid hostname")
        }
        if (InternetDomainName.from(uri.host).topDomainUnderRegistrySuffix() != InternetDomainName.from(rpId).topDomainUnderRegistrySuffix()) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "FIDO AppId must be same TLD+1")
        }
    }
}

fun RequestOptions.getWebAuthnClientData(callingPackage: String, origin: String): ByteArray {
    val obj = JSONObject()
        .put("type", webAuthnType)
        .put("challenge", challenge.toBase64(Base64.NO_PADDING, Base64.NO_WRAP, Base64.URL_SAFE))
        .put("androidPackageName", callingPackage)
        .put("tokenBinding", tokenBinding?.toJsonObject())
        .put("origin", origin)
    return obj.toString().encodeToByteArray()
}

fun getApplicationName(context: Context, options: RequestOptions, callingPackage: String): String = when (options) {
    is BrowserPublicKeyCredentialCreationOptions, is BrowserPublicKeyCredentialRequestOptions -> options.rpId
    else -> context.packageManager.getApplicationLabel(callingPackage).toString()
}

fun getApkHashOrigin(context: Context, packageName: String): String {
    val digest = context.packageManager.getFirstSignatureDigest(packageName, "SHA-256")
        ?: throw RequestHandlingException(NOT_ALLOWED_ERR, "Unknown package $packageName")
    return "android:apk-key-hash:${digest.toBase64(Base64.NO_PADDING, Base64.NO_WRAP, Base64.URL_SAFE)}"
}

fun getOrigin(context: Context, options: RequestOptions, callingPackage: String): String = when {
    options is BrowserRequestOptions -> {
        if (options.origin.scheme == null || options.origin.authority == null) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Bad url ${options.origin}")
        }
        "${options.origin.scheme}://${options.origin.authority}"
    }
    else -> getApkHashOrigin(context, callingPackage)
}

fun ByteArray.digest(md: String): ByteArray = MessageDigest.getInstance(md).digest(this)

fun getClientDataAndHash(
    context: Context,
    options: RequestOptions,
    callingPackage: String
): Pair<ByteArray, ByteArray> {
    val clientData: ByteArray?
    var clientDataHash = (options as? BrowserPublicKeyCredentialCreationOptions)?.clientDataHash
    if (clientDataHash == null) {
        clientData = options.getWebAuthnClientData(callingPackage, getOrigin(context, options, callingPackage))
        clientDataHash = clientData.digest("SHA-256")
    } else {
        clientData = "<invalid>".toByteArray()
    }
    return clientData to clientDataHash
}
