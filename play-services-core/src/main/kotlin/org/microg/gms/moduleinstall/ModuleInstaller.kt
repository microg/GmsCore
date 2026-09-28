/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.moduleinstall

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModuleBootstrap
import com.google.android.chimera.config.ChimeraStorage
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.loader.ChimeraModuleLdr
import com.google.android.gms.chimera.DynamiteContextFactory
import com.google.android.gms.chimera.ModuleInfo
import com.google.android.gms.common.app.AppContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Atomic persistence boundary for already-validated dynamic-module APKs. */
internal object ModuleInstaller {
    private const val TAG = "GmsModule/Installer"

    data class Request(
        val sourceApk: File,
        val moduleName: String,
        val version: Long,
        val oldApkPaths: Set<String>,
        val invalidateModuleIds: Set<String>,
    )

    /**
     * Stages every request, commits all registrations together, and rolls back unreferenced files if
     * persistence fails. Cleanup and cache invalidation happen only after the durable commit.
     */
    @Synchronized
    internal fun installBatch(context: Context, requests: List<Request>) {
        check(DynamicModuleSettings.isAvailable(context)) {
            "Dynamic modules are unavailable on this device or disabled by the user"
        }
        if (requests.isEmpty()) return
        val staged = ArrayList<ModuleInfo>(requests.size)
        val createdFiles = ArrayList<File>(requests.size)
        try {
            requests.forEach { request -> staged += stage(context, request, createdFiles) }
            registerInstalled(context, staged)
        } catch (error: Exception) {
            runCatching { ChimeraConfigManager.reload() }
            createdFiles.forEach { file ->
                if (!ChimeraConfigManager.isApkPathReferenced(file.absolutePath)) {
                    ChimeraStorage.safeDeleteModuleApk(context, file)
                }
            }
            throw error
        }

        requests.flatMap(Request::oldApkPaths).distinct().forEach { oldPath ->
            runCatching {
                if (!ChimeraConfigManager.isApkPathReferenced(oldPath)) {
                    ChimeraStorage.safeDeleteModuleApk(context, File(oldPath))
                }
            }.onFailure { Log.w(TAG, "Unable to remove obsolete module artifact") }
        }
        requests.flatMap { request ->
            request.invalidateModuleIds.map { moduleId -> moduleId to request.moduleName }
        }.distinct()
            .forEach { (moduleId, moduleName) ->
                runCatching { invalidateRuntimeCaches(moduleId, moduleName, null) }
                    .onFailure { Log.w(TAG, "Unable to invalidate runtime cache for $moduleId") }
            }
    }

    private fun stage(
        context: Context,
        request: Request,
        createdFiles: MutableList<File>,
    ): ModuleInfo {
        val destination = ChimeraStorage.allocateModuleApkFile(
            context,
            request.moduleName,
            request.version.toString(),
        )
        val finalFile = destination.file
        val tempFile = File(finalFile.parentFile, "${finalFile.name}.tmp-${UUID.randomUUID()}")
        try {
            request.sourceApk.copyTo(tempFile, overwrite = false)
            if (!tempFile.renameTo(finalFile)) {
                throw IOException("Failed to finalize module APK")
            }
            // Track the finalized file before permission or digest work so any later failure can roll it back.
            createdFiles += finalFile
            ChimeraStorage.makeModuleApkReadable(finalFile)
            return ModuleInfo(
                source = Uri.fromFile(finalFile).toString(),
                module_name = request.moduleName,
                module_version = request.version.toString(),
                filename = finalFile.name,
                sha256_hash = sha256Hex(finalFile),
                priority = destination.priority,
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun registerInstalled(context: Context, modules: List<ModuleInfo>) {
        ChimeraModuleBootstrap.ensureInitialized(context)
        if (!AppContext.isInitialized()) {
            (context.applicationContext as? android.app.Application)?.let(AppContext::init)
        }
        ChimeraConfigManager.updateModuleDownload(modules)

        val persisted = ChimeraConfigManager.reload()
        val missing = modules.filterNot { info ->
            val expectedPath = info.source?.let { Uri.parse(it).path }.orEmpty()
            val expectedDigest = info.sha256_hash.orEmpty()
            expectedPath.isNotEmpty() && persisted.chimeraModules.any { module ->
                module.installedApkPath == expectedPath &&
                        (expectedDigest.isEmpty() || module.apkSha256 == expectedDigest)
            }
        }
        check(missing.isEmpty()) {
            "Module registration was not persisted: ${missing.map { it.module_name }}"
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun invalidateRuntimeCaches(
        moduleId: String? = null,
        moduleName: String? = null,
        apkPath: String? = null,
    ) {
        ChimeraModuleLdr.clearModuleCache(moduleId, moduleName, apkPath)
        if (!moduleId.isNullOrEmpty()) DynamiteContextFactory.clearCacheForModule(moduleId)
    }
}
