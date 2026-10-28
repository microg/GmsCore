/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.content.Context
import android.os.Build
import androidx.preference.PreferenceManager

class NearbySharingPreferences(context: Context) {
    private val sp = PreferenceManager.getDefaultSharedPreferences(context)

    var deviceName: String
        get() = sp.getString(KEY_DEVICE_NAME, null) ?: Build.MODEL ?: "Android Device"
        set(value) = sp.edit().putString(KEY_DEVICE_NAME, value).apply()

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    var visibility: Int
        get() = sp.getInt(KEY_VISIBILITY, VISIBILITY_EVERYONE)
        set(value) = sp.edit().putInt(KEY_VISIBILITY, value).apply()

    companion object {
        const val KEY_DEVICE_NAME = "nearby_sharing_device_name"
        const val KEY_ENABLED = "nearby_sharing_enabled"
        const val KEY_VISIBILITY = "nearby_sharing_visibility"

        const val VISIBILITY_EVERYONE = 0
        const val VISIBILITY_CONTACTS = 1
        const val VISIBILITY_HIDDEN = 2
    }
}
