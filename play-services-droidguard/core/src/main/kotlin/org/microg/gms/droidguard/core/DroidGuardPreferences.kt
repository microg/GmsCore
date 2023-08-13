/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.core.database.getStringOrNull
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.DroidGuard.ENABLED
import org.microg.gms.settings.SettingsContract.DroidGuard.FORCE_LOCAL_DISABLED
import org.microg.gms.settings.SettingsContract.DroidGuard.MODE
import org.microg.gms.settings.SettingsContract.DroidGuard.NETWORK_SERVER_URL

object DroidGuardPreferences {

    private fun <T> getSettings(context: Context, projection: String, def: T, f: (Cursor) -> T): T {
        return try {
            SettingsContract.getSettings(context, SettingsContract.DroidGuard.getContentUri(context), arrayOf(projection), f)
        } catch (e: Exception) {
            def
        }
    }

    private fun setSettings(context: Context, f: ContentValues.() -> Unit) =
            SettingsContract.setSettings(context, SettingsContract.DroidGuard.getContentUri(context), f)

    @JvmStatic
    fun isForcedLocalDisabled(context: Context): Boolean = getSettings(context, FORCE_LOCAL_DISABLED, false) { it.getInt(0) != 0 }

    @JvmStatic
    fun isEnabled(context: Context): Boolean = getSettings(context, ENABLED, false) { it.getInt(0) != 0 }

    @JvmStatic
    fun isAvailable(context: Context): Boolean = isEnabled(context) && (!isForcedLocalDisabled(context) || getMode(context) != Mode.Embedded)

    @JvmStatic
    fun isLocalAvailable(context: Context): Boolean = isEnabled(context) && !isForcedLocalDisabled(context)

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) = setSettings(context) { put(ENABLED, enabled) }

    @JvmStatic
    fun getMode(context: Context): Mode = getSettings(context, MODE, Mode.Embedded) { c -> Mode.valueOf(c.getString(0)) }

    @JvmStatic
    fun setMode(context: Context, mode: Mode) = setSettings(context) { put(MODE, mode.toString()) }

    @JvmStatic
    fun getNetworkServerUrl(context: Context): String? = getSettings(context, NETWORK_SERVER_URL, null) { c -> c.getStringOrNull(0) }

    @JvmStatic
    fun setNetworkServerUrl(context: Context, url: String?) = setSettings(context) { put(NETWORK_SERVER_URL, url) }

    enum class Mode {
        Embedded,
        Network
    }
}
