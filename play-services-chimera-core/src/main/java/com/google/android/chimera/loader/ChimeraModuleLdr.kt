/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.loader

import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.chimera.config.ChimeraApkManifestReader
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModuleCapabilities
import com.google.android.chimera.config.ChimeraStorage
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.InvalidConfigException
import com.google.android.chimera.config.registry.ApkRegistry
import com.google.android.chimera.config.registry.DynamicModuleRegistry
import com.google.android.chimera.context.ModuleContext
import com.google.android.chimera.context.ModuleContextRef
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile
import androidx.core.net.toUri

object ChimeraModuleLdr {
    val loaderLock = Any()
    val loaderModuleContext = java.util.concurrent.ConcurrentHashMap<String, ModuleContextRef>()

    private const val TAG = "ChimeraModuleLdr"

    fun createModuleContext(
        context: Context,
        moduleId: String,
        moduleName: String?,
        moduleVersion: Int,
        moduleContextRef: ModuleContextRef
    ): Context {
        val moduleContext = moduleContextRef.moduleContext
        val resources = ChimeraApkLoader.getModuleResources(moduleContext.getModuleApk())
        val effectiveModuleName = moduleName?.takeIf { it.isNotEmpty() }
        return ModuleContext(context, moduleContext, moduleId, moduleVersion, effectiveModuleName, resources)
    }

    fun getOrCreateModuleContext(
        context: Context,
        moduleId: String,
        moduleVersion: Int,
        apkInfo: ApkRegistry.ApkInfo,
        implClassName: String?
    ): ModuleContextRef {
        Log.d(TAG, "getOrCreateModuleContext")

        val apkModuleContextRef = loadApkModuleContextRef(context, apkInfo)

        loaderModuleContext[moduleId]?.let {
            Log.d(TAG, "getOrCreateModuleContext: module context exist for $moduleId")
            val sameApk = it.chimeraModuleApk.getArchiveFilePath() == apkInfo.apkPath
            val sameDigest = apkInfo.apkSha256.isEmpty() || it.apkSha256 == apkInfo.apkSha256
            if (sameApk && sameDigest) return it
            Log.w(TAG, "getOrCreateModuleContext: discarding stale module context for $moduleId")
            loaderModuleContext.remove(moduleId)
        }

        loaderModuleContext.values.find {
            it.chimeraModuleApk.getArchiveFilePath() == apkInfo.apkPath &&
                    (apkInfo.apkSha256.isEmpty() || it.apkSha256 == apkInfo.apkSha256)
        }?.let { existing ->
            Log.d(TAG, "getOrCreateModuleContext: reusing ClassLoader for $moduleId (same APK as ${existing.moduleName})")
            loaderModuleContext[moduleId] = existing
            return existing
        }

        val moduleContextRef = ModuleContextRef(
            apkModuleContextRef.moduleName,
            apkModuleContextRef.moduleApiClassname,
            apkModuleContextRef.chimeraModuleApk,
            ModuleContext.createModuleApplicationContext(
                apkModuleContextRef.moduleContext,
                moduleId,
                moduleVersion,
                null
            ),
            apkModuleContextRef.classLoader,
            apkModuleContextRef.apkSha256
        )
        Log.d(TAG, "getOrCreateModuleContext: create new module context ${moduleContextRef.moduleContext}")
        loaderModuleContext[moduleId] = moduleContextRef

        val moduleApi = ChimeraApkLoader.getModuleApi(moduleContextRef.moduleApiClassname)
        if (moduleApi == null) {
            Log.w(TAG, "Failed to capture application context. ModuleApi not found for: ${moduleContextRef.moduleName}")
            return moduleContextRef
        }

        if (moduleId.isNotEmpty()) {
            try {
                moduleApi.onModuleLoaded(moduleId, implClassName, moduleContextRef.moduleContext)
            } catch (_: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Config is out of date: ${moduleContextRef.chimeraModuleApk} has been modified")
                throw InvalidConfigException("Module APK has been modified: ${moduleContextRef.chimeraModuleApk}")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to set module context for $moduleId", e)
            }
        }

        return moduleContextRef
    }

    private fun loadApkModuleContextRef(context: Context, apkInfo: ApkRegistry.ApkInfo): ModuleContextRef {
        return synchronized(loaderLock) { ChimeraApkLoader.loadModule(context, apkInfo) }
    }

    fun clearModuleCache(moduleId: String? = null, moduleName: String? = null, apkPath: String? = null) {
        if (moduleId.isNullOrEmpty() && moduleName.isNullOrEmpty() && apkPath.isNullOrEmpty()) return
        loaderModuleContext.entries.removeAll { entry ->
            val ref = entry.value
            (!moduleId.isNullOrEmpty() && entry.key == moduleId) ||
                    (!moduleName.isNullOrEmpty() && ref.moduleName == moduleName) ||
                    (!apkPath.isNullOrEmpty() && ref.chimeraModuleApk.getArchiveFilePath() == apkPath)
        }
        ChimeraApkLoader.clearModuleCaches(moduleName, apkPath)
    }

    fun loadModule(context: Context, moduleId: String, moduleName: String?, moduleVersion: Int): Context? {
        if (!DynamicModuleSettings.isAvailable(context)) {
            Log.d(TAG, "Dynamic modules unavailable; refusing to load $moduleId")
            return null
        }
        val moduleEntry = DynamicModuleRegistry.getByModuleId(moduleId)
        val targetModuleName = moduleEntry?.moduleName ?: moduleName
        if (targetModuleName == null) {
            Log.w(TAG, "Module not found in registry: $moduleId")
            return null
        }

        val apkInfo = resolveApkInfo(context, moduleId, targetModuleName)
            ?: resolveApkInfoViaContentProvider(context, moduleId, targetModuleName)
        if (apkInfo == null) {
            Log.w(TAG, "APK not found for module: $targetModuleName")
            return null
        }
        val effectiveModuleVersion = moduleVersion.takeIf { it > 0 }
            ?: apkInfo.moduleVersion.toIntOrNull()?.takeIf { it > 0 }
            ?: 0

        // implClassName stays null so GmsModuleApi.onModuleLoaded derives the AppContextProvider class
        // name by the standard naming convention (chimera.modules.<moduleKey>.AppContextProvider),
        // matching official GMS which carries no hardcoded moduleId->class table.
        val implClassName: String? = null
        val ref = getOrCreateModuleContext(
            context,
            moduleId,
            effectiveModuleVersion,
            apkInfo,
            implClassName
        )
        return createModuleContext(context, moduleId, moduleName, effectiveModuleVersion, ref)
    }

    private fun resolveApkInfo(context: Context, moduleId: String, moduleName: String): ApkRegistry.ApkInfo? {
        val chimeraModule = ChimeraConfigManager.findModule(moduleId, moduleName)

        if (chimeraModule != null && !chimeraModule.installedApkPath.isNullOrEmpty()) {
            val protoPath = chimeraModule.installedApkPath!!
            val verifiedApk = ChimeraStorage.verifiedModuleApk(
                context = context,
                file = File(protoPath),
                expectedModuleName = null,
                expectedSha256 = chimeraModule.apkSha256,
            )
            if (verifiedApk != null) {
                Log.d(TAG, "Resolved verified APK for module=$moduleId")
                val configuredModuleId = chimeraModule.moduleId?.takeIf { it.isNotEmpty() } ?: moduleId
                val capability = readModuleCapability(verifiedApk, configuredModuleId)
                val apiClass = capability?.initializerMode?.moduleApiClassName
                if (apiClass == null) {
                    Log.w(TAG, "Configured APK has unsupported module API for $configuredModuleId: ${capability?.requiredApis}")
                    return null
                }
                return ApkRegistry.createDynamicApkInfo(
                    verifiedApk.absolutePath,
                    moduleName,
                    chimeraModule.moduleVersion.orEmpty(),
                    apiClass,
                    apkSha256 = chimeraModule.apkSha256.orEmpty()
                )
            }
            Log.w(TAG, "Configured APK is missing or no longer trusted for $moduleId")
        }
        return null
    }

    private fun resolveApkInfoViaContentProvider(context: Context, moduleId: String, moduleName: String): ApkRegistry.ApkInfo? {
        try {
            val appContext = context.applicationContext ?: context
            val resolver = appContext.contentResolver ?: return null
            val metadata = queryProviderMetadata(resolver, moduleId) ?: return null
            val cacheDir = File(appContext.cacheDir, "chimera_modules")
            val safeId = moduleId.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(160)
            val digestToken = metadata.sha256.take(16)
            val cachedApk = File(cacheDir, "$safeId-${metadata.version}-$digestToken.apk")
            if (cachedApk.exists() && cachedApk.length() > 0) {
                if (isValidApk(cachedApk) && matchesExpectedDigest(cachedApk, metadata.sha256)) {
                    val capability = readModuleCapability(cachedApk, moduleId)
                    if (capability?.moduleVersion?.toString() == metadata.version) {
                        Log.d(TAG, "Using cached APK for module=$moduleId")
                        return ApkRegistry.createDynamicApkInfo(
                            cachedApk.absolutePath, moduleName, metadata.version,
                            capability.initializerMode.moduleApiClassName!!,
                            apkSha256 = metadata.sha256
                        )
                    }
                    Log.w(TAG, "Cached APK has no supported Chimera capability for module=$moduleId")
                    cachedApk.delete()
                } else {
                    Log.w(TAG, "Cached APK is invalid for module=$moduleId")
                    cachedApk.delete()
                }
            }

            val uri = "content://com.google.android.gms.chimera/module_apk/$moduleId".toUri()
            val pfd = try {
                resolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                Log.d(TAG, "ContentProvider openFile failed for $moduleId: ${e.message}")
                return null
            } ?: return null

            cacheDir.mkdirs()
            val tmpFile = File(cacheDir, "$safeId.tmp-${UUID.randomUUID()}")
            try {
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (!isValidApk(tmpFile) || !matchesExpectedDigest(tmpFile, metadata.sha256)) {
                    Log.w(TAG, "ContentProvider served invalid APK for $moduleName (${tmpFile.length()} bytes)")
                    tmpFile.delete()
                    return null
                }

                val copiedCapability = readModuleCapability(tmpFile, moduleId)
                if (copiedCapability?.moduleVersion?.toString() != metadata.version) {
                    Log.w(TAG, "Provider APK capability mismatch for $moduleId: expected=${metadata.version}")
                    tmpFile.delete()
                    return null
                }
                if (!tmpFile.renameTo(cachedApk)) {
                    tmpFile.delete()
                    Log.w(TAG, "Failed to rename cached APK for $moduleName")
                    return null
                }

                Log.d(TAG, "Cached verified APK for module=$moduleId version=${metadata.version}")
                val moduleVersion = metadata.version
                cacheDir.listFiles()?.filter {
                    it.isFile && it.name.startsWith("$safeId-") && it != cachedApk
                }?.forEach(File::delete)
                return ApkRegistry.createDynamicApkInfo(
                    cachedApk.absolutePath, moduleName, moduleVersion,
                    copiedCapability.initializerMode.moduleApiClassName!!,
                    apkSha256 = metadata.sha256
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache APK via ContentProvider for $moduleName", e)
                tmpFile.delete()
                runCatching { pfd.close() }
                return null
            }
        } catch (e: Exception) {
            Log.d(TAG, "ContentProvider-based APK resolution failed for $moduleId: ${e.message}")
            return null
        }
    }

    private data class ProviderApkMetadata(val version: String, val sha256: String)

    private fun queryProviderMetadata(
        resolver: android.content.ContentResolver,
        moduleId: String,
    ): ProviderApkMetadata? {
        val uri = "content://com.google.android.gms.chimera/api/$moduleId".toUri()
        return resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val version = cursor.getLong(cursor.getColumnIndexOrThrow("version"))
                .takeIf { it > 0 }
                ?.toString()
                ?: return@use null
            val shaIndex = cursor.getColumnIndex("apkSha256")
            val sha256 = if (shaIndex >= 0) cursor.getString(shaIndex).orEmpty() else ""
            if (sha256.isEmpty()) null else ProviderApkMetadata(version, sha256)
        }
    }

    private fun readModuleCapability(apkFile: File, moduleId: String): ChimeraModuleCapabilities? =
        ChimeraApkManifestReader.readCapabilities(apkFile).firstOrNull {
            it.moduleId == moduleId && it.initializerMode.moduleApiClassName != null
        }

    private fun matchesExpectedDigest(file: File, expected: String): Boolean {
        if (expected.isEmpty()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) digest.update(buffer, 0, read)
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expected, ignoreCase = true)
    }

    private fun isValidApk(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                zip.getEntry("AndroidManifest.xml") != null || zip.getEntry("classes.dex") != null
            }
        } catch (_: Exception) {
            false
        }
    }
}
