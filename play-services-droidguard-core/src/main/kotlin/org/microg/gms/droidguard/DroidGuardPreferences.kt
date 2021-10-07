/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.database.Cursor
import androidx.core.database.getStringOrNull
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.DroidGuard.ENABLED
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

    @JvmStatic
    fun isEnabled(context: Context): Boolean = true //getSettings(context, ENABLED, false) { it.getInt(0) != 0 }

    @JvmStatic
    fun getMode(context: Context): Mode = getSettings(context, MODE, Mode.Embedded) { c -> Mode.valueOf(c.getString(0)) }

    @JvmStatic
    fun getNetworkServerUrl(context: Context): String? = getSettings(context, NETWORK_SERVER_URL, null) { c -> c.getStringOrNull(0) }

    enum class Mode {
        Embedded,
        Network
    }
}
