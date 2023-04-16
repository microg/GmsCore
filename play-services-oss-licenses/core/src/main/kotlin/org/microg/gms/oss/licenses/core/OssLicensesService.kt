/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.oss.licenses.core

import android.content.Context
import com.google.android.gms.common.api.CommonStatusCodes.SUCCESS
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.oss.licenses.IOSSLicenseService
import com.google.android.gms.oss.licenses.License
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "OssLicensesService"

class OssLicensesService : BaseService(TAG, GmsService.OSS_LICENSES) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService?) {
        callback.onPostInitComplete(SUCCESS, OssLicensesServiceImpl(), null)
    }
}

class OssLicensesServiceImpl : IOSSLicenseService.Stub() {

    override fun getListLayoutPackage(packageName: String?): String? {
        // Use fallback resources provided by package itself
        return packageName
    }

    override fun getLicenseLayoutPackage(packageName: String?): String? {
        // Use fallback resources provided by package itself
        return packageName
    }

    override fun getLicenseDetail(libraryName: String?): String? {
        // Use license provided by package itself
        return null
    }

    override fun getLicenseList(list: MutableList<License>?): List<License> {
        // Just sort it
        val newList = arrayListOf<License>()
        newList.addAll(list.orEmpty())
        newList.sortBy { it.name }
        return newList
    }

}
