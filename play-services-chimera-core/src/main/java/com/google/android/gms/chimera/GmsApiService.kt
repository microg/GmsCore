/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.chimera

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.LifecycleService
import org.microg.gms.BaseService

/**
 * GMS Service Proxy for Chimera bound services.
 *
 * Client sends: action="com.google.android.chimera.BoundService.START"
 *               data="chimera-action:actual.service.action"
 *
 * This proxy converts the Intent and routes to the appropriate service implementation
 * using ChimeraConfigManager's service route table.
 */
@Keep
open class GmsApiService : LifecycleService() {
    private val TAG = "GmsApiService"

    // Routes chimera-action to real service implementations.
    // When a dynamically loaded module calls BoundService.getStartIntent("action"),
    // the Intent is routed here and we look up the real service to delegate to.
    private val fallbackServiceMap = mapOf(
        "com.google.android.gms.chimera.container.moduleinstall.ModuleInstallService.START"
                to "org.microg.gms.moduleinstall.ModuleInstallService",
        "com.google.android.gms.phenotype.service.START"
                to "org.microg.gms.phenotype.PhenotypeService",
        "com.google.android.gms.clearcut.service.START"
                to "org.microg.gms.clearcut.ClearcutLoggerService",
        "com.google.android.gms.games.service.START"
                to "org.microg.gms.games.GamesService",
    )

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind intent: $intent")

        val resolvedIntent = resolveChimeraIntent(intent)
        val action = resolvedIntent.action ?: return null

        val targetClassName = fallbackServiceMap[action]
        if (targetClassName == null) {
            Log.w(TAG, "No service found for action: $action")
            return null
        }

        return bindToService(targetClassName, resolvedIntent)
    }

    /**
     * Converts chimera-action Intent.
     * Input:  action="com.google.android.chimera.BoundService.START", data="chimera-action:real.action"
     * Output: action="real.action"
     */
    private fun resolveChimeraIntent(intent: Intent): Intent {
        if (intent.action != "com.google.android.chimera.BoundService.START") {
            return intent
        }

        val uri = intent.data ?: return intent
        val realAction = uri.schemeSpecificPart
        if (uri.scheme == "chimera-action" && !realAction.isNullOrEmpty()) {
            return Intent(intent).apply {
                action = realAction
                data = Uri.Builder().scheme("chimera-action").build()
            }
        }

        Log.w(TAG, "Intent missing action data: $intent")
        return intent
    }

    private fun bindToService(targetClassName: String, intent: Intent): IBinder? {
        try {
            val clazz = Class.forName(targetClassName)
            val service = clazz.getDeclaredConstructor().newInstance() as? Service ?: return null

            val attachMethod = Service::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            attachMethod.isAccessible = true
            attachMethod.invoke(service, this)

            Log.d(TAG, "Chimera bridge to $targetClassName")
            if (service is BaseService) {
                return service.onBind(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chimera service: $targetClassName", e)
        }
        return null
    }

}
