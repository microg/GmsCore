/*
 * Copyright (C) 2019 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.mapbox.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import dalvik.system.BaseDexClassLoader
import android.view.LayoutInflater
import java.io.File

class MapContext(private val context: Context) : ContextWrapper(createModuleContext(context)) {
    private var layoutInflater: LayoutInflater? = null
    private val appContext: Context
        get() = context.applicationContext ?: context

    override fun getApplicationContext(): Context {
        return this
    }

    override fun getCacheDir(): File {
        val cacheDir = File(appContext.cacheDir, "com.google.android.gms")
        cacheDir.mkdirs()
        return cacheDir
    }

    override fun getFilesDir(): File {
        val filesDir = File(appContext.filesDir, "com.google.android.gms")
        filesDir.mkdirs()
        return filesDir
    }

    override fun getPackageName(): String {
        return appContext.packageName
    }

    override fun getClassLoader(): ClassLoader {
        return getModuleClassLoader()
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return appContext.getSharedPreferences("com.google.android.gms_$name", mode)
    }

    override fun getSystemService(name: String): Any? {
        if (name == Context.LAYOUT_INFLATER_SERVICE) {
            if (layoutInflater == null) {
                layoutInflater = super.getSystemService(name) as LayoutInflater
                layoutInflater?.cloneInContext(this)?.let { layoutInflater = it }
            }
            if (layoutInflater != null) {
                return layoutInflater
            }
        }
        return context.getSystemService(name)
    }

    override fun startActivity(intent: Intent?) {
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        private var moduleClassLoader: ClassLoader? = null
        private var modulePackageName: String? = null

        @JvmStatic
        fun setModuleEnvironment(classLoader: ClassLoader) {
            moduleClassLoader = classLoader
        }

        private fun getModuleClassLoader(): ClassLoader {
            return moduleClassLoader ?: throw IllegalStateException("Class loader has not been initialized")
        }

        private fun createModuleContext(context: Context): Context {
            return context.createPackageContext(getModulePackageName(context), Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
        }

        private fun getModulePackageName(context: Context): String {
            return modulePackageName ?: resolveModulePackageName(context).also { modulePackageName = it }
        }

        private fun resolveModulePackageName(context: Context): String {
            val dexPaths = getModuleDexPaths()
            val applicationInfos = context.packageManager.getInstalledApplications(0)
            for (applicationInfo in applicationInfos) {
                if (applicationInfo.sourceDir in dexPaths || applicationInfo.publicSourceDir in dexPaths) {
                    return applicationInfo.packageName
                }
                val splitSourceDirs = applicationInfo.splitSourceDirs ?: continue
                if (splitSourceDirs.any { it in dexPaths }) {
                    return applicationInfo.packageName
                }
            }
            throw IllegalStateException("Package name could not be resolved from the backend class loader")
        }

        private fun getModuleDexPaths(): Set<String> {
            val classLoader = getModuleClassLoader()
            val dexPaths = linkedSetOf<String>()
            if (classLoader is BaseDexClassLoader) {
                try {
                    val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList")
                    pathListField.isAccessible = true
                    val pathList = pathListField.get(classLoader)
                    val dexElementsField = pathList.javaClass.getDeclaredField("dexElements")
                    dexElementsField.isAccessible = true
                    val dexElements = dexElementsField.get(pathList) as Array<*>
                    for (dexElement in dexElements) {
                        if (dexElement == null) continue
                        findDexPath(dexElement)?.let { dexPaths.add(it) }
                    }
                } catch (_: Exception) {
                }
            }
            if (dexPaths.isNotEmpty()) {
                return dexPaths
            }
            val dexPathPattern = Regex("zip file \"([^\"]+)\"")
            dexPathPattern.findAll(classLoader.toString()).forEach { dexPaths.add(it.groupValues[1]) }
            if (dexPaths.isNotEmpty()) {
                return dexPaths
            }
            throw IllegalStateException("Dex paths could not be resolved from the backend class loader")
        }

        private fun findDexPath(dexElement: Any): String? {
            for (fieldName in arrayOf("path", "zip", "file")) {
                try {
                    val field = dexElement.javaClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val value = field.get(dexElement) ?: continue
                    return if (value is File) value.path else value.toString()
                } catch (_: NoSuchFieldException) {
                }
            }
            return null
        }

        val TAG = "GmsMapContext"
    }
}
