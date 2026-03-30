/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.feature

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.microg.gms.settings.SettingsContract

object GoogleFeaturePreferences {
    private fun <T> getSettings(context: Context, vararg projection: String, f: (Cursor) -> T): T =
        SettingsContract.getSettings(
            context,
            SettingsContract.GoogleFeature.getContentUri(context),
            projection,
            f
        )

    private fun setSettings(context: Context, v: ContentValues.() -> Unit) =
        SettingsContract.setSettings(context, SettingsContract.GoogleFeature.getContentUri(context), v)

    fun setAllowMapsTimelineFeature(context: Context, allowed: Boolean) {
        setSettings(context) { put(SettingsContract.GoogleFeature.MAPS_TIMELINE, allowed) }
    }

    fun allowedMapsTimelineFeature(context: Context): Boolean {
        return getSettings(context, SettingsContract.GoogleFeature.MAPS_TIMELINE) { c -> c.getInt(0) != 0 }
    }

    fun setMapsTimelineUpload(context: Context, allowed: Boolean) {
        setSettings(context) { put(SettingsContract.GoogleFeature.MAPS_TIMELINE_UPLOAD, allowed) }
    }

    fun allowedMapsTimelineUpload(context: Context): Boolean {
        return getSettings(context, SettingsContract.GoogleFeature.MAPS_TIMELINE_UPLOAD) { c -> c.getInt(0) != 0 }
    }

}