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
    Log.d(TAG, "GPNV returned ${response.verified_phone_numbers.size} numbers")

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
        Log.w(TAG, "No exact phone match in GPNV response, using first")
    }
    return numbers.firstOrNull()
}

private fun createIidTokenAuth(
    privateKey: java.security.PrivateKey?,
    iidTokenForSig: String,
): ClientCredentialsProto {
    if (privateKey == null) {
        Log.w(TAG, "GPNV: private key missing, sending without signature")
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
        Log.w(TAG, "GPNV: failed to generate signature: ${e.message}")
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
    val payload = decodeJwtPayloadJson(jwt)
    val phoneSuffix = payload?.optString("phone_number")?.takeLast(4)
    Log.i(TAG, "JWT received (${jwt.length} chars)")
    Log.i("MicroGRcs", "constellation JWT len=${jwt.length} phone=***${phoneSuffix ?: "?"}")
}
