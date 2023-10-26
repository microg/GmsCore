/*
 * SPDX-FileCopyrightText: 2023, e Foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.play

import android.content.Context
import org.microg.gms.settings.SettingsContract

object PlayPreferences {
    @JvmStatic
    fun isLicensingEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Play.LICENSING)
        return SettingsContract.getSettings(context, SettingsContract.Play.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setLicensingEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Play.getContentUri(context)) {
            put(SettingsContract.Play.LICENSING, enabled)
        }
    }
}