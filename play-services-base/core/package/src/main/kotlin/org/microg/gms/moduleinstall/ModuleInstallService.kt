/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.moduleinstall

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ModuleInstall"

class ModuleInstallService : BaseService(TAG, GmsService.MODULE_INSTALL) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val binder = ModuleInstallServiceImpl().asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply {
            features = arrayOf(Feature("moduleinstall", 7))
        })
    }
}

class ModuleInstallServiceImpl : IModuleInstallService.Stub() {
    override fun areModulesAvailable(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        Log.d(TAG, "Not yet implemented: areModulesAvailable $request")
        runCatching { callbacks?.onModuleAvailabilityResponse(Status.SUCCESS, ModuleAvailabilityResponse(true, STATUS_ALREADY_AVAILABLE)) }
    }

    override fun installModules(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?, listener: IModuleInstallStatusListener?) {
        Log.d(TAG, "Not yet implemented: installModules $request")
        runCatching { callbacks?.onModuleInstallResponse(Status.CANCELED, ModuleInstallResponse(0, true)) }
    }

    override fun getInstallModulesIntent(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        Log.d(TAG, "Not yet implemented: getInstallModulesIntent $request")
        runCatching { callbacks?.onModuleInstallIntentResponse(Status.CANCELED, ModuleInstallIntentResponse(null)) }
    }

    override fun releaseModules(callback: IStatusCallback?, request: ApiFeatureRequest?) {
        Log.d(TAG, "Not yet implemented: releaseModules $request")
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

    override fun unregisterListener(callback: IStatusCallback?, listener: IModuleInstallStatusListener?) {
        Log.d(TAG, "Not yet implemented: unregisterListener")
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

}