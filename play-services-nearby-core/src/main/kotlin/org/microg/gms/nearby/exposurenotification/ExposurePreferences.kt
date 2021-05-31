/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.Context
import android.content.Intent
import org.microg.gms.settings.SettingsContract.Exposure.CONTENT_URI
import org.microg.gms.settings.SettingsContract.Exposure.LAST_CLEANUP
import org.microg.gms.settings.SettingsContract.Exposure.SCANNER_ENABLED
import org.microg.gms.settings.SettingsContract.getSettings
import org.microg.gms.settings.SettingsContract.setSettings

class ExposurePreferences(private val context: Context) {

    var enabled
        get() = getSettings(context, CONTENT_URI, arrayOf(SCANNER_ENABLED)) { c ->
            c.getInt(0) != 0
        }
        set(newStatus) {
            val changed = enabled != newStatus
            setSettings(context, CONTENT_URI) {
                put(SCANNER_ENABLED, newStatus)
            }
            if (!changed) return
            if (newStatus) {
                context.sendOrderedBroadcast(Intent(context, ServiceTrigger::class.java), null)
            } else {
                context.stopService(Intent(context, ScannerService::class.java))
                context.stopService(Intent(context, AdvertiserService::class.java))
            }
        }

    var lastCleanup
        get() = getSettings(context, CONTENT_URI, arrayOf(LAST_CLEANUP)) { c ->
            c.getLong(0)
        }
        set(value) = setSettings(context, CONTENT_URI) {
            put(LAST_CLEANUP, value)
        }

}
