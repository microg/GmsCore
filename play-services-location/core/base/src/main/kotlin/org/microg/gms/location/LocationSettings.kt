/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.settings.SettingsContract

class LocationSettings(private val context: Context) {
    private fun <T> getSettings(vararg projection: String, f: (Cursor) -> T): T =
        SettingsContract.getSettings(context, SettingsContract.Location.getContentUri(context), projection, f)

    private fun setSettings(v: ContentValues.() -> Unit) = SettingsContract.setSettings(context, SettingsContract.Location.getContentUri(context), v)

    var wifiIchnaea: Boolean
        get() = getSettings(SettingsContract.Location.WIFI_ICHNAEA) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.WIFI_ICHNAEA, value) }

    var wifiMoving: Boolean
        get() = getSettings(SettingsContract.Location.WIFI_MOVING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.WIFI_MOVING, value) }

    var wifiLearning: Boolean
        get() = getSettings(SettingsContract.Location.WIFI_LEARNING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.WIFI_LEARNING, value) }

    var cellIchnaea: Boolean
        get() = getSettings(SettingsContract.Location.CELL_ICHNAEA) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.CELL_ICHNAEA, value) }

    var cellLearning: Boolean
        get() = getSettings(SettingsContract.Location.CELL_LEARNING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.CELL_LEARNING, value) }

    var geocoderNominatim: Boolean
        get() = getSettings(SettingsContract.Location.GEOCODER_NOMINATIM) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.GEOCODER_NOMINATIM, value) }

    var ichneaeEndpoint: String
        get() = getSettings(SettingsContract.Location.ICHNAEA_ENDPOINT) { c -> c.getString(0) }
        set(value) = setSettings { put(SettingsContract.Location.ICHNAEA_ENDPOINT, value) }

    var ichnaeaContribute: Boolean
        get() = false
        set(value) = Unit
}