/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.PendingIntent
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import com.android.location.provider.ProviderPropertiesUnbundled
import com.android.location.provider.ProviderRequestUnbundled
import org.microg.gms.location.*
import org.microg.gms.location.network.LOCATION_EXTRA_PRECISION
import kotlin.math.max

class NetworkLocationProviderService : IntentLocationProviderService() {
    override fun extractLocation(intent: Intent): Location? = intent.getParcelableExtra<Location?>(EXTRA_LOCATION)?.apply {
        extras?.remove(LOCATION_EXTRA_PRECISION)
    }

    override fun requestIntentUpdated(currentRequest: ProviderRequestUnbundled?, pendingIntent: PendingIntent?) {
        val forceNow: Boolean
        val intervalMillis: Long
        if (currentRequest?.reportLocation == true) {
            forceNow = true
            intervalMillis = max(currentRequest.interval ?: Long.MAX_VALUE, minIntervalMillis)
        } else {
            forceNow = false
            intervalMillis = Long.MAX_VALUE
        }
        val intent = Intent(ACTION_NETWORK_LOCATION_SERVICE)
        intent.`package` = packageName
        intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        intent.putExtra(EXTRA_ENABLE, true)
        intent.putExtra(EXTRA_INTERVAL_MILLIS, intervalMillis)
        intent.putExtra(EXTRA_FORCE_NOW, forceNow)
        if (SDK_INT >= 31) {
            intent.putExtra(EXTRA_LOW_POWER, currentRequest?.isLowPower ?: false)
            intent.putExtra(EXTRA_WORK_SOURCE, currentRequest?.workSource)
        }
        if (SDK_INT >= 29) {
            intent.putExtra(EXTRA_BYPASS, currentRequest?.isLocationSettingsIgnored ?: false)
        }
        startService(intent)
    }

    override fun stopIntentUpdated(pendingIntent: PendingIntent?) {
        val intent = Intent(ACTION_NETWORK_LOCATION_SERVICE)
        intent.`package` = packageName
        intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        intent.putExtra(EXTRA_ENABLE, false)
        startService(intent)
    }

    override val minIntervalMillis: Long
        get() = MIN_INTERVAL_MILLIS
    override val minReportMillis: Long
        get() = MIN_REPORT_MILLIS
    override val properties: ProviderPropertiesUnbundled
        get() = PROPERTIES
    override val providerName: String
        get() = LocationManager.NETWORK_PROVIDER


    companion object {
        private const val MIN_INTERVAL_MILLIS = 20000L
        private const val MIN_REPORT_MILLIS = 1000L
        private val PROPERTIES = ProviderPropertiesUnbundled.create(false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE)
    }
}