/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.*
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

class ExposureNotificationService : BaseService(TAG, GmsService.NEARBY_EXPOSURE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)

        fun checkPermission(permission: String): String? {
            if (checkCallingPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                callback.onPostInitComplete(FAILED_UNAUTHORIZED, null, null)
                return null
            }
            return permission
        }

        if (request.packageName != packageName) {
            checkPermission("android.permission.BLUETOOTH") ?: return
            checkPermission("android.permission.INTERNET") ?: return
        }

        if (Build.VERSION.SDK_INT < 21) {
            callback.onPostInitComplete(FAILED_NOT_SUPPORTED, null, null)
            return
        }

        Log.d(TAG, "handleServiceRequest: " + request.packageName)
        callback.onPostInitCompleteWithConnectionInfo(SUCCESS, ExposureNotificationServiceImpl(this, lifecycle, request.packageName), ConnectionInfo().apply {
            features = arrayOf(
                    Feature("nearby_exposure_notification", 3),
                    Feature("nearby_exposure_notification_get_version", 1)
            )
        })
    }
}
