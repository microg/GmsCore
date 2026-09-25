/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.reporting

import android.accounts.Account
import android.content.Context
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.reporting.OptInRequest
import com.google.android.gms.location.reporting.ReportingState
import com.google.android.gms.location.reporting.SendDataRequest
import com.google.android.gms.location.reporting.UlrPrivateModeRequest
import com.google.android.gms.location.reporting.UploadRequest
import com.google.android.gms.location.reporting.UploadRequestResult
import com.google.android.gms.location.reporting.internal.IReportingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.common.Constants
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

/**
 * https://userlocation.googleapis.com/userlocation.UserLocationReportingService/GetApiSettings
 * Follow-up: Fill ReportingState based on AccountConfig returned by the interface and persistence processing
 */

//import com.google.android.gms.location.places.PlaceReport;
class ReportingServiceInstance(
    private val context: Context,
    private val packageName: String,
    override val lifecycle: Lifecycle
) : IReportingService.Stub(), LifecycleOwner {

    override fun getReportingState(account: Account): ReportingState {
        Log.d(TAG, "getReportingState")
        val canAccessSettings = PackageUtils.callerHasGooglePackagePermission(
                context,
                GooglePackagePermission.REPORTING
        )
        val accountOnDevice = isGoogleAccountOnDevice(context, account)
        val settings = fetchEffectiveAccountLocationSettings(
                context,
                account,
                allowRemoteAccountSettings = canAccessSettings && accountOnDevice,
                refreshRemoteAccountSettings = false
        )
        if (canAccessSettings && accountOnDevice) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    if (synchronizePendingAccountOptIn(context, account)) {
                        val refreshedSettings = fetchEffectiveAccountLocationSettings(context, account)
                        if (refreshedSettings != settings) {
                            notifyReportingSettingsChanged(context, packageName)
                        }
                    }
                }.onFailure {
                    Log.w(TAG, "getReportingState: account settings synchronization failed", it)
                }
            }
        }
        val deviceTag = if (canAccessSettings && accountOnDevice) {
            getReportingDeviceTag(context, account)
        } else {
            null
        }
        val optInResult = settings.expectedOptInResult(
                callerAllowed = canAccessSettings,
                isGmsCaller = packageName == Constants.GMS_PACKAGE_NAME
        )
        return settings.toReportingState(deviceTag, canAccessSettings, optInResult)
    }

    override fun tryOptInAccount(account: Account): Int {
        return tryOptIn(OptInRequest().apply { this.account = account })
    }

    override fun requestUpload(request: UploadRequest): UploadRequestResult {
        Log.d(TAG, "requestUpload")
        return UploadRequestResult()
    }

    override fun cancelUploadRequest(l: Long): Int {
        Log.d(TAG, "cancelUploadRequest")
        return 0
    }

    //    @Override
    //    public int reportDeviceAtPlace(Account account, PlaceReport report) throws RemoteException {
    //        Log.d(TAG, "reportDeviceAtPlace");
    //        return 0;
    //    }

    override fun tryOptIn(request: OptInRequest): Int {
        val tag = request.tag
        if (tag != null && tag.length > 100) return OPT_IN_RESULT_TAG_TOO_LONG
        val account = request.account ?: return OPT_IN_RESULT_MISSING_ACCOUNT
        if (!isGoogleAccountOnDevice(context, account)) return OPT_IN_RESULT_INVALID_ACCOUNT
        val isGmsCaller = packageName == Constants.GMS_PACKAGE_NAME
        val callerAllowed = PackageUtils.callerHasGooglePackagePermission(
                context,
                GooglePackagePermission.REPORTING
        )
        if (!callerAllowed) return OPT_IN_RESULT_CALLER_NOT_ALLOWED
        val expectedResult = fetchEffectiveAccountLocationSettings(
                context,
                account,
                refreshRemoteAccountSettings = false
        ).expectedOptInResult(callerAllowed = callerAllowed, isGmsCaller = isGmsCaller)
        if (expectedResult != OPT_IN_RESULT_SUCCESS) return expectedResult

        val baseSource = if (isGmsCaller) {
            "com.google.android.gms+opt-in"
        } else {
            packageName
        }
        val source = tag?.let { "$baseSource+$it" } ?: baseSource
        val auditToken = request.auditToken
        if (!queueAccountOptIn(context, account, source, auditToken)) {
            return OPT_IN_RESULT_WRITE_FAILED
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (!synchronizePendingAccountOptIn(context, account)) {
                Log.w(TAG, "tryOptIn: account settings synchronization failed")
            }
        }
        return OPT_IN_RESULT_SUCCESS
    }

    override fun sendData(request: SendDataRequest): Int {
        return 0
    }

    override fun requestPrivateMode(request: UlrPrivateModeRequest): Int {
        return 0
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
