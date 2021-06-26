/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.checkin

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.settings.SettingsContract.CheckIn
import org.microg.gms.settings.SettingsContract.getSettings
import org.microg.gms.settings.SettingsContract.setSettings
import java.io.Serializable

data class ServiceInfo(val configuration: ServiceConfiguration, val lastCheckin: Long, val androidId: Long) : Serializable

data class ServiceConfiguration(val enabled: Boolean) : Serializable

suspend fun getCheckinServiceInfo(context: Context): ServiceInfo = withContext(Dispatchers.IO) {
    val projection = arrayOf(CheckIn.ENABLED, CheckIn.LAST_CHECK_IN, CheckIn.ANDROID_ID)
    getSettings(context, CheckIn.CONTENT_URI, projection) { c ->
        ServiceInfo(
            configuration = ServiceConfiguration(c.getInt(0) != 0),
            lastCheckin = c.getLong(1),
            androidId = c.getLong(2),
        )
    }
}

suspend fun setCheckinServiceConfiguration(context: Context, configuration: ServiceConfiguration) = withContext(Dispatchers.IO) {
    val serviceInfo = getCheckinServiceInfo(context)
    if (serviceInfo.configuration == configuration) return@withContext
    // enabled state is not already set, setting it now
    setSettings(context, CheckIn.CONTENT_URI) {
        put(CheckIn.ENABLED, configuration.enabled)
    }
    if (configuration.enabled) {
        context.sendOrderedBroadcast(Intent(context, TriggerReceiver::class.java), null)
    }
}
