/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.location.base.BuildConfig
import org.microg.gms.settings.SettingsContract

private const val PATH_GEOLOCATE = "/v1/geolocate"
private const val PATH_GEOLOCATE_QUERY = "/v1/geolocate?"
private const val PATH_GEOSUBMIT = "/v2/geosubmit"
private const val PATH_GEOSUBMIT_QUERY = "/v2/geosubmit?"
private const val PATH_QUERY_ONLY = "/?"

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

    var wifiCaching: Boolean
        get() = getSettings(SettingsContract.Location.WIFI_CACHING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.WIFI_CACHING, value) }

    var cellIchnaea: Boolean
        get() = getSettings(SettingsContract.Location.CELL_ICHNAEA) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.CELL_ICHNAEA, value) }

    var cellLearning: Boolean
        get() = getSettings(SettingsContract.Location.CELL_LEARNING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.CELL_LEARNING, value) }

    var cellCaching: Boolean
        get() = getSettings(SettingsContract.Location.CELL_CACHING) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.CELL_CACHING, value) }

    var geocoderNominatim: Boolean
        get() = getSettings(SettingsContract.Location.GEOCODER_NOMINATIM) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.GEOCODER_NOMINATIM, value) }

    var customEndpoint: String?
        get() {
            try {
                var endpoint = getSettings(SettingsContract.Location.ICHNAEA_ENDPOINT) { c -> c.getString(0) }
                // This is only temporary as users might have already broken configuration.
                // Usually this would be corrected before storing it in settings, see below.
                if (endpoint.endsWith(PATH_GEOLOCATE)) {
                    endpoint = endpoint.substring(0, endpoint.length - PATH_GEOLOCATE.length + 1)
                } else if (endpoint.contains(PATH_GEOLOCATE_QUERY)) {
                    endpoint = endpoint.replace(PATH_GEOLOCATE_QUERY, PATH_QUERY_ONLY)
                } else if (endpoint.endsWith(PATH_GEOSUBMIT)) {
                    endpoint = endpoint.substring(0, endpoint.length - PATH_GEOSUBMIT.length + 1)
                } else if (endpoint.contains(PATH_GEOSUBMIT_QUERY)) {
                    endpoint = endpoint.replace(PATH_GEOSUBMIT_QUERY, PATH_QUERY_ONLY)
                }
                return endpoint
            } catch (e: Exception) {
                return null
            }
        }
        set(value) {
            val endpoint = if (value == null) {
                null
            } else if (value.endsWith(PATH_GEOLOCATE)) {
                value.substring(0, value.length - PATH_GEOLOCATE.length + 1)
            } else if (value.contains(PATH_GEOLOCATE_QUERY)) {
                value.replace(PATH_GEOLOCATE_QUERY, PATH_QUERY_ONLY)
            } else if (value.endsWith(PATH_GEOSUBMIT)) {
                value.substring(0, value.length - PATH_GEOSUBMIT.length + 1)
            } else if (value.contains(PATH_GEOSUBMIT_QUERY)) {
                value.replace(PATH_GEOSUBMIT_QUERY, PATH_QUERY_ONLY)
            } else {
                value
            }
            setSettings { put(SettingsContract.Location.ICHNAEA_ENDPOINT, endpoint) }
        }

    var onlineSourceId: String?
        get() = getSettings(SettingsContract.Location.ONLINE_SOURCE) { c -> c.getString(0) }
        set(value) = setSettings { put(SettingsContract.Location.ONLINE_SOURCE, value) }

    var ichnaeaContribute: Boolean
        get() = getSettings(SettingsContract.Location.ICHNAEA_CONTRIBUTE) { c -> c.getInt(0) != 0 }
        set(value) = setSettings { put(SettingsContract.Location.ICHNAEA_CONTRIBUTE, value) }
}