/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.Keep
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.NavController
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import org.microg.gms.ui.settings.SettingsProvider
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.exposurenotification.ExposureDatabase
import org.microg.gms.nearby.exposurenotification.ExposurePreferences
import org.microg.gms.ui.settings.SettingsProvider.Companion.Entry
import org.microg.gms.ui.settings.SettingsProvider.Companion.Group.OTHER

@Keep
object ExposureNotificationsSettingsProvider : SettingsProvider {
    override fun getEntriesStatic(context: Context): List<Entry> {
        if (SDK_INT < 21) return emptyList()
        if (!ExposurePreferences(context).enabled) return emptyList()
        return getEntries(context)
    }

    override suspend fun getEntriesDynamic(context: Context): List<Entry> {
        if (SDK_INT < 21) return emptyList()
        if (!ExposurePreferences(context).enabled) {
            if (ExposureDatabase.with(context) { it.isEmpty }) {
                return emptyList()
            }
        }
        return getEntries(context)
    }

    private fun getEntries(context: Context) = listOf(
        Entry(
            key = "pref_exposure",
            group = OTHER,
            navigationId = R.id.exposureNotificationsFragment,
            title = context.getString(R.string.service_name_exposure),
            summary = if (ExposurePreferences(context).enabled) {
                context.getString(org.microg.gms.base.core.R.string.service_status_enabled_short)
            } else {
                context.getString(org.microg.gms.base.core.R.string.service_status_disabled_short)
            },
            icon = AppCompatResources.getDrawable(context, R.drawable.ic_virus_outline)
        )
    )

    override fun preProcessSettingsIntent(intent: Intent) {
        if (ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS == intent.action && intent.data == null) {
            intent.data = Uri.parse("x-gms-settings://exposure-notifications")
        }
    }

    override fun extendNavigation(navController: NavController) {
        navController.graph.addAll(navController.navInflater.inflate(R.navigation.nav_nearby))
    }
}