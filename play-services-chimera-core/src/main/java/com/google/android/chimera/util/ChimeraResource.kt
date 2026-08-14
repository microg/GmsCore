/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.util

import android.content.res.Resources
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object ChimeraResource {
    private const val TAG = "ChimeraResource"
    private val resourceCache = ConcurrentHashMap<ClassLoader, ResourceInfo>()

    private data class ResourceInfo(
        val packageName: String,
        val nameToIdCache: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    ) {
        companion object {
            val MISSING = ResourceInfo(
                packageName = "missing",
                nameToIdCache = ConcurrentHashMap()
            )
        }
    }

    private fun getResourceIdForName(
        classLoader: ClassLoader,
        resources: Resources,
        resourceName: String
    ): Int {
        val resourceInfo = resourceCache.getOrPut(classLoader) {
            getResourcePackageName(classLoader, resources)?.let { ResourceInfo(it) } ?: ResourceInfo.MISSING
        }

        if (resourceInfo === ResourceInfo.MISSING) {
            throw Resources.NotFoundException("Failed to get resource package name for module $resourceName")
        }

        resourceInfo.nameToIdCache[resourceName]?.let { return it }

        val resourceId = try {
            resources.getIdentifier(resourceName, null, resourceInfo.packageName)
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Unable to locate resource id for resourceName: $resourceName and resourcePackage: ${resourceInfo.packageName}", e)
            throw e
        }

        return resourceInfo.nameToIdCache.putIfAbsent(resourceName, resourceId) ?: resourceId
    }

    private fun getResourcePackageName(classLoader: ClassLoader, resources: Resources): String? {
        try {
            val clazz = classLoader.loadClass("com.google.android.chimeraresources.R\$id")
            val chimeraField = clazz.getField("chimera")
            val chimeraResourceId = chimeraField.getInt(null)

            return resources.getResourcePackageName(chimeraResourceId)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Chimera resources class not found: ${e.message}")
            return null
        } catch (e: NoSuchFieldException) {
            Log.e(TAG, "The resource chimera could not be found: ${e.message}")
            return null
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Failed to get resource ID for chimera: ${e.message}")
            return null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid access to 'chimera' field: ${e.message}")
            return null
        } catch (e: Resources.NotFoundException) {
            Log.w(TAG, "${e.message}")
            return null
        }
    }

    fun getResourceId(appClassLoader: ClassLoader, moduleResources: Resources, sourceResources: Resources, resId: Int): Int {
        if (resId == 0) {
            return 0
        }

        var resourceName = sourceResources.getResourceName(resId)
        if (resourceName.startsWith("@")) {
            resourceName = resourceName.substring(1)
        }
        val colonIndex = resourceName.indexOf(':')
        if (colonIndex != -1) {
            val packageName = resourceName.substring(0, colonIndex)

            if (packageName == "android") {
                return resId
            }

            val resourceNameOnly = resourceName.substring(colonIndex + 1)
            return getResourceIdForName(appClassLoader, moduleResources, resourceNameOnly)
        }

        return getResourceIdForName(appClassLoader, moduleResources, resourceName)
    }
}
