/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.loader

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.util.Log
import com.google.android.chimera.component.ChimeraFileApk
import com.google.android.chimera.component.ContainerApk
import com.google.android.chimera.config.registry.ApkRegistry
import com.google.android.chimera.config.registry.ApkType

abstract class ChimeraModuleApk(
    val context: Context,
    val apkType: Int,
    val moduleType: Int
) {
    protected var appContext: Context = context.applicationContext

    abstract fun getApplicationInfo(): ApplicationInfo

    abstract fun createClassLoader(parentClassLoader: ClassLoader): ClassLoader

    abstract fun getArchiveFilePath(): String?

    open fun getResources(): Resources {
        try {
            val resources = appContext.packageManager.getResourcesForApplication(getApplicationInfo())
            if (resources.assets == null) {
                throw IllegalArgumentException("Resources is null")
            }
            return resources
        } catch (e: Exception) {
            throw IllegalArgumentException("Error in getResources()", e)
        }
    }

    companion object {
        private const val TAG = "ChimeraModuleApk"

        @JvmStatic
        fun createModuleApk(context: Context, apkInfo: ApkRegistry.ApkInfo): ChimeraModuleApk? {
            // A module descriptor must carry either both a name and a version, or neither.
            if (apkInfo.moduleName.isEmpty() != apkInfo.moduleVersion.isEmpty()) {
                Log.d(TAG, "Invalid module.yaml info for apk: ${apkInfo.moduleName}")
                return null
            }

            when (apkInfo.apkType) {
                ApkType.CONTAINER -> {
                    if ("com.google.android.gms" != apkInfo.packageName) {
                        Log.w(TAG, "Unable to create ModuleApk from invalid descriptor (CONTAINER has incorrect package name)")
                        return null
                    }
                    return ContainerApk(context)
                }
                ApkType.FILE -> {
                    Log.d(TAG, "Creating ChimeraFileApk: moduleType=${apkInfo.moduleType}, apkPath=${apkInfo.apkPath}")
                    return ChimeraFileApk(context, apkInfo.moduleType, apkInfo.apkPath, apkInfo.apkSha256.ifEmpty { null })
                }
                else -> {
                    Log.d(TAG, "Module APK type not supported")
                    return null
                }
            }
        }
    }

}
