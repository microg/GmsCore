/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera

import android.content.Context
import android.content.ContextWrapper
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.google.android.chimera.annotation.ChimeraApiVersion
import com.google.android.chimera.config.ChimeraApkManifestReader
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.DynamicModuleSettings
import org.microg.gms.common.Constants

@ChimeraApiVersion(added = 0L)
@Keep
open class BoundService: ContextWrapper(null) {

    companion object {
        private const val TAG = "BoundService"

        @JvmStatic
        fun getStartIntent(context: Context, action: String): Intent? {
            Log.d(TAG, "getStartIntent: action: $action context: $context")
            if (!DynamicModuleSettings.isAvailable(context)) {
                Log.d(TAG, "Dynamic modules unavailable for bound service action: $action")
                return null
            }
            val intent = Intent("com.google.android.chimera.BoundService.START").setData(Uri.fromParts("chimera-action", action, null))
            val prefix = Constants.GMS_PACKAGE_NAME
            try {
                val boundServiceProxy = ChimeraConfigManager.findChimeraBoundService(
                    action.removePrefix(prefix)
                )
                if (boundServiceProxy?.moduleChimeraName == null) {
                    Log.w(TAG, "No bound service route for action: $action")
                    return null
                }
                val hostClassName = "$prefix${boundServiceProxy.moduleChimeraName}"
                val moduleId = boundServiceProxy.moduleId?.takeIf { it.isNotEmpty() }
                if (moduleId != null) {
                    val module = ChimeraConfigManager.findModuleByModuleId(moduleId)
                    val verifiedRoute = module != null &&
                        ChimeraApkManifestReader.readVerifiedCapabilities(context, module).any { capability ->
                            capability.moduleId == moduleId &&
                                capability.boundServiceBindings.any { binding ->
                                    binding.containerName == boundServiceProxy.containerName &&
                                        binding.moduleChimeraName == boundServiceProxy.moduleChimeraName
                                }
                        }
                    if (!verifiedRoute) {
                        Log.w(TAG, "Bound service route is not declared by the verified module APK: $action")
                        return null
                    }
                }
                val hostComponent = ComponentName(context, hostClassName)
                if (runCatching { context.packageManager.getServiceInfo(hostComponent, 0) }.isFailure) {
                    Log.w(TAG, "No host service proxy declared for bound service action: $action")
                    return null
                }
                intent.component = hostComponent
                Log.d(TAG, "BoundServiceProxy intent: $intent ${intent.data?.getSchemeSpecificPart()}")
                return intent
            } catch (e: IndexOutOfBoundsException) {
                Log.w(TAG, "Possible corrupt config", e);
                return null;
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    fun getBoundService(): BoundService {
        return this
    }
}
