package com.android.vending.licensing

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import com.android.vending.V1Container
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import java.io.IOException

/**
 * Performs license check including caller UID verification, using a given account, for which
 * an auth token is fetched.
 *
 * @param D Request parameter data value type
 * @param R Result type
 */
abstract class LicenseChecker<D, R> {
    abstract fun createRequest(
        packageName: String, auth: String, versionCode: Int, data: D,
        then: (Int, R) -> Unit, errorListener: Response.ErrorListener?
    ): LicenseRequest<*>

    @Throws(RemoteException::class)
    fun checkLicense(
        account: Account?, accountManager: AccountManager, androidId: String?,
        packageName: String, callingUid: Int, packageManager: PackageManager,
        queue: RequestQueue, queryData: D,
        onResult: (Int, R?) -> Unit
    ) {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = packageInfo.versionCode

            // Verify caller identity
            if (packageInfo.applicationInfo.uid != callingUid) {
                Log.e(
                    TAG,
                    "an app illegally tried to request licenses for another app (caller: $callingUid)"
                )
                onResult.safeSendResult(ERROR_NON_MATCHING_UID, null)
            } else {
                val onRequestFinished: (Int, R) -> Unit = { integer: Int, r: R ->
                    onResult.safeSendResult(integer, r)
                    }

                val onRequestError = Response.ErrorListener { error: VolleyError ->
                    Log.e(TAG, "license request failed with $error")
                    onResult.safeSendResult(ERROR_CONTACTING_SERVER, null)
                }

                accountManager.getAuthToken(
                    account, AUTH_TOKEN_SCOPE, false,
                    { future: AccountManagerFuture<Bundle> ->
                        try {
                            val auth = future.result.getString(AccountManager.KEY_AUTHTOKEN)
                            if (auth == null) {
                                onResult.safeSendResult(ERROR_CONTACTING_SERVER, null)
                            } else {
                                val request = createRequest(
                                    packageName, auth,
                                    versionCode, queryData, onRequestFinished, onRequestError
                                )

                                if (androidId != null) {
                                    request.ANDROID_ID = androidId.toLong(16)
                                }

                                request.setShouldCache(false)
                                queue.add(request)
                            }
                        } catch (e: AuthenticatorException) {
                            onResult.safeSendResult(ERROR_CONTACTING_SERVER, null)
                        } catch (e: IOException) {
                            onResult.safeSendResult(ERROR_CONTACTING_SERVER, null)
                        } catch (e: OperationCanceledException) {
                            onResult.safeSendResult(ERROR_CONTACTING_SERVER, null)
                        }
                    }, null
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(
                TAG,
                "an app tried to request licenses for package $packageName, which does not exist"
            )
            onResult.safeSendResult(ERROR_INVALID_PACKAGE_NAME, null)
        }
    }

    // Implementations
    class V1 : LicenseChecker<Long, Pair<String?, String?>?>() {
        override fun createRequest(
            packageName: String,
            auth: String,
            versionCode: Int,
            nonce: Long,
            then: (Int, Pair<String?, String?>?) -> Unit,
            errorListener: Response.ErrorListener?
        ): LicenseRequest<V1Container> {
            return LicenseRequest.V1(
                packageName, auth, versionCode, nonce, { response: V1Container? ->
                    if (response != null) {
                        Log.v(TAG,
                            "licenseV1 result was ${response.result} with signed data ${response.signedData}"
                        )

                        if (response.result != null) {
                            then(
                                response.result,
                                (response.signedData to response.signature)
                            )
                        } else {
                            then(LICENSED, response.signedData to response.signature)
                        }
                    }
                }, errorListener
            )
        }
    }

    class V2 : LicenseChecker<Unit, String?>() {
        override fun createRequest(
            packageName: String, auth: String, versionCode: Int, data: Unit,
            then: (Int, String?) -> Unit, errorListener: Response.ErrorListener?
        ): LicenseRequest<String> {
            return LicenseRequest.V2(
                packageName, auth, versionCode, { response: String? ->
                    if (response != null) {
                        then(LICENSED, response)
                    } else {
                        then(NOT_LICENSED, null)
                    }
                }, errorListener
            )
        }
    }

    companion object {
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
    }

    private fun <A, B> ((A, B?) -> Unit).safeSendResult(
        a: A, b: B
    ) {
        try {
            this(a, b)
        } catch (e: Exception) {
            Log.e(TAG, "While sending result $a, $b, remote encountered an exception.")
            e.printStackTrace()
        }
    }
}