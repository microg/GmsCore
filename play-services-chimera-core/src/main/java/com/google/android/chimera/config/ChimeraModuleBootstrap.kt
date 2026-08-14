/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.StrictMode
import android.util.Log
import com.google.android.chimera.config.registry.ContainerRouteRegistry
import com.google.android.gms.common.app.AppContext
import com.google.android.gms.common.app.GCoreApplicationContext

object ChimeraModuleBootstrap {
    private const val TAG = "ChimeraModuleBootstrap"

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val oldPolicy = StrictMode.allowThreadDiskWrites()
            try {
                doInit(context)
                initialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize local modules", e)
            } finally {
                StrictMode.setThreadPolicy(oldPolicy)
            }
        }
    }

    private fun doInit(context: Context) {
        if (!AppContext.isInitialized()) {
            (context.applicationContext as? android.app.Application)?.let { AppContext.init(it) }
        }

        val appContext = context.applicationContext

        val coreAppContext = GCoreApplicationContext.instance
        if (coreAppContext.baseContext == null) {
            coreAppContext.attachBaseContext(appContext)
        }

        val moduleDir = ChimeraStorage.ensureModuleRoot(appContext)

        registerContainerBoundServices()

        // An APK found on disk without a persisted config entry has no expected digest or verified
        // import transaction to bind it to. Keep it as an orphan for explicit user cleanup/re-import;
        // never turn a directory scan into a newly trusted executable module.
        val orphanCount = ChimeraStorage.listDownloadedApks(moduleDir).count { apk ->
            ChimeraConfigManager.findModuleByModuleName(apk.moduleName) == null
        }
        if (orphanCount != 0) {
            Log.w(TAG, "Ignoring $orphanCount unregistered Chimera module APK(s)")
        }
    }

    private fun registerContainerBoundServices() {
        ChimeraConfigManager.updateConfig(autoSave = true) { config ->
            val collections = config.collections?.newBuilder() ?: ChimeraModuleCollections.Builder()
            var changed = false

            val existingServices = collections.serviceRoutes.map { it.containerName }.toSet()
            val newRoutes = ContainerRouteRegistry.serviceRoutes.filter { it.containerName !in existingServices }
            if (newRoutes.isNotEmpty()) {
                Log.i(TAG, "Registering ${newRoutes.size} container service route(s)")
                collections.serviceRoutes += newRoutes
                changed = true
            }

            if (!changed) return@updateConfig config
            config.newBuilder().collections(collections.build()).build()
        }
    }

}
