/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.chimera.container

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.android.chimera.loader.ChimeraModuleApk
import com.google.android.chimera.component.ChimeraFileApk
import com.google.android.chimera.container.ModuleApi
import java.lang.reflect.Method

@Keep
class DynamiteModuleApi : ModuleApi() {
    private val TAG = "DynamiteModuleApi"

    override fun onApkLoaded(context: Context) {
        val classLoader = context.classLoader
        var methodV2: Method? = null
        var methodV1: Method? = null

        try {
            val clazz = classLoader.loadClass("com.google.android.gms.chimera.DynamiteModuleInitializer")

            try {
                methodV2 = clazz.getDeclaredMethod("initializeModuleV2", Context::class.java, Boolean::class.javaPrimitiveType)
            } catch (_: NoSuchMethodException) {
                try {
                    methodV1 = clazz.getDeclaredMethod("initializeModuleV1", Context::class.java)
                } catch (_: NoSuchMethodException) {
                }
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to set dynamite application context: ${e}")
            return
        }

        if (methodV2 != null) {
            invokeStaticMethod(methodV2, arrayOf(context, false))
            return
        }

        if (methodV1 != null) {
            invokeStaticMethod(methodV1, arrayOf(context))
        }
    }

    override fun onBeforeApkLoad(context: Context, moduleApk: ChimeraModuleApk) {
        if (moduleApk is ChimeraFileApk) {
            moduleApk.className = "com.google.android.gms.chimera.DynamiteModuleInitializer"
            moduleApk.apkPath = moduleApk.getFullApkPath()
        }
    }
}
