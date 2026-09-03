/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.Build
import android.util.Log
import org.microg.gms.common.Constants
import org.microg.gms.settings.SettingsContract

/** One reusable gate for every dynamic-module entry point. */
object DynamicModuleSettings {
    /**
     * Dynamic module resources rely on the platform ResourcesLoader API introduced in Android 11.
     * Keep the application itself compatible with older releases while making this optional runtime
     * capability explicitly unavailable there.
     */
    @JvmStatic
    fun isRuntimeSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    @JvmStatic
    fun isEnabled(context: Context): Boolean = runCatching {
        val settingsContext = resolveSettingsContext(context)
        val projection = arrayOf(SettingsContract.DynamicModule.DYNAMIC_MODULE_ENABLED)
        SettingsContract.getSettings(
            settingsContext,
            SettingsContract.DynamicModule.getContentUri(settingsContext),
            projection
        ) { cursor -> cursor.getInt(0) != 0 }
    }.onFailure { Log.w(TAG, "Unable to read dynamic-module setting", it) }
        .getOrDefault(false)

    /** True only when the user has enabled the feature on a platform that can load module resources. */
    @JvmStatic
    fun isAvailable(context: Context): Boolean = isRuntimeSupported() && isEnabled(context)

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean): Boolean = runCatching {
        val settingsContext = resolveSettingsContext(context)
        SettingsContract.setSettings(
            settingsContext,
            SettingsContract.DynamicModule.getContentUri(settingsContext)
        ) {
            put(SettingsContract.DynamicModule.DYNAMIC_MODULE_ENABLED, enabled)
        }
        true
    }.onFailure { Log.w(TAG, "Unable to update dynamic-module setting", it) }
        .getOrDefault(false)

    /** Dynamite code may run in a third-party process, whose package has no microG SettingsProvider. */
    private fun resolveSettingsContext(context: Context): Context {
        return if (context.packageName == Constants.GMS_PACKAGE_NAME) {
            context
        } else {
            context.createPackageContext(Constants.GMS_PACKAGE_NAME, 0)
        }
    }

    private const val TAG = "DynamicModuleSettings"
}
