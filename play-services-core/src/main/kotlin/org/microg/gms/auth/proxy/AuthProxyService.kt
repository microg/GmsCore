/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.proxy

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.internal.IAuthCallbacks
import com.google.android.gms.auth.api.internal.IAuthService
import com.google.android.gms.auth.api.proxy.ProxyRequest
import com.google.android.gms.auth.api.proxy.ProxyResponse
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.auth.appcert.AppCertManager
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AuthProxyService"

class AuthProxyService : BaseService(TAG, GmsService.AUTH_PROXY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
                ?: throw IllegalArgumentException("Missing package name")
        val consumerPackageName = request.extras.getString("consumerPkg")
        if (consumerPackageName != null) PackageUtils.assertExtendedAccess(this)
        val serviceImpl = AuthServiceImpl(this, lifecycle, consumerPackageName ?: packageName)
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, serviceImpl, Bundle())
    }
}

class AuthServiceImpl(private val context: Context, private val lifecycle: Lifecycle, private val packageName: String) : IAuthService.Stub(), LifecycleOwner {
    override fun performProxyRequest(callbacks: IAuthCallbacks, request: ProxyRequest) {
        Log.d(TAG, "performProxyRequest($packageName, $request)")
        lifecycleScope.launchWhenStarted {
            callbacks.onProxyResponse(ProxyResponse().apply { gmsStatusCode = CommonStatusCodes.CANCELED })
        }
    }

    override fun getSpatulaHeader(callbacks: IAuthCallbacks) {
        Log.d(TAG, "getSpatulaHeader($packageName)")
        lifecycleScope.launchWhenStarted {
            val result = withContext(Dispatchers.IO) { AppCertManager(context).getSpatulaHeader(packageName) }
            Log.d(TAG, "Result: $result")
            callbacks.onSpatulaHeader(result)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
