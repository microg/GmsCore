/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.microg.gms.common.PackageUtils

class ExposurePreferences(private val context: Context) {
    private var preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        PackageUtils.warnIfNotPersistentProcess(ExposurePreferences::class.java)
    }

    var enabled
        get() = preferences.getBoolean(PREF_SCANNER_ENABLED, false)
        set(newStatus) {
            val changed = enabled != newStatus
            preferences.edit().putBoolean(PREF_SCANNER_ENABLED, newStatus).commit()
            if (!changed) return
            if (newStatus) {
                context.sendOrderedBroadcast(Intent(context, ServiceTrigger::class.java), null)
            } else {
                context.stopService(Intent(context, ScannerService::class.java))
                context.stopService(Intent(context, AdvertiserService::class.java))
            }
        }

    var lastCleanup
        get() = preferences.getLong(PREF_LAST_CLEANUP, 0)
        set(value) = preferences.edit().putLong(PREF_LAST_CLEANUP, value).apply()

    companion object {
        private const val PREF_SCANNER_ENABLED = "exposure_scanner_enabled"
        private const val PREF_LAST_CLEANUP = "exposure_last_cleanup"
    }
}
