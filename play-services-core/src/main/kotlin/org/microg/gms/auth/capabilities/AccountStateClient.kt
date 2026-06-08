/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * HTTP client for the GMS account_state lookup endpoint:
 *   POST https://android.googleapis.com/auth/lookup/account_state?rt=b
 */
package org.microg.gms.auth.capabilities

import android.accounts.Account
import android.content.Context
import android.util.Log
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.capabilities.proto.AccountStateRequest
import org.microg.gms.auth.capabilities.proto.AccountStateRequestHeader
import org.microg.gms.auth.capabilities.proto.AccountStateResponse
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AccountStateClient(private val context: Context) {

    companion object {
        private const val TAG = "AccountStateClient"

        private const val URL_ENDPOINT =
            "https://android.googleapis.com/auth/lookup/account_state?rt=b"

        /**
         * OAuth2 scope used when fetching the bearer token for the REST
         * account_state endpoint. Covers userinfo.email, account capabilities
         * and account service flags.
         */
        private const val ACCOUNT_STATE_SCOPE =
            "oauth2:https://www.googleapis.com/auth/userinfo.email " +
                    "https://www.googleapis.com/auth/account.capabilities " +
                    "https://www.googleapis.com/auth/account.service_flags"

        // Request-flow tag identifying a forced GAIA services sync over the GMS network stack.
        private const val GMSCORE_FLOW = "36"

        private const val TIMEOUT_MS = 5_000
    }

    /**
     * Synchronously fetch the account state. Throws IOException on any network
     * or auth failure (caller is expected to translate that to result code 8).
     */
    @Throws(IOException::class)
    fun sync(account: Account): AccountStateResponse {
        val token = fetchAccessToken(account)
            ?: throw IOException("couldn't fetch accessToken for AANG scope")

        val certSha1 = PackageUtils.firstSignatureDigest(context, Constants.GMS_PACKAGE_NAME)
            ?.lowercase()
            ?: throw IOException("no signature for ${Constants.GMS_PACKAGE_NAME}")

        val request = AccountStateRequest(
            requestHeader = AccountStateRequestHeader(
                packageName = Constants.GMS_PACKAGE_NAME,
                appCertSha1Hex = certSha1,
            )
        )

        val conn = (URL(URL_ENDPOINT).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-protobuf")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("app", Constants.GMS_PACKAGE_NAME)
            setRequestProperty("device", java.lang.Long.toHexString(LastCheckinInfo.read(context).androidId))
            setRequestProperty("gmsversion", Constants.GMS_VERSION_CODE.toString())
            setRequestProperty("gmscoreFlow", GMSCORE_FLOW)
        }

        try {
            conn.outputStream.use { it.write(AccountStateRequest.ADAPTER.encode(request)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                throw IOException("account_state HTTP $code: $err")
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            return AccountStateResponse.ADAPTER.decode(bytes)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Reuse MicroG's AuthManager to obtain an OAuth2 access token under the
     * AANG scope. This will call the same backend as regular app auth; in
     * practice the server's cert-fingerprint check may reject the result
     * unless MicroG is configured with a known-good GMS signature.
     */
    private fun fetchAccessToken(account: Account): String? {
        return try {
            AuthManager(context, account.name, Constants.GMS_PACKAGE_NAME, ACCOUNT_STATE_SCOPE)
                .requestAuth(false)
                .auth
        } catch (e: Exception) {
            Log.w(TAG, "requestAuth failed: ${e.message}")
            null
        }
    }
}
