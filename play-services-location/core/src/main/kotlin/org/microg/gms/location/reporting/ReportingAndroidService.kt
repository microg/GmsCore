/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.reporting

import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.location.manager.FEATURES

class ReportingAndroidService : BaseService("GmsLocReportingSvc", GmsService.REPORTING) {
    @Throws(RemoteException::class)
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val canSynchronizeSettings = PackageUtils.callerHasGooglePackagePermission(
            this,
            GooglePackagePermission.REPORTING
        )
        if (canSynchronizeSettings) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    synchronizePendingAccountOptIns(this@ReportingAndroidService)
                }.onFailure {
                    Log.w(TAG, "Failed to synchronize pending account opt-ins", it)
                }
            }
        }
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            ReportingServiceInstance(this, packageName, lifecycle),
            ConnectionInfo().apply { features = FEATURES }
        )
    }
}
