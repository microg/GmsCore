/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.content.Context
import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request.Method.GET
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.utils.toHexString
import java.io.UnsupportedEncodingException
import java.lang.RuntimeException
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "GmsFirebaseAuthClient"

class IdentityToolkitClient(context: Context, private val apiKey: String, private val packageName: String? = null, private val certSha1Hash: ByteArray? = null) {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    private fun buildRelyingPartyUrl(method: String) = "https://www.googleapis.com/identitytoolkit/v3/relyingparty/$method?key=$apiKey"
    private fun buildStsUrl(method: String) = "https://securetoken.googleapis.com/v1/$method?key=$apiKey"

    private fun getRequestHeaders(): Map<String, String> = hashMapOf<String, String>().apply {
        if (packageName != null) put("X-Android-Package", packageName)
        if (certSha1Hash != null) put("X-Android-Cert", certSha1Hash.toHexString().uppercase())
    }

    private suspend fun request(method: String, data: JSONObject): JSONObject = suspendCoroutine { continuation ->
        queue.add(object : JsonObjectRequest(POST, buildRelyingPartyUrl(method), data, {
            continuation.resume(it)
        }, {
            Log.d(TAG, "Error: ${it.networkResponse?.data?.decodeToString() ?: it.message}")
            continuation.resumeWithException(RuntimeException(it))
        }) {
            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }

    suspend fun createAuthUri(identifier: String? = null, tenantId: String? = null, continueUri: String? = "http://localhost"): JSONObject =
            request("createAuthUri", JSONObject()
                    .put("identifier", identifier)
                    .put("tenantId", tenantId)
                    .put("continueUri", continueUri))

    suspend fun getAccountInfo(idToken: String? = null): JSONObject =
            request("getAccountInfo", JSONObject()
                    .put("idToken", idToken))

    suspend fun getProjectConfig(): JSONObject = suspendCoroutine { continuation ->
        queue.add(JsonObjectRequest(GET, buildRelyingPartyUrl("getProjectConfig"), null, { continuation.resume(it) }, { continuation.resumeWithException(RuntimeException(it)) }))
    }

    suspend fun getOobConfirmationCode(requestType: String, email: String? = null, newEmail: String? = null, continueUrl: String? = null, idToken: String? = null, iOSBundleId: String? = null, iOSAppStoreId: String? = null, androidMinimumVersion: String? = null, androidInstallApp: Boolean? = null, androidPackageName: String? = null, canHandleCodeInApp: Boolean? = null): JSONObject =
            request("getOobConfirmationCode", JSONObject()
                    .put("kind", "identitytoolkit#relyingparty")
                    .put("requestType", requestType)
                    .put("email", email)
                    .put("newEmail", newEmail)
                    .put("continueUrl", continueUrl)
                    .put("idToken", idToken)
                    .put("iOSBundleId", iOSBundleId)
                    .put("iOSAppStoreId", iOSAppStoreId)
                    .put("androidMinimumVersion", androidMinimumVersion)
                    .put("androidInstallApp", androidInstallApp)
                    .put("androidPackageName", androidPackageName)
                    .put("canHandleCodeInApp", canHandleCodeInApp))


    suspend fun sendVerificationCode(phoneNumber: String? = null, reCaptchaToken: String? = null): JSONObject =
            request("sendVerificationCode", JSONObject()
                    .put("phoneNumber", phoneNumber)
                    .put("recaptchaToken", reCaptchaToken))

    suspend fun setAccountInfo(idToken: String? = null, localId: String? = null, email: String? = null, password: String? = null, displayName: String? = null, photoUrl: String? = null, deleteAttribute: List<String> = emptyList()): JSONObject =
            request("setAccountInfo", JSONObject()
                    .put("idToken", idToken)
                    .put("localId", localId)
                    .put("email", email)
                    .put("password", password)
                    .put("displayName", displayName)
                    .put("photoUrl", photoUrl)
                    .put("deleteAttribute", JSONArray().apply { deleteAttribute.map { put(it) } }))

    suspend fun signupNewUser(email: String? = null, password: String? = null, tenantId: String? = null): JSONObject =
            request("signupNewUser", JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("tenantId", tenantId))

    suspend fun verifyCustomToken(token: String? = null, returnSecureToken: Boolean = true): JSONObject =
            request("verifyCustomToken", JSONObject()
                    .put("token", token)
                    .put("returnSecureToken", returnSecureToken))

    suspend fun verifyAssertion(requestUri: String? = null, postBody: String? = null, returnSecureToken: Boolean = true, returnIdpCredential: Boolean = true): JSONObject =
            request("verifyAssertion", JSONObject()
                    .put("requestUri", requestUri)
                    .put("postBody", postBody)
                    .put("returnSecureToken", returnSecureToken)
                    .put("returnIdpCredential", returnIdpCredential))

    suspend fun verifyPassword(email: String? = null, password: String? = null, tenantId: String? = null, returnSecureToken: Boolean = true): JSONObject =
            request("verifyPassword", JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("tenantId", tenantId)
                    .put("returnSecureToken", returnSecureToken))

    suspend fun verifyPhoneNumber(phoneNumber: String? = null, sessionInfo: String? = null, code: String? = null, idToken: String? = null, verificationProof: String? = null, temporaryProof: String? = null): JSONObject =
            request("verifyPhoneNumber", JSONObject()
                    .put("verificationProof", verificationProof)
                    .put("code", code)
                    .put("idToken", idToken)
                    .put("temporaryProof", temporaryProof)
                    .put("phoneNumber", phoneNumber)
                    .put("sessionInfo", sessionInfo))

    suspend fun getTokenByRefreshToken(refreshToken: String): JSONObject = suspendCoroutine { continuation ->
        queue.add(object : JsonRequest<JSONObject>(POST, buildStsUrl("token"), "grant_type=refresh_token&refresh_token=$refreshToken", { continuation.resume(it) }, { continuation.resumeWithException(RuntimeException(it)) }) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
                return try {
                    val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET)))
                    Response.success(JSONObject(jsonString), null)
                } catch (e: UnsupportedEncodingException) {
                    Response.error(ParseError(e))
                } catch (je: JSONException) {
                    Response.error(ParseError(je))
                }
            }

            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded"
            }

            override fun getHeaders(): Map<String, String> = getRequestHeaders()
        })
    }
}