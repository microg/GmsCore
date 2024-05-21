package com.android.vending.licensing

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import com.android.vending.LicenseResult
import com.android.volley.VolleyError
import org.microg.vending.billing.core.HttpClient
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "FakeLicenseChecker"

/* Possible response codes for checkLicense v1, from
 * https://developer.android.com/google/play/licensing/licensing-reference#server-response-codes and
 * the LVL library.
 */

/**
 * The application is licensed to the user. The user has purchased the application, or is authorized to
 * download and install the alpha or beta version of the application.
 */
const val LICENSED: Int = 0x0

/**
 * The application is not licensed to the user.
 */
const val NOT_LICENSED: Int = 0x1

/**
 * The application is licensed to the user, but there is an updated application version available that is
 * signed with a different key.
 */
const val LICENSED_OLD_KEY: Int = 0x2

/**
 * Server error — the application (package name) was not recognized by Google Play.
 */
const val ERROR_NOT_MARKET_MANAGED: Int = 0x3

/**
 * Server error — the server could not load the application's key pair for licensing.
 */
const val ERROR_SERVER_FAILURE: Int = 0x4
const val ERROR_OVER_QUOTA: Int = 0x5

/**
 * Local error — the Google Play application was not able to reach the licensing server, possibly because
 * of network availability problems.
 */
const val ERROR_CONTACTING_SERVER: Int = 0x101

/**
 * Local error — the application requested a license check for a package that is not installed on the device.
 */
const val ERROR_INVALID_PACKAGE_NAME: Int = 0x102

/**
 * Local error — the application requested a license check for a package whose UID (package, user ID pair)
 * does not match that of the requesting application.
 */
const val ERROR_NON_MATCHING_UID: Int = 0x103

const val AUTH_TOKEN_SCOPE: String = "oauth2:https://www.googleapis.com/auth/googleplay"

sealed class LicenseRequestParameters
data class V1Parameters(
    val nonce: Long
) : LicenseRequestParameters()
object V2Parameters : LicenseRequestParameters()

sealed class LicenseResponse(
    val result: Int
)
class V1Response(
    result: Int,
    val signedData: String,
    val signature: String
) : LicenseResponse(result)
class V2Response(
    result: Int,
    val jwt: String?
): LicenseResponse(result)
class ErrorResponse(
    result: Int
): LicenseResponse(result)

/**
 * Performs license check including caller UID verification, using a given account, for which
 * an auth token is fetched.
 */
@Throws(RemoteException::class)
suspend fun HttpClient.checkLicense(
    account: Account,
    accountManager: AccountManager,
    androidId: String?,
    packageInfo: PackageInfo,
    packageName: String,
    queryData: LicenseRequestParameters
) : LicenseResponse {

    val auth = try {
        accountManager.getAuthToken(account, AUTH_TOKEN_SCOPE, false)
            .getString(AccountManager.KEY_AUTHTOKEN)
    } catch (e: AuthenticatorException) {
        Log.e(TAG, "Could not fetch auth token for account $account")
        return ErrorResponse(ERROR_CONTACTING_SERVER)
    }

    if (auth == null) {
        return ErrorResponse(ERROR_CONTACTING_SERVER)
    }

    val decodedAndroidId = androidId?.toLong(16) ?: 1

    return try {
        when (queryData) {
            is V1Parameters -> makeLicenseV1Request(
                packageName, auth, packageInfo.versionCode, queryData.nonce, decodedAndroidId
            )
            is V2Parameters -> makeLicenseV2Request(
                packageName, auth, packageInfo.versionCode, decodedAndroidId
            )
        } ?: ErrorResponse(NOT_LICENSED)
    } catch (e: VolleyError) {
        Log.e(TAG, "License request failed with $e")
        ErrorResponse(ERROR_CONTACTING_SERVER)
    } catch (e: IOException) {
        Log.e(TAG, "Encountered a network error during operation ($e)")
        ErrorResponse(ERROR_CONTACTING_SERVER)
    } catch (e: OperationCanceledException) {
        ErrorResponse(ERROR_CONTACTING_SERVER)
    }
}

suspend fun HttpClient.makeLicenseV1Request(
    packageName: String, auth: String, versionCode: Int, nonce: Long, androidId: Long
): V1Response? = get(
    url = "https://play-fe.googleapis.com/fdfe/apps/checkLicense?pkgn=$packageName&vc=$versionCode&nnc=$nonce",
    headers = getLicenseRequestHeaders(auth, androidId),
    adapter = LicenseResult.ADAPTER
).information?.v1?.let {
    if (it.result != null && it.signedData != null && it.signature != null) {
        V1Response(it.result, it.signedData, it.signature)
    } else null
}

suspend fun HttpClient.makeLicenseV2Request(
    packageName: String,
    auth: String,
    versionCode: Int,
    androidId: Long
): V2Response? = get(
    url = "https://play-fe.googleapis.com/fdfe/apps/checkLicenseServerFallback?pkgn=$packageName&vc=$versionCode",
    headers = getLicenseRequestHeaders(auth, androidId),
    adapter = LicenseResult.ADAPTER
).information?.v2?.license?.jwt?.let {
    // Field present ←→ user has license
    V2Response(LICENSED, it)
}


suspend fun AccountManager.getAuthToken(account: Account, authTokenType: String, notifyAuthFailure: Boolean) =
    suspendCoroutine { continuation ->
        getAuthToken(account, authTokenType, notifyAuthFailure, { future: AccountManagerFuture<Bundle> ->
            try {
                val result = future.result
                continuation.resume(result)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, null)
    }