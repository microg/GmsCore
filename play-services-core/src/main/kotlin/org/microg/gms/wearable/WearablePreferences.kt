/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable

import android.content.Context
import org.microg.gms.settings.SettingsContract

object WearablePreferences {
    @JvmStatic
    fun isAutoAcceptTosEnabled(context: Context): Boolean {
        val projection = arrayOf(SettingsContract.Wearable.AUTO_ACCEPT_TOS)
        return SettingsContract.getSettings(context, SettingsContract.Wearable.getContentUri(context), projection) { c ->
            c.getInt(0) != 0
        }
    }

    @JvmStatic
    fun setAutoAcceptTosEnabled(context: Context, enabled: Boolean) {
        SettingsContract.setSettings(context, SettingsContract.Wearable.getContentUri(context)) {
            put(SettingsContract.Wearable.AUTO_ACCEPT_TOS, enabled)
        }
    }
}
