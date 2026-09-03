/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.ComponentName
import android.content.Context
import org.microg.gms.common.Constants

/** The host API requested by a signed Chimera module manifest. */
enum class InitializerMode(val moduleApiClassName: String?) {
    DYNAMITE("com.google.android.gms.chimera.container.DynamiteModuleApi"),
    GMS("com.google.android.gms.chimera.container.GmsModuleApi"),
    UNSUPPORTED(null);

    companion object {
        fun fromRequiredApis(requiredApis: String?): InitializerMode = entries.firstOrNull {
            it.moduleApiClassName == requiredApis
        } ?: UNSUPPORTED
    }
}

/** Immutable capability data read from an APK-local signed Chimera manifest. */
data class ChimeraModuleCapabilities(
    val moduleId: String,
    val moduleVersion: Int,
    val initializerMode: InitializerMode,
    val requiredApis: String?,
    val activityBindings: List<ComponentBinding>,
    val boundServiceBindings: List<ComponentBinding>,
    val providerBindings: List<ComponentBinding>,
    val sliceProviderBindings: List<ComponentBinding>,
)

/** Host-visible support state derived from a verified signed module capability. */
enum class ModuleCapabilityStatus {
    LOADABLE,
    PARTIAL_COMPONENT_SUPPORT,
    UNSUPPORTED_INITIALIZER,
    UNVERIFIED_ARTIFACT,
}

/** The module entry shown in the settings screen; it intentionally excludes APK paths and digests. */
data class InstalledModuleStatus(
    val moduleName: String,
    val moduleVersion: String,
    val capabilityStatus: ModuleCapabilityStatus,
)

/** Checks whether a signed component capability has a host component that Android can dispatch. */
object ChimeraCapabilitySupport {
    fun classify(context: Context, capabilities: List<ChimeraModuleCapabilities>): ModuleCapabilityStatus {
        if (capabilities.isEmpty()) return ModuleCapabilityStatus.UNVERIFIED_ARTIFACT
        if (capabilities.any { it.initializerMode == InitializerMode.UNSUPPORTED }) {
            return ModuleCapabilityStatus.UNSUPPORTED_INITIALIZER
        }
        if (capabilities.any { it.providerBindings.isNotEmpty() || it.sliceProviderBindings.isNotEmpty() }) {
            return ModuleCapabilityStatus.PARTIAL_COMPONENT_SUPPORT
        }
        if (capabilities.any { capability ->
                capability.activityBindings.any { !hasActivity(context, it.containerName) } ||
                    capability.boundServiceBindings.any { !hasService(context, it.moduleChimeraName) }
            }
        ) {
            return ModuleCapabilityStatus.PARTIAL_COMPONENT_SUPPORT
        }
        return ModuleCapabilityStatus.LOADABLE
    }

    private fun hasActivity(context: Context, name: String?): Boolean =
        hasComponent(context, name) { component ->
            context.packageManager.getActivityInfo(component, 0)
        }

    private fun hasService(context: Context, name: String?): Boolean =
        hasComponent(context, name) { component ->
            context.packageManager.getServiceInfo(component, 0)
        }

    private fun hasComponent(
        context: Context,
        rawName: String?,
        resolve: (ComponentName) -> Any,
    ): Boolean {
        val name = rawName?.toHostClassName() ?: return false
        return runCatching { resolve(ComponentName(context, name)) }.isSuccess
    }

    private fun String.toHostClassName(): String = when {
        startsWith('.') -> Constants.GMS_PACKAGE_NAME + this
        startsWith("${Constants.GMS_PACKAGE_NAME}.") -> this
        else -> "${Constants.GMS_PACKAGE_NAME}.$this"
    }
}
