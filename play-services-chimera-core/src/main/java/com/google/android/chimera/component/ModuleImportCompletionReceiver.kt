/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.chimera.config.ModuleDownloadRegistry

/** Restores the pending request task after its requested module has been imported. */
class ModuleImportCompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ModuleDownloadActivity.ACTION_DOWNLOAD_TARGET_SELECTED) {
            val requestId = intent.getStringExtra(ModuleDownloadActivity.EXTRA_REQUEST_ID).orEmpty()
            if (!PendingModuleRequestStore.markDownloadTargetSelected(context, requestId)) {
                Log.w(TAG, "Ignoring chooser callback for unknown request")
            }
            return
        }
        if (intent.action != ModuleDownloadActivity.ACTION_MODULE_IMPORT_COMPLETED) return
        val pending = PendingModuleRequestStore.loadAll(context).firstOrNull { request ->
            val modules = ModuleDownloadRegistry.resolveModules(request.features)
            val currentModule = modules.firstOrNull { it.catalogId == request.currentCatalogId }
                ?: return@firstOrNull false
            val componentForModule = request.componentClassName?.takeIf {
                modules.size == 1 || it in currentModule.requiredComponentClassNames
            }
            ModuleDownloadRegistry.isModuleInstalled(
                context,
                currentModule,
                request.features,
                componentForModule,
            )
        } ?: return
        if (pending.taskId < 0) {
            PendingModuleRequestStore.remove(context, pending.requestId)
            return
        }
        runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .moveTaskToFront(pending.taskId, 0)
        }.onFailure {
            Log.w(TAG, "Unable to restore pending module task", it)
            PendingModuleRequestStore.remove(context, pending.requestId)
        }
    }

    private companion object {
        const val TAG = "ChimeraImportReceiver"
    }
}
