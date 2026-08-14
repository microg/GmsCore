/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.moduleinstall

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModuleBootstrap
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.FeatureCheckUtils
import com.google.android.chimera.config.FeatureMessage
import com.google.android.chimera.config.FeaturesMessage
import com.google.android.chimera.config.ModuleDownloadRegistry
import com.google.android.chimera.config.ModuleManager
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_ALREADY_AVAILABLE
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_READY_TO_DOWNLOAD
import com.google.android.gms.common.moduleinstall.ModuleAvailabilityResponse.AvailabilityStatus.STATUS_UNKNOWN_MODULE
import com.google.android.gms.common.moduleinstall.ModuleInstallIntentResponse
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusCodes
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallCallbacks
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallService
import com.google.android.gms.common.moduleinstall.internal.IModuleInstallStatusListener
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.ui.MainSettingsActivity
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "GmsModule/Service"
private const val MODULE_ACTION_REQUIRED_MESSAGE =
    "Interactive module download or import is required."
private const val ACTION_REQUEST_FEATURES_WITH_UI = "com.google.android.chimera.container.REQUEST_FEATURES_WITH_UI"
private const val EXTRA_CHIMERA_FEATURE_LIST = "chimera.FEATURE_LIST"
private const val EXTRA_CHIMERA_REQUESTER_PACKAGE = "chimera.REQUESTER_PACKAGE"
private const val EXTRA_OFFICIAL_REQUESTER_PACKAGE = "get_module_install_request_package"
private const val EXTRA_REQUESTED_FEATURE_NAMES = "org.microg.gms.moduleinstall.REQUESTED_FEATURE_NAMES"
private const val EXTRA_REQUESTED_FEATURE_VERSIONS = "org.microg.gms.moduleinstall.REQUESTED_FEATURE_VERSIONS"
private const val EXTRA_REQUESTER_PACKAGE = "org.microg.gms.moduleinstall.REQUESTER_PACKAGE"

class ModuleInstallService : BaseService(TAG, GmsService.MODULE_INSTALL) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val callingPackage = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = ModuleInstallServiceImpl(this, callingPackage, lifecycle).asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply {
            features = arrayOf(Feature("moduleinstall", 7))
        })
    }
}

/**
 * Reports installed feature availability and supplies the catalog-backed permission/download UI when a
 * client requests an install intent. Background install calls never bypass that user interaction.
 */
class ModuleInstallServiceImpl(
    private val context: Context,
    private val callingPackage: String,
    override val lifecycle: Lifecycle
) : IModuleInstallService.Stub(), LifecycleOwner {

    private fun checkAvailability(request: ApiFeatureRequest?): Int {
        // Module imports are performed by the UI process, while ModuleInstall normally runs in the
        // main GMS process. Initialize AppContext in this process before refreshing the persisted
        // manifest; reload() intentionally returns an empty manifest while AppContext is uninitialized.
        ChimeraModuleBootstrap.ensureInitialized(context)
        runCatching { ChimeraConfigManager.reload() }
            .onFailure { Log.w(TAG, "checkAvailability: failed to reload Chimera config", it) }

        val featureCheck = ModuleManager.FeatureCheck()
        for (feature in request?.features ?: emptyList()) {
            val name = feature.name?.takeIf { it.isNotEmpty() } ?: return ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE
            if (feature.version < -1L) {
                Log.w(TAG, "checkAvailability: invalid requested version for $name: ${feature.version}")
                return ModuleManager.FEATURE_CHECK_ERROR
            }
            featureCheck.checkFeatureAtVersion(name, feature.version)
        }
        // Preserve the legacy optimistic response for features that are not in our download
        // catalog. Built-in and fallback Dynamite implementations are not exhaustively described
        // by feature aliases, so rejecting an unknown alias can disable otherwise working APIs.
        // Catalog-backed .mods features still need their real state so clients can request install UI.
        for (descriptor in featureCheck.featureDescriptors) {
            if (!ModuleDownloadRegistry.isKnownDynamicFeature(descriptor.featureName)) {
                Log.d(TAG, "checkAvailability: treating unregistered feature as already available: ${descriptor.featureName}")
                continue
            }
            val result = FeatureCheckUtils.checkFeatureDescriptors(
                listOf(descriptor),
                allowStaticRegistry = false,
                allowDynamicModules = DynamicModuleSettings.isAvailable(context)
            )
            if (result != ModuleManager.FEATURE_CHECK_SUCCESS) return result
        }
        return ModuleManager.FEATURE_CHECK_SUCCESS
    }

    private fun getEffectivePackage(request: ApiFeatureRequest?): String {
        val requestedPackage = request?.callingPackage?.takeUnless { it.isEmpty() }
        if (requestedPackage != null && requestedPackage != callingPackage) {
            Log.w(TAG, "Ignoring spoofed ModuleInstall callingPackage=$requestedPackage from bound package=$callingPackage")
        }
        return callingPackage
    }

    private fun requestedFeatures(request: ApiFeatureRequest?): List<Feature> = request?.features ?: emptyList()

    override fun areModulesAvailable(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        Log.d(TAG, "areModulesAvailable: $request")
        val result = checkAvailability(request)
        val response = when (result) {
            ModuleManager.FEATURE_CHECK_SUCCESS ->
                ModuleAvailabilityResponse(true, STATUS_ALREADY_AVAILABLE)

            ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE ->
                ModuleAvailabilityResponse(false, STATUS_UNKNOWN_MODULE)

            ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED ->
                ModuleAvailabilityResponse(false, STATUS_READY_TO_DOWNLOAD)

            else -> null
        }
        if (response == null) {
            Log.w(TAG, "areModulesAvailable: internal error, result=$result")
            runCatching {
                callbacks?.onModuleAvailabilityResponse(
                    Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error while attempting to perform the availability check"),
                    null
                )
            }
            return
        }
        runCatching { callbacks?.onModuleAvailabilityResponse(Status.SUCCESS, response) }
    }

    override fun installModules(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?, listener: IModuleInstallStatusListener?) {
        Log.d(TAG, "installModules: request=$request, urgent=${request?.urgent}")
        if (request?.urgent != true) {
            deferredInstall(callbacks, request)
            return
        }
        // A background request cannot grant permissions or launch the chooser; missing modules require the
        // separate install-intent flow below.
        when (val result = checkAvailability(request)) {
            ModuleManager.FEATURE_CHECK_SUCCESS -> {
                runCatching {
                    callbacks?.onModuleInstallResponse(Status.SUCCESS, ModuleInstallResponse(0, false))
                }
            }

            ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE,
            ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED -> {
                val sessionId = registerListener(listener)
                if (sessionId != 0) {
                    // Acknowledge with a real session, then deliver a terminal failure so clients waiting on
                    // background progress do not hang while user interaction is still required.
                    runCatching {
                        callbacks?.onModuleInstallResponse(Status.SUCCESS, ModuleInstallResponse(sessionId, true))
                    }
                    notifyInstallFailed(sessionId, listener, result)
                } else {
                    runCatching {
                        callbacks?.onModuleInstallResponse(
                            Status(CommonStatusCodes.API_NOT_CONNECTED, MODULE_ACTION_REQUIRED_MESSAGE),
                            null
                        )
                    }
                }
            }

            else -> {
                runCatching {
                    callbacks?.onModuleInstallResponse(
                        Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error while attempting to start module install"),
                        null
                    )
                }
            }
        }
    }

    private fun deferredInstall(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        if (requestedFeatures(request).isEmpty()) {
            Log.w(TAG, "deferredInstall: no valid features in request=$request")
            runCatching { callbacks?.onStatus(Status(CommonStatusCodes.INTERNAL_ERROR)) }
            return
        }
        val status = when (val result = checkAvailability(request)) {
            ModuleManager.FEATURE_CHECK_SUCCESS -> Status.SUCCESS
            ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE ->
                Status(ModuleInstallStatusCodes.UNKNOWN_MODULE, MODULE_ACTION_REQUIRED_MESSAGE)

            ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED ->
                Status(ModuleInstallStatusCodes.MODULE_NOT_FOUND, MODULE_ACTION_REQUIRED_MESSAGE)

            else -> {
                Log.w(TAG, "deferredInstall: internal error, result=$result")
                Status(CommonStatusCodes.INTERNAL_ERROR)
            }
        }
        runCatching { callbacks?.onStatus(status) }
    }

    override fun getInstallModulesIntent(callbacks: IModuleInstallCallbacks?, request: ApiFeatureRequest?) {
        Log.d(TAG, "getInstallModulesIntent: $request")
        val result = checkAvailability(request)
        val response = when (result) {
            ModuleManager.FEATURE_CHECK_SUCCESS -> ModuleInstallIntentResponse(null)
            ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE,
            ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED -> ModuleInstallIntentResponse(createInstallModulesPendingIntent(request))

            else -> null
        }
        val status = if (response != null) {
            Status.SUCCESS
        } else {
            Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error while attempting to build the install intent")
        }
        runCatching { callbacks?.onModuleInstallIntentResponse(status, response) }
    }

    private fun createInstallModulesPendingIntent(request: ApiFeatureRequest?): PendingIntent {
        val features = requestedFeatures(request).filter { !it.name.isNullOrEmpty() }
        val featureNames = ArrayList(features.map { checkNotNull(it.name) })
        val featureVersions = features.map { it.version }.toLongArray()
        val requesterPackage = getEffectivePackage(request)
        val requestedDynamicFeatures = features.map {
            ModuleDownloadRegistry.RequestedFeature(checkNotNull(it.name), it.version)
        }
        val availabilityRequest = ApiFeatureRequest().apply {
            this.features = features
        }
        val intent = ModuleDownloadRegistry.createModuleDownloadIntentForRequests(
            context,
            requestedDynamicFeatures,
            availabilityRequest,
        ) ?: run {
            Intent(context, MainSettingsActivity::class.java).apply {
                action = ACTION_REQUEST_FEATURES_WITH_UI
                putExtra(MainSettingsActivity.EXTRA_OPEN_DYNAMIC_MODULE_MANAGER, true)
                putExtra(EXTRA_CHIMERA_FEATURE_LIST, encodeFeatureList(features))
                putExtra(EXTRA_CHIMERA_REQUESTER_PACKAGE, requesterPackage)
                putExtra(EXTRA_OFFICIAL_REQUESTER_PACKAGE, requesterPackage)
                putStringArrayListExtra(EXTRA_REQUESTED_FEATURE_NAMES, featureNames)
                putExtra(EXTRA_REQUESTED_FEATURE_VERSIONS, featureVersions)
                putExtra(EXTRA_REQUESTER_PACKAGE, requesterPackage)
            }
        }
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val requestCode = 31 * requesterPackage.hashCode() + requestedDynamicFeatures.hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun encodeFeatureList(features: List<Feature>): ByteArray {
        return FeaturesMessage.Builder()
            .features(features.mapNotNull { feature ->
                val name = feature.name?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                FeatureMessage.Builder()
                    .featureName(name)
                    .featureVersion(feature.version)
                    .build()
            })
            .build()
            .encode()
    }

    override fun releaseModules(callback: IStatusCallback?, request: ApiFeatureRequest?) {
        Log.d(TAG, "releaseModules: $request")
        val features = request?.features
        if (features.isNullOrEmpty() || features.any { it.name.isNullOrEmpty() }) {
            runCatching { callback?.onResult(Status(CommonStatusCodes.INTERNAL_ERROR)) }
            return
        }
        getEffectivePackage(request)
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

    override fun unregisterListener(callback: IStatusCallback?, listener: IModuleInstallStatusListener?) {
        Log.d(TAG, "unregisterListener")
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

    private fun registerListener(listener: IModuleInstallStatusListener?): Int =
        if (listener?.asBinder()?.isBinderAlive == true) nextSessionId.getAndIncrement() else 0

    private fun notifyInstallFailed(sessionId: Int, listener: IModuleInstallStatusListener?, featureCheckResult: Int) {
        val errorCode = when (featureCheckResult) {
            ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE -> ModuleInstallStatusCodes.UNKNOWN_MODULE
            else -> ModuleInstallStatusCodes.MODULE_NOT_FOUND
        }
        runCatching {
            listener?.onModuleInstallStatusUpdate(
                ModuleInstallStatusUpdate(sessionId, INSTALL_STATE_FAILED, null, null, errorCode)
            )
        }.onFailure {
            Log.w(TAG, "notifyInstallFailed: failed to notify listener for sessionId=$sessionId", it)
        }
    }

    companion object {
        private const val INSTALL_STATE_FAILED = 5
        private val nextSessionId = AtomicInteger(1)
    }
}
