/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.chimera

import android.content.Context
import android.util.Log
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraStorage
import com.google.android.gms.common.app.AppContext
import org.microg.gms.moduleinstall.ModuleInstaller
import java.io.File

object ChimeraModuleRemover {
    private const val TAG = "ChimeraModuleRemover"

    fun remove(context: Context, moduleName: String): Boolean {
        // The content provider process may not have AppContext initialized; without this, the
        // saveToFile in updateConfig below silently fails because configFile cannot be resolved
        // (the removal never reaches chimera_manifest.pb and the module is not deleted).
        if (!AppContext.isInitialized()) {
            (context.applicationContext as? android.app.Application)?.let { AppContext.init(it) }
        }
        runCatching { ChimeraConfigManager.reload() }

        val configEntry = ChimeraConfigManager.findModuleByModuleName(moduleName)
        val configPath = configEntry?.installedApkPath
        // Clean config metadata before deleting the apk so we can still read the old ChimeraManifest.pb and
        // remove feature descriptors/routes that belonged to this moduleName. Deleting only chimeraModules leaves
        // stale features behind and makes later availability checks report removed modules as installed.
        val removedMetadata = ChimeraConfigManager.removeModuleMetadata(moduleName, configPath)
        if (!removedMetadata.persisted) {
            Log.e(TAG, "Refusing to delete APK after metadata persistence failed for $moduleName")
            return false
        }
        var deleteFailed = false
        for (path in (removedMetadata.apkPaths + listOfNotNull(configPath)).distinct()) {
            if (ChimeraConfigManager.isApkPathReferenced(path)) continue
            val f = File(path)
            if (f.exists() && !ChimeraStorage.safeDeleteModuleApk(context, f)) {
                Log.w(TAG, "Failed to delete module APK")
                deleteFailed = true
            }
        }

        while (true) {
            val f = ChimeraStorage.findDownloadedApk(context, moduleName) ?: break
            if (ChimeraConfigManager.isApkPathReferenced(f.absolutePath)) break
            if (!ChimeraStorage.safeDeleteModuleApk(context, f)) {
                Log.w(TAG, "Failed to delete orphan APK")
                deleteFailed = true
                break
            }
        }

        val idsToInvalidate = removedMetadata.moduleIds.ifEmpty { setOfNotNull(configEntry?.moduleId) }
        if (idsToInvalidate.isEmpty()) {
            ModuleInstaller.invalidateRuntimeCaches(moduleName = moduleName, apkPath = configPath)
        } else {
            idsToInvalidate.forEach { moduleId ->
                ModuleInstaller.invalidateRuntimeCaches(moduleId, moduleName, configPath)
            }
        }
        return !deleteFailed
    }
}
