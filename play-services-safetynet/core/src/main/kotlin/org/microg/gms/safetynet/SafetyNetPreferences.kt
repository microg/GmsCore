/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.safetynet

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.SafetyNet.ENABLED

object SafetyNetPreferences {
    private fun <T> getSettings(context: Context, projection: String, def: T, f: (Cursor) -> T): T {
        return try {
            SettingsContract.getSettings(context, SettingsContract.SafetyNet.getContentUri(context), arrayOf(projection), f)
        } catch (e: Exception) {
            def
        }
    }

    private fun setSettings(context: Context, f: ContentValues.() -> Unit) =
            SettingsContract.setSettings(context, SettingsContract.SafetyNet.getContentUri(context), f)

    @JvmStatic
    fun isEnabled(context: Context): Boolean = getSettings(context, ENABLED, false) { it.getInt(0) != 0 }

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) = setSettings(context) { put(ENABLED, enabled) }
}
