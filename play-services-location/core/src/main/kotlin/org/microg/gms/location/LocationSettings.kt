/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location

import android.content.Context
import org.microg.gms.settings.SettingsContract

class LocationSettings(private val context: Context) {
    var wifiMls : Boolean
        get() = SettingsContract.getSettings(context, SettingsContract.Location.getContentUri(context), arrayOf(SettingsContract.Location.WIFI_MLS)) { c ->
            c.getInt(0) != 0
        }
        set(value) {
            SettingsContract.setSettings(context, SettingsContract.Location.getContentUri(context)) { put(SettingsContract.Location.WIFI_MLS, value)}
        }

    var wifiMoving: Boolean
        get() = SettingsContract.getSettings(context, SettingsContract.Location.getContentUri(context), arrayOf(SettingsContract.Location.WIFI_MOVING)) { c ->
            c.getInt(0) != 0
        }
        set(value) {
            SettingsContract.setSettings(context, SettingsContract.Location.getContentUri(context)) { put(SettingsContract.Location.WIFI_MOVING, value)}
        }

    var cellMls : Boolean
        get() = SettingsContract.getSettings(context, SettingsContract.Location.getContentUri(context), arrayOf(SettingsContract.Location.CELL_MLS)) { c ->
            c.getInt(0) != 0
        }
        set(value) {
            SettingsContract.setSettings(context, SettingsContract.Location.getContentUri(context)) { put(SettingsContract.Location.CELL_MLS, value)}
        }
}