/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.os.Build.VERSION.SDK_INT
import android.os.WorkSource
import androidx.annotation.RequiresApi
import com.android.location.provider.ProviderPropertiesUnbundled
import com.android.location.provider.ProviderRequestUnbundled
import org.microg.gms.location.*
import org.microg.gms.location.network.LOCATION_EXTRA_PRECISION
import org.microg.gms.location.network.NetworkLocationService
import org.microg.gms.location.provider.NetworkLocationProviderService.Companion.ACTION_REPORT_LOCATION
import java.io.PrintWriter
import kotlin.math.max

class NetworkLocationProviderPreTiramisu : AbstractLocationProviderPreTiramisu {
    @Deprecated("Use only with SDK < 31")
    constructor(context: Context, legacy: Unit) : super(properties) {
        this.context = context
    }

    @RequiresApi(31)
    constructor(context: Context) : super(context, properties) {
        this.context = context
    }

    private val context: Context
    private var enabled = false
    private var currentRequest: ProviderRequestUnbundled? = null
    private var pendingIntent: PendingIntent? = null
    private var lastReportedLocation: Location? = null

    private fun updateRequest() {
        if (enabled) {
            val forceNow: Boolean
            val intervalMillis: Long
            if (currentRequest?.reportLocation == true) {
                forceNow = true
                intervalMillis = max(currentRequest?.interval ?: Long.MAX_VALUE, MIN_INTERVAL_MILLIS)
            } else {
                forceNow = false
                intervalMillis = Long.MAX_VALUE
            }
            val intent = Intent(context, NetworkLocationService::class.java)
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
            context.startService(intent)
        }
    }

    override fun dump(writer: PrintWriter) {
        writer.println("Enabled: $enabled")
        writer.println("Current request: $currentRequest")
        writer.println("Last reported: $lastReportedLocation")
    }

    override fun onSetRequest(request: ProviderRequestUnbundled, source: WorkSource) {
        synchronized(this) {
            currentRequest = request
            updateRequest()
        }
    }

    override fun enable() {
        synchronized(this) {
            if (enabled) throw IllegalStateException()
            val intent = Intent(context, NetworkLocationProviderService::class.java)
            intent.action = ACTION_REPORT_LOCATION
            pendingIntent = PendingIntent.getService(context, 0, intent, (if (SDK_INT >= 31) FLAG_MUTABLE else 0) or FLAG_UPDATE_CURRENT)
            currentRequest = null
            enabled = true
            when {
                SDK_INT >= 30 -> isAllowed = true
                SDK_INT >= 29 -> isEnabled = true
            }
        }
    }

    override fun disable() {
        synchronized(this) {
            if (!enabled) throw IllegalStateException()
            val intent = Intent(context, NetworkLocationService::class.java)
            intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent)
            intent.putExtra(EXTRA_ENABLE, false)
            context.startService(intent)
            pendingIntent?.cancel()
            pendingIntent = null
            currentRequest = null
            enabled = false
        }
    }

    override fun reportLocationToSystem(location: Location) {
        location.provider = "network"
        location.extras?.remove(LOCATION_EXTRA_PRECISION)
        lastReportedLocation = location
        super.reportLocation(location)
    }

    companion object {
        private const val MIN_INTERVAL_MILLIS = 20000L
        private val properties = ProviderPropertiesUnbundled.create(false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE)
    }
}