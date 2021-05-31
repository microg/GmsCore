/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable

data class ServiceInfo(val configuration: ServiceConfiguration) : Serializable

data class ServiceConfiguration(val enabled: Boolean) : Serializable {
    fun saveToPrefs(context: Context) {
        ExposurePreferences(context).enabled = enabled
    }
}

private fun ExposurePreferences.toConfiguration(): ServiceConfiguration = ServiceConfiguration(enabled)

suspend fun getExposureNotificationsServiceInfo(context: Context): ServiceInfo =
    withContext(Dispatchers.IO) {
        ServiceInfo(ExposurePreferences(context).toConfiguration())
    }

suspend fun setExposureNotificationsServiceConfiguration(context: Context, configuration: ServiceConfiguration) =
    withContext(Dispatchers.IO) {
        ExposurePreferences(context).enabled = configuration.enabled
    }
