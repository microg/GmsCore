/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.WorkSource
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import com.android.location.provider.ProviderPropertiesUnbundled
import com.android.location.provider.ProviderRequestUnbundled
import org.microg.gms.location.elapsedMillis
import org.microg.gms.location.formatRealtime
import java.io.PrintWriter
import kotlin.math.max

class IntentLocationProviderPreTiramisu : AbstractLocationProviderPreTiramisu {
    @Deprecated("Use only with SDK < 31")
    constructor(service: IntentLocationProviderService, properties: ProviderPropertiesUnbundled, legacy: Unit) : super(properties) {
        this.service = service
    }

    @RequiresApi(31)
    constructor(service: IntentLocationProviderService, properties: ProviderPropertiesUnbundled) : super(service, properties) {
        this.service = service
    }

    private val service: IntentLocationProviderService
    private var enabled = false
    private var currentRequest: ProviderRequestUnbundled? = null
    private var pendingIntent: PendingIntent? = null
    private var lastReportedLocation: Location? = null
    private var lastReportTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val reportAgainRunnable = Runnable { reportAgain() }

    private fun updateRequest() {
        if (enabled && pendingIntent != null) {
            service.requestIntentUpdated(currentRequest, pendingIntent!!)
            reportAgain()
        }
    }

    override fun dump(writer: PrintWriter) {
        writer.println("Enabled: $enabled")
        writer.println("Current request: $currentRequest")
        if (SDK_INT >= 31) writer.println("Current work source: ${currentRequest?.workSource}")
        writer.println("Last reported: $lastReportedLocation")
        writer.println("Last report time: ${lastReportTime.formatRealtime()}")
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
            val intent = Intent(service, service.javaClass)
            intent.action = ACTION_REPORT_LOCATION
            pendingIntent = PendingIntentCompat.getService(service, 0, intent, FLAG_UPDATE_CURRENT, true)
            currentRequest = null
            enabled = true
            when {
                SDK_INT >= 30 -> isAllowed = true
                SDK_INT >= 29 -> isEnabled = true
            }
            try {
                if (lastReportedLocation == null) {
                    lastReportedLocation = service.getSystemService<LocationManager>()?.getLastKnownLocation(service.providerName)
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
    }

    override fun disable() {
        synchronized(this) {
            if (!enabled || pendingIntent == null) throw IllegalStateException()
            service.stopIntentUpdated(pendingIntent!!)
            pendingIntent?.cancel()
            pendingIntent = null
            currentRequest = null
            enabled = false
            handler.removeCallbacks(reportAgainRunnable)
        }
    }

    private fun reportAgain() {
        // Report location again if it's recent enough
        lastReportedLocation?.let {
            if (it.elapsedMillis + max(currentRequest?.interval ?: 0, service.minIntervalMillis) > SystemClock.elapsedRealtime()) {
                reportLocationToSystem(it)
            }
        }
    }

    override fun reportLocationToSystem(location: Location) {
        handler.removeCallbacks(reportAgainRunnable)
        location.provider = service.providerName
        lastReportedLocation = location
        lastReportTime = SystemClock.elapsedRealtime()
        super.reportLocation(location)
        val repeatInterval = max(service.minReportMillis, currentRequest?.interval ?: Long.MAX_VALUE)
        if (repeatInterval < service.minIntervalMillis) {
            handler.postDelayed(reportAgainRunnable, repeatInterval)
        }
    }
}