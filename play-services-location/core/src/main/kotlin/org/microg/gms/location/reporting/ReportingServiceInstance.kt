/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.reporting

import android.accounts.Account
import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.location.reporting.*
import com.google.android.gms.location.reporting.internal.IReportingService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

//import com.google.android.gms.location.places.PlaceReport;
class ReportingServiceInstance(private val context: Context, private val packageName: String) : IReportingService.Stub() {

    override fun getReportingState(account: Account): ReportingState {
        Log.d(TAG, "getReportingState")
        val state = ReportingState()
        if (PackageUtils.callerHasGooglePackagePermission(context, GooglePackagePermission.REPORTING)) {
            state.deviceTag = 0
        }
        return state
    }

    override fun tryOptInAccount(account: Account): Int {
        val request = OptInRequest()
        request.account = account
        return tryOptIn(request)
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
        return 0
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
