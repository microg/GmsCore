/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.Bundle
import com.google.android.chimera.annotation.ChimeraApiVersion
import java.util.Collections
import java.util.Map

@ChimeraApiVersion(added = 0L)
abstract class ModuleManager {

    companion object {
        const val FEATURE_CHECK_SUCCESS = 0
        const val FEATURE_CHECK_UNKNOWN_FEATURE = 1
        const val FEATURE_CHECK_UPDATE_REQUIRED = 2
        const val FEATURE_CHECK_ERROR = 3

        @ChimeraApiVersion(added = 105L)
        const val FEATURE_REQUEST_RESULT_FAILURE = 1

        @ChimeraApiVersion(added = 105L)
        const val FEATURE_REQUEST_RESULT_FAILURE_NO_RETRY = 2

        @ChimeraApiVersion(added = 105L)
        const val FEATURE_REQUEST_RESULT_SUCCESS = 0

        private var supplier: ModuleManagerSupplier = ChimeraModuleManager.ChimeraModuleManagerSupplier()

        @ChimeraApiVersion(added = 0x79L)
        @JvmStatic
        fun createSubmoduleContext(context: Context, moduleName: String): Context? {
            return supplier.createSubmoduleContext(context, moduleName, false)
        }

        @ChimeraApiVersion(added = 0x7BL)
        @JvmStatic
        fun requireSubmoduleContext(context: Context, moduleName: String): Context {
            val ctx = supplier.createSubmoduleContext(context, moduleName, true)
            return ctx ?: throw IllegalStateException()
        }

        @JvmStatic
        fun get(context: Context): ModuleManager {
            return supplier.createModuleManager(context)
        }

        @JvmStatic
        fun getBasicModuleInfo(context: Context): BasicModuleInfo? {
            return supplier.createBasicModuleInfo(context)
        }

        @JvmStatic
        fun setModuleManagerSupplier(moduleManagerSupplier: ModuleManagerSupplier) {
            supplier = moduleManagerSupplier
        }
    }

    @ChimeraApiVersion(added = 104L)
    abstract fun checkFeaturesAreAvailable(featureCheck: FeatureCheck): Int

    @Deprecated("Use checkFeaturesAreAvailable with FeatureCheck")
    abstract fun checkFeaturesAreAvailable(featureList: FeatureList): Int

    abstract fun fetchFeatures(features: Array<String>): FeatureList?

    abstract fun getAllModules(): Collection<*>

    abstract fun getAllModulesWithMetadata(moduleName: String): Collection<*>

    @ChimeraApiVersion(added = 100L)
    abstract fun getApiVersion(moduleName: String): Int

    abstract fun getCurrentConfig(): ConfigInfo?

    abstract fun getCurrentModule(): ModuleInfo?

    abstract fun getCurrentModuleApk(): ModuleApkInfo?

    @Deprecated("Use other license mechanism")
    abstract fun getThirdPartyLicenses(): Map<*, *>

    @ChimeraApiVersion(added = 0x7CL)
    abstract fun pauseModuleUpdates(moduleName: String, flags: Int)

    abstract fun requestFeatures(request: FeatureRequest): Boolean

    @ChimeraApiVersion(added = 0x7CL)
    abstract fun resumeModuleUpdates(moduleName: String)

    interface ModuleManagerSupplier {
        fun createModuleManager(context: Context): ModuleManager
        fun createBasicModuleInfo(context: Context): BasicModuleInfo?
        fun createSubmoduleContext(context: Context, moduleName: String, require: Boolean): Context?
    }

    @ChimeraApiVersion(added = 104L)
    class FeatureCheck {
        private val _featureDescriptors = ArrayList<FeatureDescriptor>()
        val featureDescriptors: List<FeatureDescriptor> get() = _featureDescriptors

        private fun addFeature(feature: String, status: Long) {
            _featureDescriptors.add(FeatureDescriptor(feature, status))
        }

        fun checkFeatureAtAnyVersion(feature: String): FeatureCheck {
            addFeature(feature, 0)
            return this
        }

        fun checkFeatureAtLatestVersion(feature: String): FeatureCheck {
            addFeature(feature, -1)
            return this
        }

        fun checkFeatureAtVersion(feature: String, version: Long): FeatureCheck {
            require(version >= -1)
            addFeature(feature, version)
            return this
        }
    }

    @ChimeraApiVersion(added = 0L)
    class FeatureList private constructor(private val protoBytes: ByteArray?) {

        companion object {
            fun fromDescriptors(featureDataList: List<FeatureDescriptor>) = FeatureList(
                FeaturesMessage.Builder()
                    .features(featureDataList.map { fd ->
                        FeatureMessage.Builder().featureName(fd.featureName).featureVersion(fd.featureVersion).build()
                    })
                    .build()
                    .encode()
            )
        }

        fun getProtoBytes(): ByteArray? = protoBytes
    }

    @ChimeraApiVersion(added = 0L)
    open class BasicModuleInfo(
        @JvmField
        val moduleId: String?,
        @JvmField
        val moduleVersion: Int,
        @JvmField
        @ChimeraApiVersion(added = 123L) val submoduleId: String? = null
    ) {

        @ChimeraApiVersion(added = 123L)
        fun requireSubmoduleId(): String {
            require(submoduleId != null) { "The context used to obtain the module info for the $moduleId module is not associated with a submoduleId." }
            return submoduleId
        }
    }

    @ChimeraApiVersion(added = 0L)
    abstract class ModuleInfo(
        moduleId: String?,
        moduleVersion: Int,
        submoduleId: String? = null,
        @ChimeraApiVersion(added = 138L) val configurationMode: String?,
        val moduleApk: ModuleApkInfo?
    ) : BasicModuleInfo(moduleId, moduleVersion, submoduleId) {
        abstract fun getMetadata(): Bundle

        @ChimeraApiVersion(added = 0L)
        open fun getMetadata(context: Context): Bundle = getMetadata()
    }

    @ChimeraApiVersion(added = 0L)
    class FeatureRequest {
        private val _requestedFeatures = mutableMapOf<String, Long>()
        private val _unrequestedFeatures = mutableSetOf<String>()
        private var _forceUnrequest = false
        private var _urgent = false
        private var _listener: FeatureRequestProgressListener? = null
        private var _sessionId: String? = null
        private var _requesterAppPackage: String? = null

        fun getRequestedFeatures(): kotlin.collections.Map<String, Long> = _requestedFeatures.toMap()

        fun getUnrequestedFeatures(): Set<String> = _unrequestedFeatures.toSet()

        fun getForceUnrequest(): Boolean = _forceUnrequest

        fun getUrgent(): Boolean = _urgent

        fun getListener(): FeatureRequestProgressListener? = _listener

        fun getSessionId(): String? = _sessionId

        fun getRequesterAppPackage(): String? = _requesterAppPackage

        fun requestFeatureAtAnyVersion(feature: String): FeatureRequest {
            _requestedFeatures[feature] = 0L
            return this
        }

        fun requestFeatureAtLatestVersion(feature: String): FeatureRequest {
            _requestedFeatures[feature] = -1L
            return this
        }

        fun requestFeatureAtVersion(feature: String, version: Long): FeatureRequest {
            require(version >= 0 || version == -1L) { "Invalid version: $version" }
            _requestedFeatures[feature] = version
            return this
        }

        fun unrequestFeature(feature: String): FeatureRequest {
            _unrequestedFeatures.add(feature)
            return this
        }

        fun setForceUnrequest(): FeatureRequest {
            _forceUnrequest = true
            return this
        }

        @ChimeraApiVersion(added = 0x6FL)
        fun setRequesterAppPackage(pkg: String): FeatureRequest {
            _requesterAppPackage = pkg
            return this
        }

        @ChimeraApiVersion(added = 106L)
        fun setSessionId(sessionId: String): FeatureRequest {
            _sessionId = sessionId
            return this
        }

        fun setUrgent(): FeatureRequest {
            _urgent = true
            return this
        }

        fun setUrgent(listener: FeatureRequestProgressListener): FeatureRequest {
            _urgent = true
            _listener = listener
            return this
        }

        override fun toString(): String {
            return "FeatureRequest{" +
                    "requestedFeatures=$_requestedFeatures, " +
                    "unrequestedFeatures=$_unrequestedFeatures, " +
                    "forceUnrequest=$_forceUnrequest, " +
                    "isUrgent=$_urgent, " +
                    "listener=$_listener, " +
                    "sessionId=$_sessionId, " +
                    "requesterAppPackage=$_requesterAppPackage" +
                    "}"
        }
    }

    @ChimeraApiVersion(added = 0L)
    abstract class FeatureRequestProgressListener {
        @Deprecated("Use onRequestComplete(Int)")
        open fun onRequestComplete() {
        }

        @ChimeraApiVersion(added = 105L)
        open fun onRequestComplete(result: Int) {
            @Suppress("DEPRECATION")
            onRequestComplete()
        }
    }

    @ChimeraApiVersion(added = 0L)
    class ConfigInfo(
        moduleSets: List<*>,
        optionalModules: List<*>,
        val chimeraConfigModifierFlags: Int
    ) {
        val moduleSets: List<*> = Collections.unmodifiableList(moduleSets)
        val optionalModules: List<*> = Collections.unmodifiableList(optionalModules)
    }

    @ChimeraApiVersion(added = 0L)
    data class ModuleApkInfo(
        val apkPackageName: String,
        val apkVersionName: String,
        val apkVersionCode: Int,
        val apkType: Int,
        val apkTimestamp: Long,
        val apkRequired: Boolean
    )
}
