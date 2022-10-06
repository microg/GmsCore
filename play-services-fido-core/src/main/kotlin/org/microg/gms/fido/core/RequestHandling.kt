/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ErrorCode.*
import com.google.common.net.InternetDomainName
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.fido.core.RequestOptionsType.REGISTER
import org.microg.gms.fido.core.RequestOptionsType.SIGN
import org.microg.gms.utils.*
import java.net.HttpURLConnection
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
        is PublicKeyCredentialRequestOptions, is BrowserPublicKeyCredentialRequestOptions -> SIGN
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

fun topDomainOf(string: String?) =
    string?.let { InternetDomainName.from(string).topDomainUnderRegistrySuffix().toString() }

fun <T> JSONArray.map(fn: JSONArray.(Int) -> T): List<T> = (0 until length()).map { fn(this, it) }

private suspend fun isFacetIdTrusted(context: Context, facetId: String, appId: String): Boolean {
    val trustedFacets = try {
        val deferred = CompletableDeferred<JSONObject>()
        HttpURLConnection.setFollowRedirects(false)
        Volley.newRequestQueue(context)
            .add(JsonObjectRequest(appId, { deferred.complete(it) }, { deferred.completeExceptionally(it) }))
        val obj = deferred.await()
        val arr = obj.getJSONArray("trustedFacets")
        if (arr.length() > 1) {
            // Unsupported
            emptyList()
        } else {
            arr.getJSONObject(0).getJSONArray("ids").map(JSONArray::getString)
        }
    } catch (e: Exception) {
        // Ignore and fail
        emptyList()
    }
    return trustedFacets.contains(facetId)
}

private const val ASSET_LINK_REL = "delegate_permission/common.get_login_creds"
private suspend fun isAssetLinked(context: Context, rpId: String, facetId: String, packageName: String?): Boolean {
    try {
        if (!facetId.startsWith("android:apk-key-hash-sha256:")) return false
        val fp = Base64.decode(facetId.substring(28), HASH_BASE64_FLAGS).toHexString(":")
        val deferred = CompletableDeferred<JSONArray>()
        HttpURLConnection.setFollowRedirects(true)
        val url = "https://$rpId/.well-known/assetlinks.json"
        Volley.newRequestQueue(context)
            .add(JsonArrayRequest(url, { deferred.complete(it) }, { deferred.completeExceptionally(it) }))
        val arr = deferred.await()
        for (obj in arr.map(JSONArray::getJSONObject)) {
            if (!obj.getJSONArray("relation").map(JSONArray::getString).contains(ASSET_LINK_REL)) continue
            val target = obj.getJSONObject("target")
            if (target.getString("namespace") != "android_app") continue
            if (packageName != null && target.getString("package_name") != packageName) continue
            for (fingerprint in target.getJSONArray("sha256_cert_fingerprints").map(JSONArray::getString)) {
                if (fingerprint.equals(fp, ignoreCase = true)) return true
            }
        }
        return false
    } catch (e: Exception) {
        return false
    }
}

// Note: This assumes the RP ID is allowed
private suspend fun isAppIdAllowed(context: Context, appId: String, facetId: String, rpId: String): Boolean {
    return try {
        when {
            topDomainOf(Uri.parse(appId).host) == topDomainOf(rpId) -> {
                // Valid: AppId TLD+1 matches RP ID
                true
            }
            topDomainOf(Uri.parse(appId).host) == "gstatic.com" && rpId == "google.com" -> {
                // Valid: Hardcoded support for Google putting their app id under gstatic.com.
                // This is gonna save us a ton of requests
                true
            }
            isFacetIdTrusted(context, facetId, appId) -> {
                // Valid: Allowed by TrustedFacets list
                true
            }
            else -> {
                false
            }
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun RequestOptions.checkIsValid(context: Context, facetId: String, packageName: String?) {
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
    if (facetId.startsWith("https://")) {
        if (topDomainOf(Uri.parse(facetId).host) != topDomainOf(rpId)) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "RP ID $rpId not allowed from facet $facetId")
        }
        // FIXME: Standard suggests doing additional checks, but this is already sensible enough
    } else if (facetId.startsWith("android:apk-key-hash:") && packageName != null) {
        val sha256FacetId = getAltFacetId(context, packageName, facetId)
        if (!isAssetLinked(context, rpId, sha256FacetId, packageName)) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "RP ID $rpId not allowed from facet $sha256FacetId")
        }
    } else if (facetId.startsWith("android:apk-key-hash-sha256:")) {
        if (!isAssetLinked(context, rpId, facetId, packageName)) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "RP ID $rpId not allowed from facet $facetId")
        }
    } else {
        throw RequestHandlingException(NOT_SUPPORTED_ERR, "Facet $facetId not supported")
    }
    val appId = authenticationExtensions?.fidoAppIdExtension?.appId
    if (appId != null) {
        if (!appId.startsWith("https://")) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "AppId $appId must start with https://")
        }
        if (Uri.parse(appId).host.isNullOrEmpty()) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "AppId $appId must have a valid hostname")
        }
        val altFacetId = packageName?.let { getAltFacetId(context, it, facetId) }
        if (!isAppIdAllowed(context, appId, facetId, rpId) &&
            (altFacetId == null || !isAppIdAllowed(context, appId, altFacetId, rpId))
        ) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "AppId $appId not allowed from facet $facetId/$altFacetId")
        }
    }
}

private const val HASH_BASE64_FLAGS = Base64.NO_PADDING + Base64.NO_WRAP + Base64.URL_SAFE

fun RequestOptions.getWebAuthnClientData(callingPackage: String, origin: String): ByteArray {
    val obj = JSONObject()
        .put("type", webAuthnType)
        .put("challenge", challenge.toBase64(HASH_BASE64_FLAGS))
        .put("androidPackageName", callingPackage)
        .put("tokenBinding", tokenBinding?.toJsonObject())
        .put("origin", origin)
    return obj.toString().encodeToByteArray()
}

fun getApplicationName(context: Context, options: RequestOptions, callingPackage: String): String = when (options) {
    is BrowserPublicKeyCredentialCreationOptions, is BrowserPublicKeyCredentialRequestOptions -> options.rpId
    else -> context.packageManager.getApplicationLabel(callingPackage).toString()
}

fun getApkKeyHashFacetId(context: Context, packageName: String): String {
    val digest = context.packageManager.getFirstSignatureDigest(packageName, "SHA1")
        ?: throw RequestHandlingException(NOT_ALLOWED_ERR, "Unknown package $packageName")
    return "android:apk-key-hash:${digest.toBase64(HASH_BASE64_FLAGS)}"
}

fun getAltFacetId(context: Context, packageName: String, facetId: String): String {
    val firstSignature = context.packageManager.getSignatures(packageName).firstOrNull()
        ?: throw RequestHandlingException(NOT_ALLOWED_ERR, "Unknown package $packageName")
    return when (facetId) {
        "android:apk-key-hash:${firstSignature.digest("SHA1").toBase64(HASH_BASE64_FLAGS)}" -> {
            "android:apk-key-hash-sha256:${firstSignature.digest("SHA-256").toBase64(HASH_BASE64_FLAGS)}"
        }
        "android:apk-key-hash-sha256:${firstSignature.digest("SHA-256").toBase64(HASH_BASE64_FLAGS)}" -> {
            "android:apk-key-hash:${firstSignature.digest("SHA1").toBase64(HASH_BASE64_FLAGS)}"
        }
        else -> {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Package $packageName does not match facet $facetId")
        }
    }
}

fun getFacetId(context: Context, options: RequestOptions, callingPackage: String): String = when {
    options is BrowserRequestOptions -> {
        if (options.origin.scheme == null || options.origin.authority == null) {
            throw RequestHandlingException(NOT_ALLOWED_ERR, "Bad url ${options.origin}")
        }
        "${options.origin.scheme}://${options.origin.authority}"
    }
    else -> getApkKeyHashFacetId(context, callingPackage)
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
        clientData = options.getWebAuthnClientData(callingPackage, getFacetId(context, options, callingPackage))
        clientDataHash = clientData.digest("SHA-256")
    } else {
        clientData = "<invalid>".toByteArray()
    }
    return clientData to clientDataHash
}
