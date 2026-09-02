/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.hms.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import com.huawei.hms.maps.MapClientIdentify
import com.huawei.hms.maps.utils.MapClientUtil
import org.microg.gms.common.Constants
import java.io.File

class MapContext(private val context: Context) : ContextWrapper(context.createPackageContext(Constants.GMS_PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)) {
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

    override fun getClassLoader(): ClassLoader {
        return MapContext::class.java.classLoader!!
    }

    override fun getPackageName(): String {
        // Use original package name for requests not from HMS MapClientIdentify
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.any { it.className == MapClientUtil::class.java.name || it.className == MapClientIdentify::class.java.name }) return Constants.GMS_PACKAGE_NAME
        return appContext.packageName
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
        context.startActivity(intent)
    }

    @RequiresApi(24)
    override fun createDeviceProtectedStorageContext(): Context {
        return appContext.createDeviceProtectedStorageContext()
    }

    private val appRes: Resources get() = appContext.resources

    internal val mergedResources: Resources by lazy(LazyThreadSafetyMode.NONE) {
        val gmsRes = try { super.getResources() } catch (e: Exception) { return@lazy appRes }
        @SuppressLint("DiscouragedApi")
        @Suppress("DEPRECATION")
        object : Resources(gmsRes.assets, gmsRes.displayMetrics, gmsRes.configuration) {
            override fun getText(id: Int): CharSequence {
                return try {
                    gmsRes.getText(id)
                } catch (e: Exception) {
                    try {
                        appRes.getText(id)
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }

            override fun getText(id: Int, def: CharSequence?): CharSequence {
                return try {
                    gmsRes.getText(id, def)
                } catch (e: Exception) {
                    try {
                        appRes.getText(id, def)
                    } catch (e2: Exception) {
                        def ?: ""
                    }
                }
            }

            override fun getString(id: Int): String {
                return try {
                    gmsRes.getString(id)
                } catch (e: Exception) {
                    try {
                        appRes.getString(id)
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }

            override fun getString(id: Int, vararg formatArgs: Any?): String {
                return try {
                    gmsRes.getString(id, *formatArgs)
                } catch (e: Exception) {
                    try {
                        appRes.getString(id, *formatArgs)
                    } catch (e2: Exception) {
                        ""
                    }
                }
            }
        }
    }

    override fun getResources(): Resources = mergedResources

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val configCtx = super.createConfigurationContext(overrideConfiguration)
        return object : ContextWrapper(configCtx) {
            override fun getResources(): Resources = mergedResources
            override fun createConfigurationContext(configuration: Configuration): Context {
                return this@MapContext.createConfigurationContext(configuration)
            }
        }
    }

    companion object {
        val TAG = "GmsMapContext"
    }
}