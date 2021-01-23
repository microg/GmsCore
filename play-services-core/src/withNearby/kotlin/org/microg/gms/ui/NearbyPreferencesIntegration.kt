/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.R
import org.microg.gms.nearby.exposurenotification.Constants
import org.microg.gms.nearby.exposurenotification.getExposureNotificationsServiceInfo

interface NearbyPreferencesIntegration {
    companion object {
        suspend fun getExposurePreferenceSummary(context: Context): String = if (isAvailable && getExposureNotificationsServiceInfo(context).configuration.enabled) {
            context.getString(R.string.service_status_enabled_short)
        } else {
            context.getString(R.string.service_status_disabled_short)
        }

        fun preProcessSettingsIntent(intent: Intent) {
            if (Constants.ACTION_EXPOSURE_NOTIFICATION_SETTINGS == intent.action && intent.data == null) {
                intent.data = Uri.parse("x-gms-settings://exposure-notifications")
            }
        }

        fun getIcon(context: Context): Drawable? = ContextCompat.getDrawable(context, org.microg.gms.nearby.core.R.drawable.ic_virus_outline)

        val isAvailable: Boolean = android.os.Build.VERSION.SDK_INT >= 21

        const val exposureNotificationNavigationId: Int = R.id.openExposureNotificationSettings
    }
}
