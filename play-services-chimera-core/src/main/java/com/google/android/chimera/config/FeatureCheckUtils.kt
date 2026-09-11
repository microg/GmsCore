/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.util.Log
import com.google.android.chimera.config.registry.FeatureConfigRegistry
import java.io.IOException

object FeatureCheckUtils {
    private const val TAG = "FeatureCheckUtils"

    fun checkFeatureDescriptors(
        descriptors: Iterable<FeatureDescriptor>,
        allowStaticRegistry: Boolean = true,
        allowDynamicModules: Boolean = true,
    ): Int {
        for (fd in descriptors) {
            val featureName = fd.featureName
            val requestedVersion = fd.featureVersion ?: 0L
            Log.d(TAG, "Checking feature: $featureName, requestedVersion: $requestedVersion")

            val result = checkFeature(
                featureName,
                requestedVersion,
                allowStaticRegistry,
                allowDynamicModules
            )
            if (result != ModuleManager.FEATURE_CHECK_SUCCESS) return result
        }

        Log.d(TAG, "All features checked successfully")
        return ModuleManager.FEATURE_CHECK_SUCCESS
    }

    fun checkFeatureListProto(bytes: ByteArray, allowDynamicModules: Boolean = true): Int {
        val features = try {
            FeaturesMessage.ADAPTER.decode(bytes)
        } catch (e: IOException) {
            Log.d(TAG, "Failed to parse FeatureList proto: ${e.message}")
            return ModuleManager.FEATURE_CHECK_ERROR
        }

        return checkFeatureMessages(
            features.features,
            allowStaticRegistry = true,
            allowDynamicModules = allowDynamicModules
        )
    }

    fun checkFeatureMessages(
        messages: Iterable<FeatureMessage>,
        allowStaticRegistry: Boolean = true,
        allowDynamicModules: Boolean = true,
    ): Int {
        for (message in messages) {
            val result = checkFeature(
                message.featureName,
                message.featureVersion ?: 0L,
                allowStaticRegistry,
                allowDynamicModules
            )
            if (result != ModuleManager.FEATURE_CHECK_SUCCESS) return result

            val nested = message.featureDescriptor
            if (nested.isNotEmpty()) {
                val nestedResult = checkFeatureDescriptors(
                    nested,
                    allowStaticRegistry,
                    allowDynamicModules
                )
                if (nestedResult != ModuleManager.FEATURE_CHECK_SUCCESS) return nestedResult
            }
        }
        return ModuleManager.FEATURE_CHECK_SUCCESS
    }

    private fun checkFeature(
        featureName: String?,
        requestedVersion: Long,
        allowStaticRegistry: Boolean,
        allowDynamicModules: Boolean,
    ): Int {
        if (featureName.isNullOrEmpty()) {
            Log.w(TAG, "Unknown feature: $featureName")
            return ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE
        }
        if (requestedVersion < -1L) {
            Log.w(TAG, "Invalid requested version for $featureName: $requestedVersion")
            return ModuleManager.FEATURE_CHECK_ERROR
        }
        // The static catalog only knows built-in aliases. An imported future Chimera module can
        // contribute a feature descriptor without appearing there, but it must still receive the
        // compatible "module required" result while dynamic modules are disabled.
        if (!allowDynamicModules && (
                    ModuleDownloadRegistry.isKnownDynamicFeature(featureName) ||
                            ChimeraConfigManager.featureConfigByKey(featureName) != null
                    )
        ) {
            Log.d(TAG, "Dynamic feature '$featureName' is disabled")
            return ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED
        }

        if (allowDynamicModules) {
            ChimeraConfigManager.featureConfigByKey(featureName)?.let { installed ->
                return evaluateKnownFeature(
                    featureName = featureName,
                    availableVersion = installed.featureVersion,
                    requestedVersion = requestedVersion,
                    source = "installed config"
                )
            }
        }

        val registry = FeatureConfigRegistry.featureMap[featureName]
        if (registry == null) {
            if (ModuleDownloadRegistry.isKnownDynamicFeature(featureName)) {
                Log.d(TAG, "Known dynamic feature '$featureName' is not installed")
                return ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED
            }
            Log.w(TAG, "Unknown feature: $featureName")
            return ModuleManager.FEATURE_CHECK_UNKNOWN_FEATURE
        }

        if (!allowStaticRegistry) {
            Log.d(TAG, "Feature '$featureName' is known but not installed")
            return ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED
        }

        return evaluateKnownFeature(
            featureName = featureName,
            availableVersion = registry.featureVersion.toLong(),
            requestedVersion = requestedVersion,
            source = "static registry"
        )
    }

    private fun evaluateKnownFeature(
        featureName: String,
        availableVersion: Long?,
        requestedVersion: Long,
        source: String
    ): Int {
        if (requestedVersion == 0L) {
            Log.d(TAG, "Feature '$featureName' available from $source at any version")
            return ModuleManager.FEATURE_CHECK_SUCCESS
        }

        if (availableVersion == null || availableVersion < 0L) {
            Log.w(TAG, "Feature '$featureName' has no usable version in $source")
            return ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED
        }

        if (requestedVersion == -1L || availableVersion >= requestedVersion) {
            Log.d(TAG, "Feature '$featureName' available from $source at version $availableVersion")
            return ModuleManager.FEATURE_CHECK_SUCCESS
        }

        Log.d(TAG, "Feature '$featureName' version $availableVersion is below requested $requestedVersion")
        return ModuleManager.FEATURE_CHECK_UPDATE_REQUIRED
    }
}
