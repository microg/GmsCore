/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Bundle
import android.os.ParcelFileDescriptor
import dalvik.system.DexClassLoader
import java.io.File
import java.io.IOException
import java.util.UUID

class HandleProxyFactory(private val context: Context) {
    private fun getTheApkFile(vmKey: String) = File(getCacheDir(vmKey), "the.apk")
    private fun getCacheDir() = context.getDir(CACHE_FOLDER_NAME, Context.MODE_PRIVATE)
    private fun getCacheDir(vmKey: String) = File(getCacheDir(), vmKey)
    private fun getOptDir(vmKey: String) = File(getCacheDir(vmKey), "opt")
    private fun isValidCache(vmKey: String) = getTheApkFile(vmKey).isFile && getOptDir(vmKey).isDirectory

    private fun updateCacheTimestamp(vmKey: String) {
        try {
            val timestampFile = File(getCacheDir(vmKey), "t")
            if (!timestampFile.exists() && !timestampFile.createNewFile()) {
                throw Exception("Failed to touch last-used file for $vmKey.")
            }
            if (!timestampFile.setLastModified(System.currentTimeMillis())) {
                throw Exception("Failed to update last-used timestamp for $vmKey.")
            }
        } catch (e: IOException) {
            throw Exception("Failed to touch last-used file for $vmKey.")
        }
    }

    private fun verifyApkSignature(apk: File): Boolean {
        return true
    }

    private fun copyTheApk(pfd: ParcelFileDescriptor, vmKey: String) {
        if (!isValidCache(vmKey)) {
            val auIs = ParcelFileDescriptor.AutoCloseInputStream(pfd)
            val temp = File(getCacheDir(), "${UUID.randomUUID()}.apk")
            temp.parentFile!!.mkdirs()
            temp.writeBytes(auIs.readBytes())
            auIs.close()
            getOptDir(vmKey).mkdirs()
            temp.renameTo(getTheApkFile(vmKey))
            updateCacheTimestamp(vmKey)
            if (!isValidCache(vmKey)) {
                getCacheDir(vmKey).deleteRecursively()
                throw IllegalStateException("unknown except")
            }
        }
    }

    fun createHandle(vmKey: String, pfd: ParcelFileDescriptor, extras: Bundle): HandleProxy {
        copyTheApk(pfd, vmKey)
        val clazz = loadClass(vmKey)
        return HandleProxy(clazz, context, vmKey, extras)
    }

    private fun loadClass(vmKey: String): Class<*> {
        val clazz = classMap[vmKey]
        if (clazz != null) {
            updateCacheTimestamp(vmKey)
            return clazz
        } else {
            if (!isValidCache(vmKey)) {
                throw RuntimeException("VM key $vmKey not found in cache")
            }
            if (!verifyApkSignature(getTheApkFile(vmKey))) {
                getCacheDir(vmKey).deleteRecursively()
                throw ClassNotFoundException("APK signature verification failed")
            }
            val loader = DexClassLoader(
                getTheApkFile(vmKey).absolutePath, getOptDir(vmKey).absolutePath, null, context.classLoader
            )
            val clazz = loader.loadClass(CLASS_NAME)
            classMap[vmKey] = clazz
            return clazz
        }
    }

    companion object {
        const val CLASS_NAME = "com.google.ccc.abuse.droidguard.DroidGuard"
        const val CACHE_FOLDER_NAME = "cache_dg"
        val classMap = hashMapOf<String, Class<*>>()
    }
}