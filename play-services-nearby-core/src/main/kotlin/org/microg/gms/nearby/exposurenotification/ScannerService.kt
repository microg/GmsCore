/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import org.microg.gms.common.ForegroundServiceContext
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*

@TargetApi(21)
class ScannerService : Service() {
    private var scanning = false
    private var lastStartTime = 0L
    private var seenAdvertisements = 0L
    private var lastAdvertisement = 0L
    private lateinit var database: ExposureDatabase
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { onScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(TAG, "onBatchScanResults: ${results.size}")
            for (result in results) {
                onScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "onScanFailed: $errorCode")
            stopScan()
        }
    }
    private val trigger = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.bluetooth.adapter.action.STATE_CHANGED") {
                when (intent.getIntExtra(EXTRA_STATE, -1)) {
                    STATE_TURNING_OFF, STATE_OFF -> stopScan()
                    STATE_ON -> startScanIfNeeded()
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val stopLaterRunnable = Runnable { stopScan() }
    private val startLaterRunnable = Runnable { startScan() }

    private val scanner: BluetoothLeScanner?
        get() = getDefaultAdapter()?.bluetoothLeScanner

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        super.onStartCommand(intent, flags, startId)
        startScanIfNeeded()
        return START_STICKY
    }

    fun onScanResult(result: ScanResult) {
        val data = result.scanRecord?.serviceData?.get(SERVICE_UUID) ?: return
        if (data.size < 16) return // Ignore invalid advertisements
        database.noteAdvertisement(data.sliceArray(0..15), data.drop(16).toByteArray(), result.rssi)
        seenAdvertisements++
        lastAdvertisement = System.currentTimeMillis()
    }

    fun startScanIfNeeded() {
        if (ExposurePreferences(this).enabled) {
            startScan()
        } else {
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = ExposureDatabase.ref(this)
        registerReceiver(trigger, IntentFilter().also { it.addAction("android.bluetooth.adapter.action.STATE_CHANGED") })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(trigger)
        stopScan()
        database.unref()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Synchronized
    private fun startScan() {
        if (scanning) return
        val scanner = scanner ?: return
        Log.d(TAG, "Starting scanner for service $SERVICE_UUID")
        handler.removeCallbacks(startLaterRunnable)
        seenAdvertisements = 0
        scanner.startScan(
                listOf(ScanFilter.Builder()
                        .setServiceUuid(SERVICE_UUID)
                        .setServiceData(SERVICE_UUID, byteArrayOf(0), byteArrayOf(0))
                        .build()),
                ScanSettings.Builder()
                        .let { if (Build.VERSION.SDK_INT >= 23) it.setMatchMode(ScanSettings.MATCH_MODE_STICKY) else it }
                        .build(),
                callback
        )
        scanning = true
        lastStartTime = System.currentTimeMillis()
        handler.postDelayed(stopLaterRunnable, SCANNING_TIME_MS)
    }

    @Synchronized
    private fun stopScan() {
        if (!scanning) return
        Log.d(TAG, "Stopping scanner for service $SERVICE_UUID")
        handler.removeCallbacks(stopLaterRunnable)
        scanning = false
        scanner?.stopScan(callback)
        if (ExposurePreferences(this).enabled) {
            handler.postDelayed(startLaterRunnable, ((lastStartTime + SCANNING_INTERVAL_MS) - System.currentTimeMillis()).coerceIn(0, SCANNING_INTERVAL_MS))
        }
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Scanning now: $scanning")
        writer?.println("Last scan start: ${Date(lastStartTime)}")
        if (Build.VERSION.SDK_INT >= 29) {
            writer?.println("Scan start pending: ${handler.hasCallbacks(startLaterRunnable)}")
            writer?.println("Scan stop pending: ${handler.hasCallbacks(stopLaterRunnable)}")
        }
        writer?.println("Seen advertisements since last scan start: $seenAdvertisements")
        writer?.println("Last advertisement seen: ${Date(lastAdvertisement)}")
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).enabled
        }
    }
}
