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
import android.view.LayoutInflater
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

    override fun getPackageName(): String {
        return appContext.packageName
    }

    override fun getClassLoader(): ClassLoader {
        return MapContext::class.java.classLoader!!
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

    companion object {
        val TAG = "GmsMapContext"
    }
}
