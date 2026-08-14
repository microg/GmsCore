/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.chimera.context.ModuleContext
import com.google.android.chimera.loader.ChimeraModuleLdr
import com.google.android.chimera.config.registry.DynamicModuleRegistry
import com.google.android.chimera.config.registry.FeatureConfigRegistry
import com.google.android.gms.common.app.AppContext

class ChimeraModuleManager(
    private val context: Context,
    private val moduleContext: ModuleContext?,
    private val hasModuleId: Boolean,
) : ModuleManager() {
    private var moduleInfo: ModuleInfo? = null
    private var moduleApkInfo: ModuleApkInfo? = null

    companion object {
        private const val TAG = "ChimeraModuleManager"
    }

    private fun toModuleApkInfo(chimeraModule: ChimeraModule?) = ModuleApkInfo(
        chimeraModule?.packageName ?: "com.google.android.gms",
        chimeraModule?.versionName ?: "",
        chimeraModule?.versionCode ?: 0,
        chimeraModule?.sourceType ?: 0,
        0L,
        false
    )

    private fun toDynamicModule(moduleId: String, chimeraModule: ChimeraModule?): DynamicModuleRegistry.DynamicModule {
        return DynamicModuleRegistry.getByModuleId(moduleId)
            ?: DynamicModuleRegistry.DynamicModule(
                moduleName = chimeraModule?.moduleName?.takeIf { it.isNotEmpty() } ?: moduleId,
                moduleIds = listOf(moduleId)
            )
    }

    private fun toModuleInfo(
        dynamicModule: DynamicModuleRegistry.DynamicModule,
        chimeraModule: ChimeraModule?,
        submoduleId: String?,
        moduleVersion: Int = chimeraModule?.moduleVersion?.toIntOrNull() ?: 0,
    ): ChimeraModuleInfoImpl = ChimeraModuleInfoImpl(
        dynamicModule,
        toModuleApkInfo(chimeraModule),
        submoduleId,
        moduleVersion,
    )

    override fun checkFeaturesAreAvailable(featureCheck: FeatureCheck): Int {
        if (featureCheck.featureDescriptors.isEmpty()) {
            Log.d(TAG, "No feature descriptors provided, returning FEATURE_CHECK_SUCCESS")
            return FEATURE_CHECK_SUCCESS
        }

        return try {
            Log.d(
                TAG, "Checking ${featureCheck.featureDescriptors.size} feature(s): " +
                        featureCheck.featureDescriptors.joinToString { it.featureName ?: "\"\"" })
            FeatureCheckUtils.checkFeatureDescriptors(
                featureCheck.featureDescriptors,
                allowStaticRegistry = true,
                allowDynamicModules = DynamicModuleSettings.isAvailable(context)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve available features: $e", e)
            FEATURE_CHECK_ERROR
        }
    }

    @Deprecated("This method is deprecated.")
    override fun checkFeaturesAreAvailable(featureList: FeatureList): Int {
        val protoBytes = featureList.getProtoBytes()
        if (protoBytes == null || protoBytes.isEmpty()) {
            return FEATURE_CHECK_SUCCESS
        }

        try {
            return FeatureCheckUtils.checkFeatureListProto(
                protoBytes,
                allowDynamicModules = DynamicModuleSettings.isAvailable(context)
            )
        } catch (e: InvalidConfigException) {
            Log.d(TAG, "Unable to retrieve available features: $e")
            return FEATURE_CHECK_ERROR
        }
    }

    override fun fetchFeatures(features: Array<String>): FeatureList? {
        if (features.isEmpty()) {
            Log.e(TAG, "Feature check call didn't receive any featureNames")
            return null
        }
        val dynamicModulesEnabled = DynamicModuleSettings.isAvailable(context)
        val descriptors = features.mapNotNull { name ->
            if (!dynamicModulesEnabled && ModuleDownloadRegistry.isKnownDynamicFeature(name)) {
                return@mapNotNull null
            }
            val installed = if (dynamicModulesEnabled) {
                ChimeraConfigManager.featureConfigByKey(name)
            } else {
                null
            }
            if (installed != null) {
                FeatureDescriptor.Builder()
                    .featureName(name)
                    .featureVersion(installed.featureVersion ?: 0L)
                    .build()
            } else {
                val registry = FeatureConfigRegistry.featureMap[name]
                if (registry != null) {
                    FeatureDescriptor.Builder()
                        .featureName(name)
                        .featureVersion(registry.featureVersion.toLong())
                        .build()
                } else null
            }
        }
        return if (descriptors.isNotEmpty()) FeatureList.fromDescriptors(descriptors) else null
    }

    override fun getAllModules(): Collection<*> {
        val modules = linkedMapOf<String, Pair<DynamicModuleRegistry.DynamicModule, ChimeraModule?>>()
        for (entry in DynamicModuleRegistry.modules) {
            if (entry.moduleName == "ROOT") continue // skip ROOT container
            val chimeraModule = ChimeraConfigManager.findModule(entry.primaryModuleId, entry.moduleName)
            modules[entry.primaryModuleId] = entry to chimeraModule
        }
        for (chimeraModule in ChimeraConfigManager.getRegisteredModules()) {
            val moduleId = chimeraModule.moduleId?.takeIf { it.isNotEmpty() } ?: continue
            val dynamicModule = toDynamicModule(moduleId, chimeraModule)
            modules.putIfAbsent(dynamicModule.primaryModuleId, dynamicModule to chimeraModule)
        }
        return modules.values.map { (dynamicModule, chimeraModule) ->
            toModuleInfo(dynamicModule, chimeraModule, null)
        }
    }

    override fun getAllModulesWithMetadata(metadataKey: String): Collection<*> {
        return (getAllModules() as Collection<ModuleInfo>).filter { moduleInfo ->
            try {
                moduleInfo.getMetadata().get(metadataKey) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun getApiVersion(apiName: String): Int {
        if (moduleContext == null) {
            Log.d(TAG, "Unable to get current module\'s fulfilled APIs in ModuleManager created with non-module Context")
            return -2
        }
        return moduleContext.getFulfilledApis().getOrDefault(apiName, -1)
    }

    override fun getCurrentConfig(): ConfigInfo? {
        return ConfigInfo(emptyList<Any>(), (getAllModules() as Collection<*>).toList(), 0)
    }

    override fun getCurrentModule(): ModuleInfo? {
        if (!hasModuleId) {
            throw IllegalStateException("Unable to get current module info in ModuleManager created with non-module Context");
        }

        synchronized(this) {
            if (moduleInfo == null) {
                initializeModuleInfo()
            }
            return moduleInfo
        }
    }

    override fun getCurrentModuleApk(): ModuleApkInfo? {
        require(moduleContext != null) { "Unable to get current module APK info in ModuleManager created with non-module Context" }

        synchronized(this) {
            if (moduleApkInfo == null) {
                initializeModuleInfo()
            }
            return moduleApkInfo
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getThirdPartyLicenses(): java.util.Map<*, *> {
        return java.util.HashMap<Any, Any>() as java.util.Map<*, *>
    }

    override fun pauseModuleUpdates(moduleName: String, flags: Int) {
        Log.d(TAG, "pauseModuleUpdates($moduleName, $flags) — not implemented")
    }

    override fun requestFeatures(request: FeatureRequest): Boolean {
        Log.d(TAG, "requestFeatures: $request")
        if (request.getRequestedFeatures().isEmpty()) {
            // Feature release is currently bookkeeping-free, but it is safe and idempotent.
            request.getListener()?.onRequestComplete(FEATURE_REQUEST_RESULT_SUCCESS)
            return true
        }
        val check = FeatureCheck()
        request.getRequestedFeatures().forEach { (feature, version) ->
            check.checkFeatureAtVersion(feature, version)
        }
        val available = checkFeaturesAreAvailable(check) == FEATURE_CHECK_SUCCESS
        request.getListener()?.onRequestComplete(
            if (available) FEATURE_REQUEST_RESULT_SUCCESS else FEATURE_REQUEST_RESULT_FAILURE_NO_RETRY
        )
        if (!available) {
            Log.w(TAG, "requestFeatures cannot complete synchronously; caller must use ModuleInstall UI")
        }
        return available
    }

    override fun resumeModuleUpdates(moduleName: String) {
        Log.d(TAG, "resumeModuleUpdates($moduleName) — not implemented")
    }

    private fun initializeModuleInfo() {
        require(moduleContext != null) { "Illegal state attempting to cache module info." }
        val moduleId = moduleContext.getModuleId() ?: return
        val registeredModule = ChimeraConfigManager.findModuleByModuleId(moduleId)
        val dynamicModule = toDynamicModule(moduleId, registeredModule)
        val chimeraModule = registeredModule
            ?: ChimeraConfigManager.findModule(moduleId, dynamicModule.moduleName)
        moduleApkInfo = toModuleApkInfo(chimeraModule)

        if (hasModuleId) {
            moduleInfo = toModuleInfo(
                dynamicModule,
                chimeraModule,
                moduleContext.getSubmoduleId(),
                moduleContext.getModuleVersion(),
            )
        }
    }

    class ChimeraModuleManagerSupplier : ModuleManagerSupplier {
        companion object {
            private const val TAG = "ModuleMgrSupplier"
        }

        override fun createModuleManager(context: Context): ModuleManager {
            if (!AppContext.isInitialized()) {
                Log.w(TAG, "AppContext not initialized, initializing with application context")
                (context.applicationContext as? android.app.Application)?.let { AppContext.init(it) }
            }
            ChimeraModuleBootstrap.ensureInitialized(context)

            ModuleContext.getModuleContext(context)?.let { existingContext ->
                Log.d(TAG, "ModuleContext already exists moduleId:${existingContext.getModuleId()}")
                val hasModuleId = existingContext.getModuleId() != null
                return ChimeraModuleManager(context, existingContext, hasModuleId)
            }

            val loadedModuleContext: ModuleContext? = null

            return ChimeraModuleManager(context, loadedModuleContext, false)
        }

        override fun createBasicModuleInfo(context: Context): BasicModuleInfo? {
            val moduleContext = ModuleContext.getModuleContext(context) ?: return null
            val moduleId = moduleContext.getModuleId() ?: return null
            return BasicModuleInfo(moduleId, moduleContext.getModuleVersion(), moduleContext.getSubmoduleId())
        }

        override fun createSubmoduleContext(
            context: Context,
            moduleName: String,
            require: Boolean
        ): Context? {
            val moduleContext = ModuleContext.getModuleContext(context)
            if (moduleContext == null) {
                return unavailableSubmodule(moduleName, require)
            }
            val moduleId = moduleContext.getModuleId()
            if (moduleId == null) {
                return unavailableSubmodule(moduleName, require)
            }
            if (!DynamicModuleSettings.isAvailable(context)) {
                return unavailableSubmodule(moduleName, require)
            }

            val parent = ChimeraConfigManager.findModuleByModuleId(moduleId)
                ?: return unavailableSubmodule(moduleName, require)
            val capabilities = ChimeraApkManifestReader.readVerifiedCapabilities(context, parent)
            val targetModuleId = resolveSubmoduleId(moduleName, capabilities)
                ?: return unavailableSubmodule(moduleName, require)
            val targetCapability = capabilities.first { it.moduleId == targetModuleId }
            val target = ChimeraConfigManager.findModuleByModuleId(targetModuleId)
                ?: return unavailableSubmodule(moduleName, require)

            if (target.installedApkPath != parent.installedApkPath || target.apkSha256 != parent.apkSha256) {
                Log.w(TAG, "Submodule $targetModuleId is not owned by the current verified APK")
                return unavailableSubmodule(moduleName, require)
            }

            Log.d(TAG, "createSubmoduleContext($moduleName) -> $targetModuleId for moduleId=$moduleId")
            return ChimeraModuleLdr.loadModule(
                context,
                targetModuleId,
                target.moduleName,
                targetCapability.moduleVersion,
            ) ?: unavailableSubmodule(moduleName, require)
        }

        private fun resolveSubmoduleId(
            requestedName: String,
            capabilities: List<ChimeraModuleCapabilities>,
        ): String? {
            if (capabilities.any { it.moduleId == requestedName }) return requestedName
            val registered = DynamicModuleRegistry.getByModuleId(requestedName)
                ?: DynamicModuleRegistry.modules.firstOrNull { it.moduleName == requestedName }
                ?: return null
            return registered.moduleIds.filter { candidate ->
                capabilities.any { it.moduleId == candidate }
            }.singleOrNull()
        }

        private fun unavailableSubmodule(moduleName: String, require: Boolean): Context? {
            if (!require) return null
            Log.w(TAG, "Required submodule context not available: $moduleName")
            throw IllegalStateException("Submodule not available: $moduleName")
        }

    }
}
