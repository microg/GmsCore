/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.chimera.loader.BaseFileModuleApk
import dalvik.system.DelegateLastClassLoader
import dalvik.system.PathClassLoader
import java.io.File
import java.io.IOException
import java.io.InvalidObjectException
import java.security.MessageDigest
import java.util.zip.ZipFile

class ChimeraFileApk(
    context: Context,
    moduleType: Int,
    private val archiveFilePath: String,
    private val expectedSha256: String? = null
): BaseFileModuleApk(context, 3, moduleType) {

    var apkPath: String? = null
    var className: String? = null

    companion object {
        private const val TAG = "ChimeraFileApk"

        @JvmStatic
        fun buildNativeLibPaths(apkPath: String): List<String> {
            val abis: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (android.os.Process.is64Bit()) {
                    Build.SUPPORTED_64_BIT_ABIS.toList()
                } else {
                    Build.SUPPORTED_32_BIT_ABIS.toList()
                }
            } else {
                listOfNotNull(Build.CPU_ABI, Build.CPU_ABI2.takeIf { it.isNotEmpty() })
            }

            val nativeDir = File(File(apkPath).parentFile, "n")

            return abis.map { abi ->
                if (nativeDir.exists()) "$nativeDir/$abi" else "$apkPath!/lib/$abi"
            }
        }
    }

    fun getFullApkPath(): String? {
        val abis = buildNativeLibPaths(archiveFilePath)
        return if (abis.isNotEmpty()) {
            abis.joinToString(";")
        } else {
            null
        }
    }


    private fun requireLoadableApk() {
        val file = File(archiveFilePath)
        if (!file.isFile || !file.canRead()) {
            throw InvalidObjectException("Module APK is not readable: $archiveFilePath")
        }
        if (!isValidApk(file)) {
            throw InvalidObjectException("Module APK is invalid: $archiveFilePath")
        }
        if (!expectedSha256.isNullOrEmpty()) {
            val actual = sha256Hex(file)
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                throw InvalidObjectException("Module APK digest mismatch: $archiveFilePath")
            }
        }
    }

    private fun isValidApk(file: File): Boolean {
        return try {
            ZipFile(file).use { zip -> zip.getEntry("AndroidManifest.xml") != null }
        } catch (_: Exception) {
            false
        }
    }

    private fun sha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    override fun createClassLoader(parentClassLoader: ClassLoader): ClassLoader {
        requireLoadableApk()
        val args = this.apkPath ?: getFullApkPath()
        var canonicalPath = ""
        try {
            canonicalPath = File(archiveFilePath).canonicalPath
        } catch (e: IOException) {
            Log.w(TAG, "Unable to determine canonical path for apk \'$canonicalPath\'")
        }

        val newClassLoader =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DelegateLastClassLoader(canonicalPath, args, parentClassLoader, false)
        } else {
            PathClassLoader(canonicalPath, args, parentClassLoader)
        }

        if (className != null) {
            try {
                newClassLoader.loadClass(className)
                return newClassLoader
            } catch (e: ClassNotFoundException) {
                Log.w(TAG, "Failed to validate PathClassLoader for $archiveFilePath :$e")
                throw InvalidObjectException("Can\'t load code for ${File(archiveFilePath).name}")
            }
        }
        return newClassLoader
    }

    override fun getArchiveFilePath(): String {
        return archiveFilePath
    }
}
