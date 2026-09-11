/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha.modac

import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CancellationException
import org.microg.gms.recaptcha.qr.InitializationRequest
import org.microg.gms.recaptcha.qr.InitializationResponse
import org.microg.gms.recaptcha.qr.SignalUpdateRequest
import org.microg.gms.recaptcha.qr.SignalUpdateResponse
import org.microg.gms.recaptcha.qr.VerificationRequest
import org.microg.gms.recaptcha.qr.VerificationResponse

private const val INITIALIZATION_ENDPOINT = "https://www.recaptcha.net/recaptcha/api3/mri"
private const val VERIFICATION_ENDPOINT = "https://www.recaptcha.net/recaptcha/api3/mrr"
private const val SIGNAL_UPDATE_ENDPOINT = "https://www.recaptcha.net/recaptcha/api3/mrs"

internal class RecaptchaProtocolClient(private val transport: RecaptchaProtoTransport) {

    suspend fun initialize(request: InitializationRequest, timeoutMs: Long): InitializationResponse {
        val response = postAndDecode(
            stage = "initialization",
            endpoint = INITIALIZATION_ENDPOINT,
            body = request.encode(),
            adapter = InitializationResponse.ADAPTER,
            timeoutMs = timeoutMs,
        )
        if (response.landingToken.isEmpty()) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.INVALID_SERVER_RESPONSE,
                "init_missing_landing_token",
            )
        }
        return response
    }

    suspend fun verify(request: VerificationRequest, timeoutMs: Long): VerificationResponse = postAndDecode(
        stage = "verification",
        endpoint = VERIFICATION_ENDPOINT,
        body = request.encode(),
        adapter = VerificationResponse.ADAPTER,
        timeoutMs = timeoutMs,
    )

    suspend fun updateSignals(request: SignalUpdateRequest, timeoutMs: Long): SignalUpdateResponse {
        val response = postAndDecode(
            stage = "signal_update",
            endpoint = SIGNAL_UPDATE_ENDPOINT,
            body = request.encode(),
            adapter = SignalUpdateResponse.ADAPTER,
            timeoutMs = timeoutMs,
        )
        if (response.credential.isEmpty()) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.INVALID_SERVER_RESPONSE,
                "signal_update_missing_credential",
            )
        }
        return response
    }

    private suspend fun <T> postAndDecode(
        stage: String,
        endpoint: String,
        body: ByteArray,
        adapter: ProtoAdapter<T>,
        timeoutMs: Long,
    ): T {
        val response = postProto(stage, endpoint, body, timeoutMs)
        return try {
            adapter.decode(response)
        } catch (e: Exception) {
            throw RecaptchaError(
                RecaptchaErrorKind.INTERNAL,
                RecaptchaErrorSubKind.RUNTIME_ERROR,
                "${stage}_decode_failed",
                cause = e,
            )
        }
    }

    private suspend fun postProto(
        stage: String,
        endpoint: String,
        body: ByteArray,
        timeoutMs: Long,
    ): ByteArray {
        val response = try {
            transport.post(endpoint, body, timeoutMs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RecaptchaTransportException) {
            throw e.statusCode?.let { httpStatusError(stage, it, e) }
                ?: RecaptchaError(
                    RecaptchaErrorKind.NETWORK,
                    RecaptchaErrorSubKind.UNKNOWN,
                    "${stage}_transport_failed",
                    cause = e,
                )
        } catch (e: Exception) {
            throw RecaptchaError(
                RecaptchaErrorKind.NETWORK,
                RecaptchaErrorSubKind.UNKNOWN,
                "${stage}_transport_failed",
                cause = e,
            )
        }
        if (response.statusCode != 200) {
            throw httpStatusError(stage, response.statusCode)
        }
        return response.body
    }

    private fun httpStatusError(
        stage: String,
        statusCode: Int,
        cause: Throwable? = null,
    ): RecaptchaError {
        val (kindCode, subKindCode) = when (statusCode) {
            400 -> RecaptchaErrorKind.NETWORK to RecaptchaErrorSubKind.BAD_REQUEST
            403, 503 -> RecaptchaErrorKind.SERVER to RecaptchaErrorSubKind.SERVICE_UNAVAILABLE
            404 -> RecaptchaErrorKind.NETWORK to RecaptchaErrorSubKind.NOT_FOUND
            else -> RecaptchaErrorKind.NETWORK to RecaptchaErrorSubKind.HTTP_ERROR
        }
        return RecaptchaError(
            kindCode = kindCode,
            subKindCode = subKindCode,
            label = "${stage}_http_status",
            detail = "HTTP $statusCode",
            cause = cause,
        )
    }
}
