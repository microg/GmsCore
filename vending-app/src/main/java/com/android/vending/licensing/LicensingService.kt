/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending.licensing

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.android.vending.VendingPreferences.isLicensingEnabled
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import org.microg.gms.auth.AuthConstants
import org.microg.gms.profile.ProfileManager.ensureInitialized
import java.util.LinkedList
import java.util.Queue

class LicensingService : Service() {
    private lateinit var queue: RequestQueue
    private lateinit var accountManager: AccountManager
    private lateinit var androidId: String

    private val mLicenseService: ILicensingService.Stub = object : ILicensingService.Stub() {

        @Throws(RemoteException::class)
        override fun checkLicense(
            nonce: Long,
            packageName: String,
            listener: ILicenseResultListener
        ) {
            Log.v(TAG, "checkLicense($nonce, $packageName)")
            val callingUid = getCallingUid()

            if (!isLicensingEnabled(applicationContext)) {
                Log.d(TAG, "not checking license, as it is disabled by user")
                return
            }

            val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            val packageManager = packageManager

            if (accounts.isEmpty()) {
                handleNoAccounts(packageName, packageManager)
            } else {
                checkLicense(
                    callingUid, nonce, packageName, packageManager, listener, LinkedList(
                        listOf(*accounts)
                    )
                )
            }
        }

        @Throws(RemoteException::class)
        private fun checkLicense(
            callingUid: Int, nonce: Long, packageName: String, packageManager: PackageManager,
            listener: ILicenseResultListener, remainingAccounts: Queue<Account>
        ) {
            LicenseChecker.V1().checkLicense(
                remainingAccounts.poll(),
                accountManager,
                androidId,
                packageName,
                callingUid,
                packageManager,
                queue,
                nonce
            ) { responseCode: Int, stringTuple: Pair<String?, String?>? ->
                if (responseCode != LicenseChecker.LICENSED && !remainingAccounts.isEmpty()) {
                    checkLicense(
                        callingUid,
                        nonce,
                        packageName,
                        packageManager,
                        listener,
                        remainingAccounts
                    )
                } else {
                    listener.verifyLicense(responseCode, stringTuple?.first, stringTuple?.second)
                }
            }
        }

        @Throws(RemoteException::class)
        override fun checkLicenseV2(
            packageName: String,
            listener: ILicenseV2ResultListener,
            extraParams: Bundle
        ) {
            Log.v(TAG, "checkLicenseV2($packageName, $extraParams)")
            val callingUid = getCallingUid()

            if (!isLicensingEnabled(applicationContext)) {
                Log.d(TAG, "not checking license, as it is disabled by user")
                return
            }

            val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            val packageManager = packageManager

            if (accounts.isEmpty()) {
                handleNoAccounts(packageName, packageManager)
            } else {
                checkLicenseV2(
                    callingUid, packageName, packageManager, listener, extraParams, LinkedList(
                        listOf(*accounts)
                    )
                )
            }
        }

        @Throws(RemoteException::class)
        private fun checkLicenseV2(
            callingUid: Int, packageName: String, packageManager: PackageManager,
            listener: ILicenseV2ResultListener, extraParams: Bundle,
            remainingAccounts: Queue<Account>
        ) {
            LicenseChecker.V2().checkLicense(
                remainingAccounts.poll(),
                accountManager,
                androidId,
                packageName,
                callingUid,
                packageManager,
                queue,
                Unit
            ) { responseCode: Int, data: String? ->
                /*
                 * Suppress failures on V2. V2 is commonly used by free apps whose checker
                 * will not throw users out of the app if it never receives a response.
                 *
                 * This means that users who are signed in to a Google account will not
                 * get a worse experience in these apps than users that are not signed in.
                 */
                if (responseCode == LicenseChecker.LICENSED) {
                    val bundle = Bundle()
                    bundle.putString(KEY_V2_RESULT_JWT, data)

                    listener.verifyLicense(responseCode, bundle)
                } else if (!remainingAccounts.isEmpty()) {
                    checkLicenseV2(
                        callingUid,
                        packageName,
                        packageManager,
                        listener,
                        extraParams,
                        remainingAccounts
                    )
                } else {
                    Log.i(TAG, "Suppressed negative license result for package $packageName")
                }
            }
        }

        private fun handleNoAccounts(packageName: String, packageManager: PackageManager) {
            try {
                Log.e(TAG, "not checking license, as user is not signed in")

                packageManager.getPackageInfo(packageName, 0).let {
                    sendLicenseServiceNotification(
                        packageName,
                        packageManager.getApplicationLabel(it.applicationInfo),
                        it.applicationInfo.uid
                    )
                }

            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "ignored license request, but package name $packageName was not known!")
                // don't send sign in notification
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {

        ensureInitialized(this)

        contentResolver.query(
            CHECKIN_SETTINGS_PROVIDER,
            arrayOf("androidId"),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null) {
                cursor.moveToNext()
                androidId = java.lang.Long.toHexString(cursor.getLong(0))
            }
        }
        queue = Volley.newRequestQueue(this)
        accountManager = AccountManager.get(this)

        return mLicenseService
    }


    companion object {
        private const val TAG = "FakeLicenseService"
        private const val KEY_V2_RESULT_JWT = "LICENSE_DATA"

        private val CHECKIN_SETTINGS_PROVIDER: Uri =
            Uri.parse("content://com.google.android.gms.microg.settings/check-in")
    }
}
