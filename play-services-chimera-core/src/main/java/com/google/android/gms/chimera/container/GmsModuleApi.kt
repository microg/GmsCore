/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.chimera.container

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.android.chimera.loader.ChimeraModuleApk
import com.google.android.chimera.container.ModuleApi
import com.google.android.gms.common.app.BaseApplicationContext
import com.google.android.gms.common.app.GCoreApplicationContext

@Keep
class GmsModuleApi : ModuleApi() {
    private val TAG = "GmsModuleApi"
    override fun onApkLoaded(context: Context) {
        invokeStaticMethod(
            context.classLoader.loadClass("com.google.android.gms.chimera.GmsModuleInitializer")
                .getMethod(
                    "initializeModuleV0",
                    Context::class.java,
                    BaseApplicationContext::class.java
                ),
            arrayOf(context, GCoreApplicationContext.instance)
        )
    }

    override fun onBeforeApkLoad(context: Context, moduleApk: ChimeraModuleApk) {
    }

    override fun onModuleLoaded(moduleName: String, providerClass: String?, context: Context) {
        val targetClassName = if (providerClass == null) {
            val moduleKey: String = if (moduleName.startsWith("com.google.android.gms.")) {
                moduleName.removePrefix("com.google.android.gms.")
            } else {
                moduleName.ifEmpty { "container" }
            }

            "com.google.android.gms.chimera.modules.${moduleKey.replace("__", ".").replace('_', '.')}.AppContextProvider"
        } else {
            providerClass
        }

        try {
            val clazz = context.classLoader.loadClass(targetClassName)
            val method = clazz.getDeclaredMethod("setApplicationContextV0", Context::class.java)
            method.isAccessible = true
            invokeStaticMethod(method, arrayOf(context))
            Log.d(TAG, "Module context set for $targetClassName")
        } catch (e: NoSuchMethodException) {
            Log.d(TAG, "No setApplicationContextV0 in $targetClassName, skipping")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set module context for $moduleName: $targetClassName", e)
            throw e
        }
    }
}
