/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.accounts.AccountManager
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import android.os.Parcel
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.profile.Build

private const val TAG = "RcsService"

// OAuth scope the real GMS RCS module uses when talking to Jibe.
private const val RCS_OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/rcsmessaging"

// Google's Jibe RCS provisioning backend.
private const val JIBE_BASE_URL = "https://jibe-api.googleapis.com"

class RcsService : BaseService(TAG, GmsService.RCS) {

    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RcsServiceImpl(applicationContext).asBinder(),
            ConnectionInfo()
        )
    }
}

/**
 * Handles the three binder calls Google Messages makes after binding:
 *
 *   code 1 — isRcsEnabled: returns whether we are already provisioned.
 *   code 2 — startProvisioning: kicks off registration with Jibe.
 *   code 3 — getProvisioningState: detailed state for the UI status string.
 *
 * Transaction codes were identified by running logcat against the stub and
 * correlating with the sequence Google Messages follows at startup.
 */
class RcsServiceImpl(private val context: Context) : android.os.Binder() {

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        data.enforceInterface("com.google.android.gms.rcs.IRcsService")
        return when (code) {
            1 -> {
                // isRcsEnabled — fast check, no network
                Log.d(TAG, "isRcsEnabled called")
                reply?.writeNoException()
                reply?.writeInt(if (isProvisioned()) 1 else 0)
                true
            }
            2 -> {
                // startProvisioning — async, does not block the caller
                val accountName = data.readString()
                Log.d(TAG, "startProvisioning called for $accountName")
                reply?.writeNoException()
                if (!accountName.isNullOrEmpty()) {
                    Thread { runCatching { provision(accountName) } }.start()
                }
                reply?.writeInt(CommonStatusCodes.SUCCESS)
                true
            }
            3 -> {
                // getProvisioningState — 1=pending, 2=provisioned, 3=failed
                Log.d(TAG, "getProvisioningState called")
                reply?.writeNoException()
                reply?.writeInt(if (isProvisioned()) 2 else 1)
                true
            }
            else -> {
                Log.d(TAG, "onTransact unknown code=$code")
                reply?.writeNoException()
                true
            }
        }
    }

    // -------------------------------------------------------------------------

    private fun isProvisioned(): Boolean =
        context.getSharedPreferences("rcs_state", Context.MODE_PRIVATE)
            .getBoolean("provisioned", false)

    private fun markProvisioned() =
        context.getSharedPreferences("rcs_state", Context.MODE_PRIVATE)
            .edit().putBoolean("provisioned", true).apply()

    /**
     * Registers this device and Google account with Google's Jibe RCS backend.
     *
     * Steps:
     *  1. Get an OAuth token for the rcsmessaging scope.
     *  2. Read SIM operator and device info.
     *  3. POST a JSON provisioning request to Jibe.
     *  4. On HTTP 200, mark the device as provisioned.
     */
    private fun provision(accountName: String) {
        Log.d(TAG, "provision: starting for $accountName")
        runBlocking {
            withContext(Dispatchers.IO) {
                try {
                    val am = AccountManager.get(context)
                    val account = am.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
                        .firstOrNull { it.name == accountName }
                        ?: am.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).firstOrNull()
                        ?: run { Log.w(TAG, "provision: no Google account"); return@withContext }

                    val token = runCatching {
                        am.blockingGetAuthToken(account, RCS_OAUTH_SCOPE, true)
                    }.getOrNull()

                    if (token.isNullOrEmpty()) {
                        Log.w(TAG, "provision: could not get OAuth token for ${account.name}")
                        return@withContext
                    }

                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    val body = buildProvisioningJson(
                        accountName = account.name,
                        simOperator = tm?.simOperator.orEmpty(),
                        networkOperator = tm?.networkOperatorName.orEmpty(),
                        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                        osVersion = Build.VERSION.RELEASE
                    ).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val client = OkHttpClient.Builder()
                        .addInterceptor(BearerInterceptor(token))
                        .build()

                    val response = client.newCall(
                        Request.Builder()
                            .url("$JIBE_BASE_URL/v1/provisioning:start")
                            .post(body)
                            .build()
                    ).execute()

                    val responseBody = response.body?.string()
                    Log.d(TAG, "provision: ${response.code} $responseBody")

                    if (response.isSuccessful) {
                        markProvisioned()
                        Log.d(TAG, "provision: RCS provisioned successfully")
                    } else {
                        Log.w(TAG, "provision: server returned ${response.code}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "provision: error", e)
                }
            }
        }
    }

    private fun buildProvisioningJson(
        accountName: String,
        simOperator: String,
        networkOperator: String,
        deviceModel: String,
        osVersion: String
    ) = """{"account":"$accountName","simOperator":"$simOperator","networkOperator":"$networkOperator","deviceModel":"$deviceModel","osVersion":"$osVersion","platform":"ANDROID"}"""
}

/** Attaches a Bearer token to every outgoing request. */
private class BearerInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
}
