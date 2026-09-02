/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.service.ICommonCallbacks
import com.google.android.gms.common.internal.service.ICommonService
import org.microg.gms.BaseService
import org.microg.gms.auth.signin.SignInConfigurationService

private const val TAG = "GmsCommonService"

class GmsCommonService : BaseService(TAG, GmsService.COMMON) {
    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName) ?: throw IllegalArgumentException("Missing package name")
        Log.d(TAG, "handleServiceRequest: start: $packageName")
        callback?.onPostInitComplete(ConnectionResult.SUCCESS, GmsCommonServiceImpl(this, packageName, lifecycle).asBinder(), null)
    }
}

class GmsCommonServiceImpl(val context: Context, val packageName: String, override val lifecycle: Lifecycle) : ICommonService.Stub(), LifecycleOwner {
    override fun clearDefaultAccount(callbacks: ICommonCallbacks?) {
        Log.d(TAG, "clearDefaultAccount: packageName: $packageName")
        lifecycleScope.launchWhenStarted {
            AccountUtils.get(context).removeSelectedAccount(packageName)
            SignInConfigurationService.setAuthInfo(context, packageName, null, null)
            runCatching { callbacks?.onClearDefaultAccountResult(0) }
        }
    }
}