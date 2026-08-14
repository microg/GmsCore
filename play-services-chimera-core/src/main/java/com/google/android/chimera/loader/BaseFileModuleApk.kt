/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.loader

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException

abstract class BaseFileModuleApk(
    context: Context,
    moduleVersion: Int,
    moduleType: Int
) : ChimeraModuleApk(context, moduleVersion, moduleType) {
    private var cachedApplicationInfo: ApplicationInfo? = null
    private var resourcesLoader: ResourcesLoader? = null

    private fun getApkPath(): String {
        val path = getArchiveFilePath() ?: throw NameNotFoundException("Could not find APK path for ${toString()}")
        return path
    }

    override fun getApplicationInfo(): ApplicationInfo {
        synchronized(this) {
            if (cachedApplicationInfo == null) {
                cachedApplicationInfo = ApplicationInfo().apply {
                    packageName = appContext.packageName
                    publicSourceDir = getApkPath()
                    sourceDir = getApkPath()
                }
            }
            return cachedApplicationInfo!!
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getResources(): Resources {
        val resources = appContext.packageManager.getResourcesForApplication("android")
        resources.addLoaders(createResourcesLoader(listOf(getApkPath())))
        return resources
    }

    @RequiresApi(Build.VERSION_CODES.R)
    protected fun createResourcesLoader(paths: List<String>): ResourcesLoader {
        synchronized(this) {
            if (resourcesLoader == null) {
                val resourcesLoader = ResourcesLoader()
                paths.forEach { path ->
                    try {
                        val parcelFileDescriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                        resourcesLoader.addProvider(ResourcesProvider.loadFromApk(parcelFileDescriptor))
                    } catch (e: IOException) {
                        throw Exception("error: could not open file:$path ${File(path).exists()}-> ${e.message}", e)
                    }
                }
                this.resourcesLoader = resourcesLoader
            }
            return resourcesLoader!!
        }
    }
}
