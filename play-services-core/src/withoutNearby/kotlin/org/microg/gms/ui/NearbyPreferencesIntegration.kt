/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.google.android.gms.R

interface NearbyPreferencesIntegration {
    companion object {
        suspend fun getExposurePreferenceSummary(context: Context): String = context.getString(R.string.service_status_disabled_short)
        fun preProcessSettingsIntent(intent: Intent) {}
        fun getIcon(context: Context): Drawable? = null
        const val exposureNotificationNavigationId: Int = 0
        const val isAvailable: Boolean = false
    }
}
