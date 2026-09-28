/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.chimera.context.ModuleContext
import com.google.android.chimera.loader.ChimeraModuleApk
import dalvik.system.DelegateLastClassLoader
import dalvik.system.PathClassLoader

class ContainerApk(context: Context) : ChimeraModuleApk(
    ModuleContext.getModuleContext(context)?.baseContext ?: context,
    1,
    0
) {
    private val packageName: String = appContext.packageName

    override fun getApplicationInfo(): ApplicationInfo {
        return appContext.createPackageContext(packageName, 0).applicationInfo
    }

    override fun createClassLoader(parentClassLoader: ClassLoader): ClassLoader {
        val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
        val nativeLibPaths = mutableListOf<String>()
        if (appInfo.nativeLibraryDir != null) {
            nativeLibPaths.add(appInfo.nativeLibraryDir)
        }

        if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val abis = if (Build.VERSION.SDK_INT >= 23) {
                if (android.os.Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS.toList()
                else Build.SUPPORTED_32_BIT_ABIS.toList()
            } else {
                listOfNotNull(Build.CPU_ABI, Build.CPU_ABI2.takeIf { it.isNotEmpty() })
            }
            abis.forEach { abi -> nativeLibPaths.add("${appInfo.sourceDir}!/lib/$abi") }
        }

        val nativePathString = if (nativeLibPaths.isEmpty()) null else nativeLibPaths.joinToString(";")
        val apkPath = appInfo.sourceDir
        val args = nativePathString

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DelegateLastClassLoader(apkPath, args, parentClassLoader, false)
        } else {
            PathClassLoader(apkPath, args, parentClassLoader)
        }
    }

    override fun getArchiveFilePath(): String? {
        return try {
            appContext.packageManager.getApplicationInfo(packageName, 0).sourceDir
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun toString(): String {
        return "ContainerApk($packageName)"
    }
}
