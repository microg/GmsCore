/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.checkin

import android.content.Context
import org.microg.mgms.settings.SettingsContract
import org.microg.mgms.settings.SettingsContract.CheckIn
import org.microg.mgms.settings.SettingsContract.setSettings

object CheckinPrefs {

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        val projection = arrayOf(CheckIn.ENABLED)
        return SettingsContract.getSettings(context, CheckIn.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun isSpoofingEnabled(context: Context): Boolean {
        val projection = arrayOf(CheckIn.BRAND_SPOOF)
        return SettingsContract.getSettings(context, CheckIn.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setSpoofingEnabled(context: Context, enabled: Boolean) {
        setSettings(context, CheckIn.getContentUri(context)) {
            put(CheckIn.BRAND_SPOOF, enabled)
        }
    }

    @JvmStatic
    fun hideLauncherIcon(context: Context, enabled: Boolean) {
        setSettings(context, CheckIn.getContentUri(context)) {
            put(CheckIn.HIDE_LAUNCHER_ICON, enabled)
        }
    }

}
