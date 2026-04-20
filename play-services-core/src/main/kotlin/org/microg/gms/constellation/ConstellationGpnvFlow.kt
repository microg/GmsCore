/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.util.Log
import com.google.android.gms.constellation.VerifyPhoneNumberRequest as AidlVerifyPhoneNumberRequest
import google.internal.communications.phonedeviceverification.v1.ClientCredentialsProto
import google.internal.communications.phonedeviceverification.v1.GetVerifiedPhoneNumbersRequest
import google.internal.communications.phonedeviceverification.v1.IdTokenRequestProto
import google.internal.communications.phonedeviceverification.v1.VerifiedPhoneNumber
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import okio.ByteString
import org.json.JSONObject

private const val TAG = "GmsConstellationClient"

internal data class GpnvRequestContext(
    val sessionId: String,
    val privateKey: java.security.PrivateKey?,
    val readOnlyIidToken: String,
    val idTokenCertificateHash: String,
    val idTokenCallingPackage: String,
    val idTokenNonce: String,
)

internal data class GpnvLookupResult(
    val jwt: String,
    val phoneNumber: String?,
)

internal suspend fun fetchVerifiedPhoneToken(
    rpc: ConstellationRpcClient,
    requestContext: GpnvRequestContext,
    targetPhone: String?,
    marker: String,
): GpnvLookupResult? {
    val response = rpc.getVerifiedPhoneNumbers(buildGpnvRequest(requestContext))
    Log.i(TAG, "GetVerifiedPhoneNumbers response: ${response.verified_phone_numbers.size} numbers")

    val matchingNumber = findMatchingVerifiedNumber(response.verified_phone_numbers, targetPhone)
    val jwt = matchingNumber?.token
    if (matchingNumber != null && !jwt.isNullOrEmpty()) {
        logJwtSummary(marker, jwt, matchingNumber.phone_number)
        return GpnvLookupResult(jwt = jwt, phoneNumber = matchingNumber.phone_number)
    }

    return null
}

internal fun extractRequestedPhoneNumber(
    request: AidlVerifyPhoneNumberRequest?,
    msisdnOverride: String?,
): String? {
    val requestMsisdn = request?.imsiRequests?.firstOrNull()?.msisdn?.takeIf { it.isNotEmpty() }
    val e164PolicyId = request?.policyId?.takeIf { it.startsWith("+") }
    return requestMsisdn ?: msisdnOverride ?: e164PolicyId
}

internal fun findMatchingVerifiedNumber(
    numbers: List<VerifiedPhoneNumber>,
    targetPhone: String?,
): VerifiedPhoneNumber? {
    if (numbers.isEmpty()) return null
    if (!targetPhone.isNullOrEmpty()) {
        val match = numbers.firstOrNull { it.phone_number == targetPhone }
        if (match != null) return match
        Log.w(TAG, "No exact phone match for $targetPhone in ${numbers.size} numbers, using first")
    }
    return numbers.firstOrNull()
}

private fun createIidTokenAuth(
    privateKey: java.security.PrivateKey?,
    iidTokenForSig: String,
): ClientCredentialsProto {
    if (privateKey == null) {
        Log.w(TAG, "GPNV auth: private key missing; sending iid_token without client_signature")
        return ClientCredentialsProto(
            iid_token = iidTokenForSig,
            client_signature = ByteString.EMPTY,
            signature_timestamp = null,
        )
    }

    val nowMillis = System.currentTimeMillis()
    val seconds = nowMillis / 1000
    val nanos = ((nowMillis % 1000) * 1_000_000).toInt()
    val signingString = "$iidTokenForSig:$seconds:$nanos"

    return try {
        val signature = java.security.Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(signingString.toByteArray(Charsets.UTF_8))
        val signatureBytes = signature.sign()
        val ts = Instant.ofEpochSecond(seconds, nanos.toLong())

        ClientCredentialsProto(
            iid_token = iidTokenForSig,
            client_signature = ByteString.of(*signatureBytes),
            signature_timestamp = ts,
        )
    } catch (e: Exception) {
        Log.w(TAG, "GPNV auth: failed to generate client_signature; sending iid_token without signature", e)
        ClientCredentialsProto(
            iid_token = iidTokenForSig,
            client_signature = ByteString.EMPTY,
            signature_timestamp = null,
        )
    }
}

private fun buildGpnvRequest(requestContext: GpnvRequestContext): GetVerifiedPhoneNumbersRequest {
    return GetVerifiedPhoneNumbersRequest(
        session_id = requestContext.sessionId,
        client_credentials = createIidTokenAuth(requestContext.privateKey, requestContext.readOnlyIidToken),
        selection_types = listOf(1),
        id_token_request = IdTokenRequestProto(
            certificate_hash = requestContext.idTokenCertificateHash,
            calling_package = requestContext.idTokenCallingPackage,
            token_nonce = requestContext.idTokenNonce,
        ),
        droidguard_result = "",
    )
}

private fun decodeJwtPayloadJson(jwt: String): JSONObject? {
    val parts = jwt.split('.')
    if (parts.size < 2) return null
    val payloadB64 = parts[1]
    val padLen = (4 - (payloadB64.length % 4)) % 4
    val padded = payloadB64 + "=".repeat(padLen)
    return try {
        val decoded = Base64.getUrlDecoder().decode(padded)
        JSONObject(String(decoded, Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }
}

internal fun jwtSha256HexPrefix(jwt: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(jwt.toByteArray(Charsets.UTF_8))
    val sb = StringBuilder(8)
    for (b in digest.take(4)) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}

internal fun logJwtSummary(marker: String, jwt: String, phoneFromResponse: String?) {
    val sha8 = try {
        jwtSha256HexPrefix(jwt)
    } catch (_: Exception) {
        "????????"
    }

    val payload = decodeJwtPayloadJson(jwt)
    val iss = payload?.optString("iss")
    val expSec = payload?.optLong("exp")?.takeIf { it > 0 } ?: 0L
    val expDate = if (expSec > 0) java.util.Date(expSec * 1000L).toString() else "?"
    val phoneClaim = payload?.optString("phone_number")
    val phoneSuffix = phoneClaim?.takeLast(4)
    val method = payload?.optJSONObject("google")?.optString("phone_number_verification_method")
    val respSuffix = phoneFromResponse?.takeLast(4)

    Log.i(TAG, "$marker: JWT len=${jwt.length} sha256_8=$sha8 iss=${iss ?: "?"} exp=${if (expSec > 0) expSec else "?"} ($expDate) phone_suffix=${phoneSuffix ?: "?"} method=${method ?: "?"} resp_phone_suffix=${respSuffix ?: "?"}")
    Log.i("MicroGRcs", "constellation JWT len=${jwt.length} phone=***${phoneSuffix ?: "?"}")
}
