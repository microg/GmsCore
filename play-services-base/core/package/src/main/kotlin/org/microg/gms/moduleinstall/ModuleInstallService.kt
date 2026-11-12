/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.moduleinstall

import android.content.Context
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
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_UNKNOWN_MODULE
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusCodes
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener
import com.google.android.gms.dynamite.DynamiteModule
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ModuleInstall"
private const val OCR_FEATURE_VERSION = 1L
private val OCR_MODULE_IDS = mapOf(
    "vision.ocr" to "com.google.android.gms.vision.ocr",
    "mlkit.ocr.chinese" to "com.google.android.gms.mlkit_ocr_chinese",
    "mlkit.ocr.devanagari" to "com.google.android.gms.mlkit_ocr_devanagari",
    "mlkit.ocr.japanese" to "com.google.android.gms.mlkit_ocr_japanese",
    "mlkit.ocr.korean" to "com.google.android.gms.mlkit_ocr_korean",
)

class ModuleInstallService : BaseService(TAG, GmsService.MODULE_INSTALL) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val binder = ModuleInstallServiceImpl(applicationContext).asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply {
            features = arrayOf(Feature("moduleinstall", 7))
        })
    }
}

class ModuleInstallServiceImpl(private val context: Context) : IModuleInstallService.Stub() {
    private fun areRequestedOcrModulesAvailable(request: ApiFeatureRequest?): Boolean =
        request?.features.orEmpty().all { feature ->
            val name = feature?.name ?: return@all true
            val moduleId = OCR_MODULE_IDS[name] ?: return@all true
            feature.version <= OCR_FEATURE_VERSION && DynamiteModule.getLocalVersion(context, moduleId) > 0
        }

    override fun areModulesAvailable(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        val available = areRequestedOcrModulesAvailable(request)
        val availabilityStatus = if (available) STATUS_ALREADY_AVAILABLE else STATUS_UNKNOWN_MODULE
        runCatching {
            callbacks?.onModuleAvailabilityResponse(Status.SUCCESS, ModuleAvailabilityResponse(available, availabilityStatus))
        }
    }

    override fun installModules(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?, listener: IModuleInstallStatusListener?) {
        runCatching {
            if (areRequestedOcrModulesAvailable(request)) {
                callbacks?.onModuleInstallResponse(Status.SUCCESS, ModuleInstallResponse(0, true))
            } else {
                callbacks?.onModuleInstallResponse(Status(ModuleInstallStatusCodes.UNKNOWN_MODULE), null)
            }
        }
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
