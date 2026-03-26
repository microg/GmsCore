package org.microg.gms.constellation.core.verification

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.getSystemService
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.ClientChallengeResponse
import org.microg.gms.constellation.core.proto.OdsaOperation
import org.microg.gms.constellation.core.proto.ServerChallengeResponse
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest
import org.microg.gms.constellation.core.proto.Ts43Challenge
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponse
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseError
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseStatus
import org.microg.gms.constellation.core.verification.ts43.EapAkaService
import org.microg.gms.constellation.core.verification.ts43.builder
import org.microg.gms.constellation.core.verification.ts43.userAgent
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "Ts43Verifier"

fun Ts43Challenge.verify(context: Context, subId: Int): ChallengeResponse {
    val verifier = InternalTs43Verifier(context, subId)
    return verifier.verify(this)
}

private class InternalTs43Verifier(private val context: Context, private val subId: Int) {
    private val httpHistory = mutableListOf<String>()

    private val telephonyManager: TelephonyManager by lazy {
        val tm = requireNotNull(context.getSystemService<TelephonyManager>()) {
            "TelephonyManager unavailable"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && subId >= 0) {
            tm.createForSubscriptionId(subId)
        } else tm
    }

    private val eapAkaService by lazy { EapAkaService(telephonyManager) }

    fun verify(challenge: Ts43Challenge): ChallengeResponse {
        httpHistory.clear()

        return try {
            when {
                challenge.client_challenge != null -> {
                    val op = challenge.client_challenge.operation
                    Log.d(TAG, "TS43: Client ODSA challenge route: ${op?.operation}")
                    if (op == null) {
                        buildFailureResponse(
                            challenge,
                            Ts43ChallengeResponseStatus.Code.TS43_STATUS_CHALLENGE_NOT_SET
                        )
                    } else {
                        val requestPayload = buildOdsaRequestPayload(
                            challenge.entitlement_url,
                            challenge.eap_aka_realm,
                            challenge.service_entitlement_request,
                            challenge.app_id
                        )
                        if (requestPayload == null) {
                            buildFailureResponse(
                                challenge,
                                Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR
                            )
                        } else {
                            val responseBody = performOdsaRequest(
                                challenge.entitlement_url,
                                op,
                                requestPayload,
                                challenge.app_id,
                                Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_GET_PHONE_NUMBER_API
                            )
                            ChallengeResponse(
                                ts43_challenge_response = Ts43ChallengeResponse(
                                    ts43_type = challenge.ts43_type,
                                    client_challenge_response = ClientChallengeResponse(
                                        get_phone_number_response = responseBody
                                    ),
                                    http_history = httpHistory.toList()
                                )
                            )
                        }
                    }
                }

                challenge.server_challenge != null -> {
                    val op = challenge.server_challenge.operation
                    Log.d(TAG, "TS43: Server ODSA challenge route: ${op?.operation}")
                    if (op == null) {
                        buildFailureResponse(
                            challenge,
                            Ts43ChallengeResponseStatus.Code.TS43_STATUS_CHALLENGE_NOT_SET
                        )
                    } else {
                        val requestPayload = buildOdsaRequestPayload(
                            challenge.entitlement_url,
                            challenge.eap_aka_realm,
                            challenge.service_entitlement_request,
                            challenge.app_id
                        )
                        if (requestPayload == null) {
                            buildFailureResponse(
                                challenge,
                                Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR
                            )
                        } else {
                            val responseBody = performOdsaRequest(
                                challenge.entitlement_url,
                                op,
                                requestPayload,
                                challenge.app_id,
                                Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_ACQUIRE_TEMPORARY_TOKEN_API
                            )
                            ChallengeResponse(
                                ts43_challenge_response = Ts43ChallengeResponse(
                                    ts43_type = challenge.ts43_type,
                                    server_challenge_response = ServerChallengeResponse(
                                        acquire_temporary_token_response = responseBody
                                    ),
                                    http_history = httpHistory.toList()
                                )
                            )
                        }
                    }
                }

                else -> buildFailureResponse(
                    challenge,
                    Ts43ChallengeResponseStatus.Code.TS43_STATUS_CHALLENGE_NOT_SET
                )
            }
        } catch (e: Ts43ApiException) {
            Log.w(TAG, "TS43 API failure requestType=${e.requestType} http=${e.httpStatus}", e)
            ChallengeResponse(
                ts43_challenge_response = Ts43ChallengeResponse(
                    ts43_type = challenge.ts43_type,
                    status = Ts43ChallengeResponseStatus(
                        error = Ts43ChallengeResponseError(
                            error_code = e.errorCode,
                            http_status = e.httpStatus,
                            request_type = e.requestType
                        )
                    ),
                    http_history = httpHistory.toList()
                )
            )
        } catch (e: NullPointerException) {
            Log.e(TAG, "TS43 null failure", e)
            buildFailureResponse(
                challenge,
                Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR
            )
        } catch (e: RuntimeException) {
            Log.e(TAG, "TS43 runtime failure", e)
            buildFailureResponse(
                challenge,
                Ts43ChallengeResponseStatus.Code.TS43_STATUS_RUNTIME_ERROR
            )
        }
    }

    private fun performOdsaRequest(
        entitlementUrl: String,
        op: OdsaOperation,
        req: ServiceEntitlementRequest?,
        challengeAppId: String?,
        requestType: Ts43ChallengeResponseError.RequestType
    ): String {
        val builder = op.builder(telephonyManager, req, currentAppIds(challengeAppId))
        val url = builder.buildBaseUrl(entitlementUrl)
        val userAgent = req?.userAgent(context) ?: "PRD-TS43 OS-Android/${Build.VERSION.RELEASE}"
        val accept = req?.accept_content_type?.takeIf { it.isNotEmpty() } ?: "application/json"
        val acceptLanguage = Locale.getDefault().toLanguageTag().ifEmpty { "en-US" }
        httpHistory += "GET $url"

        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("User-Agent", userAgent)
            .header("Accept-Language", acceptLanguage)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            httpHistory += "RESP ${response.code} ${request.url}"
            if (response.isSuccessful) {
                response.body?.string()
                    ?: throw Ts43ApiException(
                        errorCode = errorCode(32),
                        httpStatus = response.code,
                        requestType = requestType
                    )
            } else {
                Log.w(TAG, "ODSA request failed: ${response.code} for ${op.operation}")
                throw Ts43ApiException(
                    errorCode = errorCode(31),
                    httpStatus = response.code,
                    requestType = requestType
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, " Network error in ODSA request", e)
            throw Ts43ApiException(
                errorCode = errorCode(30),
                httpStatus = -1,
                requestType = requestType,
                cause = e
            )
        }
    }

    private fun buildOdsaRequestPayload(
        entitlementUrl: String,
        eapAkaRealm: String?,
        req: ServiceEntitlementRequest?,
        challengeAppId: String?
    ): ServiceEntitlementRequest? {
        if (req == null) return null
        if (req.authentication_token.isNotEmpty() || req.temporary_token.isNotEmpty()) return req

        val mccMnc = telephonyManager.simOperator ?: ""
        val imsi = telephonyManager.subscriberId ?: ""
        if (mccMnc.length < 5 || imsi.isEmpty()) return null

        val eapId = eapAkaService.buildEapId(mccMnc, imsi, eapAkaRealm)
        val builder = req.builder(telephonyManager, eapId, currentAppIds(challengeAppId))

        val initialUrl = builder.buildBaseUrl(entitlementUrl)
        val postUrl = entitlementUrl
        val userAgent = req.userAgent(context)
        val acceptLanguage = Locale.getDefault().toLanguageTag().ifEmpty { "en-US" }

        // 1. Initial Identity Probe (GET)
        var currentRequest = Request.Builder()
            .url(initialUrl)
            .header("Accept", "application/vnd.gsma.eap-relay.v1.0+json")
            .header("User-Agent", userAgent)
            .header("Accept-Language", acceptLanguage)
            .build()
        httpHistory += "GET $initialUrl"

        // 2. EAP Rounds Loop
        for (round in 1..3) {
            val response = try {
                okHttpClient.newCall(currentRequest).execute()
            } catch (e: IOException) {
                Log.e(TAG, "Network error in EAP round $round", e)
                throw Ts43ApiException(
                    errorCode = errorCode(30),
                    httpStatus = -1,
                    requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API,
                    cause = e
                )
            }

            val body = response.body?.string() ?: return null
            httpHistory += "RESP ${response.code} ${currentRequest.url}"
            if (!response.isSuccessful) {
                Log.w(TAG, "EAP round $round failed with code ${response.code}")
                throw Ts43ApiException(
                    errorCode = errorCode(31),
                    httpStatus = response.code,
                    requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API
                )
            }

            val token = extractAuthToken(body)
            if (token != null) {
                return req.copy(authentication_token = token)
            }

            val eapRelayPacket = extractEapRelayPacket(body)
                ?: throw Ts43ApiException(
                    errorCode = errorCode(32),
                    httpStatus = response.code,
                    requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API
                )

            val akaResponse =
                eapAkaService.performSimAkaAuth(eapRelayPacket, imsi, mccMnc) ?: return null

            val postBody = JSONObject().put("eap-relay-packet", akaResponse).toString()
            currentRequest = Request.Builder()
                .url(postUrl)
                .header(
                    "Accept",
                    "application/vnd.gsma.eap-relay.v1.0+json, text/vnd.wap.connectivity-xml"
                )
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/vnd.gsma.eap-relay.v1.0+json")
                .header("Accept-Language", acceptLanguage)
                .post(
                    postBody.toByteArray().toRequestBody(
                        "application/vnd.gsma.eap-relay.v1.0+json".toMediaType()
                    )
                )
                .build()
            httpHistory += "POST $postUrl"
        }

        return null
    }

    private fun currentAppIds(challengeAppId: String? = null): List<String> {
        return listOfNotNull(
            challengeAppId?.takeIf { it.isNotBlank() }
        ).ifEmpty {
            listOf("ap2014")
        }
    }

    private fun extractEapRelayPacket(body: String): String? {
        return runCatching { JSONObject(body).optString("eap-relay-packet") }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun extractAuthToken(body: String): String? {
        runCatching {
            JSONObject(body).optJSONObject("Token")?.optString("token")
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }

        return runCatching {
            val normalized = body
                .replace("&", "&amp;")
                .replace("&amp;amp;", "&amp;")
                .replace("\r\n", "\n")
            val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(InputSource(StringReader(normalized)))
            val parms = doc.getElementsByTagName("parm")
            for (i in 0 until parms.length) {
                val node = parms.item(i)
                val attrs = node.attributes ?: continue
                val name = attrs.getNamedItem("name")?.nodeValue ?: continue
                if (name == "token") {
                    return@runCatching attrs.getNamedItem("value")?.nodeValue
                }
            }
            null
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun buildFailureResponse(
        challenge: Ts43Challenge,
        statusCode: Ts43ChallengeResponseStatus.Code
    ): ChallengeResponse {
        return ChallengeResponse(
            ts43_challenge_response = Ts43ChallengeResponse(
                ts43_type = challenge.ts43_type,
                status = Ts43ChallengeResponseStatus(status_code = statusCode),
                http_history = httpHistory.toList()
            )
        )
    }
}

private class Ts43ApiException(
    val errorCode: Ts43ChallengeResponseError.Code,
    val httpStatus: Int,
    val requestType: Ts43ChallengeResponseError.RequestType,
    cause: Throwable? = null
) : Exception(cause)

private fun errorCode(rawCode: Int): Ts43ChallengeResponseError.Code =
    Ts43ChallengeResponseError.Code.fromValue(rawCode)
        ?: Ts43ChallengeResponseError.Code.TS43_ERROR_CODE_UNSPECIFIED

private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existing = cookieStore.getOrPut(url.host) { mutableListOf() }
                for (cookie in cookies) {
                    existing.removeAll {
                        it.name == cookie.name &&
                                it.domain == cookie.domain &&
                                it.path == cookie.path
                    }
                    existing += cookie
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host]
                    ?.filter { cookie -> cookie.matches(url) }
                    .orEmpty()
            }
        })
        .build()
}
