/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.loader

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Log
import com.google.android.chimera.component.ContainerApk
import com.google.android.chimera.config.InvalidConfigException
import com.google.android.chimera.config.registry.ApkRegistry
import com.google.android.chimera.config.registry.ApkType
import com.google.android.chimera.container.ModuleApi
import com.google.android.chimera.context.ModuleContext
import com.google.android.chimera.context.ModuleContextRef
import java.util.concurrent.ConcurrentHashMap

object ChimeraApkLoader {
    private const val TAG = "ChimeraApkLoader"
    private val moduleApiCache = HashMap<String, ModuleApi>()
    private val moduleContextCache = ConcurrentHashMap<ApkInfoKey, ModuleContextRef>()

    fun getModuleApi(name: String): ModuleApi? {
        synchronized(moduleApiCache) {
            var moduleApi = moduleApiCache[name]

            if (moduleApi == null) {
                try {
                    moduleApi = Class.forName(name).asSubclass(ModuleApi::class.java).getConstructor().newInstance()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to instantiate chimera ModuleApi class : $name", e)
                }

                if (moduleApi != null) {
                    moduleApiCache[name] = moduleApi
                }
            }
            return moduleApi
        }
    }

    fun loadModule(context: Context, apkInfo: ApkRegistry.ApkInfo): ModuleContextRef {
        val cacheKey = ApkInfoKey(apkInfo)
        moduleContextCache[cacheKey]?.let { return it }

        val dependencyLength = apkInfo.dependCount
        val isDependLoop = dependencyLength == 1 && apkInfo.moduleName == apkInfo.dependModuleNames[0]
        lateinit var moduleContextRef: ModuleContextRef
        if (dependencyLength != 0 && !isDependLoop) {
            val apiClassName = apkInfo.moduleApiClassName
            val moduleApi = getModuleApi(apiClassName)
            require(moduleApi != null) { "failed to get module api: $apiClassName" }

            val parentClassLoader: ClassLoader = context.applicationContext.classLoader
            for (i in dependencyLength - 1 downTo 0) {
                Log.d(TAG, "dependencyApkIndex: ${apkInfo.dependModuleNames[i]}")
            }

            val moduleApk = ChimeraModuleApk.createModuleApk(context, apkInfo)
            require(moduleApk != null) { "failed to create ModuleApk" }

            Log.d(TAG, "skip APK signature verification")

            try {
                moduleApi.onBeforeApkLoad(context, moduleApk)
            } catch (e: Exception) {
                Log.w(TAG, "Setup failed for module ${apkInfo.apkPath}", e)
                throw Exception("onBeforeApkLoad failed.", e)
            }

            val newModuleClassLoader: ClassLoader?
            try {
                newModuleClassLoader = moduleApk.createClassLoader(parentClassLoader)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(TAG, "Config is out of date: $moduleApk has been removed")
                throw InvalidConfigException("can\'t load code from  $moduleApk", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to load code for module $moduleApk", e)
                throw RuntimeException("Failed to create ClassLoader for module $moduleApk", e)
            }
            Log.d(TAG, "loadModule: " + context.packageName)
            val newModuleContext = ModuleContext.createApkApplicationContext(
                context.applicationContext,
                moduleApk,
                getModuleResources(moduleApk),
                newModuleClassLoader,
                emptyMap()
            )

            try {
                moduleApi.onApkLoaded(newModuleContext)
            } catch (e: Exception) {
                Log.w(TAG, "Initialization failed for module apk ${apkInfo.apkPath}", e)
                throw Exception("onApkLoaded failed.", e)
            }

            moduleContextRef = ModuleContextRef(
                apkInfo.moduleName,
                apkInfo.moduleApiClassName,
                moduleApk,
                newModuleContext,
                newModuleClassLoader,
                apkInfo.apkSha256.ifEmpty { null }
            )
        } else {
            require(apkInfo.apkType == ApkType.CONTAINER) {
                "Unexpected apkType for ${apkInfo.packageName}: expected CONTAINER but got ${apkInfo.apkType}"
            }
            val containerApk = ContainerApk(context)
            val moduleAppContext = ModuleContext.createApkApplicationContext(
                context,
                containerApk,
                null,
                context.applicationContext.classLoader,
                emptyMap()
            )

            moduleContextRef = ModuleContextRef(
                apkInfo.moduleName,
                apkInfo.moduleApiClassName,
                containerApk,
                moduleAppContext,
                context.classLoader,
                apkInfo.apkSha256.ifEmpty { null }
            )
        }

        moduleContextCache[cacheKey] = moduleContextRef
        return moduleContextRef
    }

    fun clearModuleCaches(moduleName: String? = null, apkPath: String? = null) {
        if (moduleName.isNullOrEmpty() && apkPath.isNullOrEmpty()) return
        moduleContextCache.entries.removeAll { entry ->
            val info = entry.key.apkInfo
            (!moduleName.isNullOrEmpty() && info.moduleName == moduleName) ||
                    (!apkPath.isNullOrEmpty() && info.apkPath == apkPath)
        }
    }
    fun getModuleResources(chimeraModuleApk: ChimeraModuleApk): Resources? {
        if (chimeraModuleApk is ContainerApk) {
            return null
        }

        try {
            return chimeraModuleApk.getResources()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Config is out of date: $chimeraModuleApk has been removed")
            throw InvalidConfigException("can\'t load resources from $chimeraModuleApk", e)
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to load resources for module $chimeraModuleApk", e)
            throw e
        }
    }
}
