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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.mapbox.mapboxsdk.LibraryLoader
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils
import java.io.*
import java.util.zip.ZipFile

class MultiArchLoader(private val mapContext: Context, private val appContext: Context) : LibraryLoader() {
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    override fun load(name: String) {
        try {
            val otherAppInfo = mapContext.packageManager.getApplicationInfo(appContext.packageName, 0)
            var primaryCpuAbi = ApplicationInfo::class.java.getField("primaryCpuAbi").get(otherAppInfo) as String?
            if (primaryCpuAbi == "armeabi") {
                primaryCpuAbi = "armeabi-v7a"
            }
            if (primaryCpuAbi != null) {
                val path = "lib/$primaryCpuAbi/lib$name.so"
                val cacheFile = File("${appContext.cacheDir.absolutePath}/.gmscore/$path")
                cacheFile.parentFile?.mkdirs()
                val cacheFileStamp = File("${appContext.cacheDir.absolutePath}/.gmscore/$path.stamp")
                val cacheVersion = kotlin.runCatching { cacheFileStamp.readText() }.getOrNull()
                // TODO: Use better version indicator
                val mapVersion = PackageUtils.versionName(mapContext, Constants.GMS_PACKAGE_NAME)
                val apkFile = File(mapContext.packageCodePath)
                if (!cacheFile.exists() || cacheVersion == null || cacheVersion != mapVersion) {
                    val zipFile = ZipFile(apkFile)
                    val entry = zipFile.getEntry(path)
                    if (entry != null) {
                        copyInputStream(zipFile.getInputStream(entry), FileOutputStream(cacheFile))
                    } else {
                        Log.d(TAG, "Can't load native library: $path does not exist in $apkFile")
                    }
                    cacheFileStamp.writeText(mapVersion.toString())
                }
                Log.d(TAG, "Loading $name from ${cacheFile.getPath()}")
                System.load(cacheFile.absolutePath)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        Log.d(TAG, "Loading native $name")
        System.loadLibrary(name)
    }

    @Throws(IOException::class)
    private fun copyInputStream(inp: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var len: Int = inp.read(buffer)
        while (len >= 0) {
            out.write(buffer, 0, len)
            len = inp.read(buffer)
        }

        inp.close()
        out.close()
    }

    companion object {
        private val TAG = "GmsMultiArchLoader"
    }

}
