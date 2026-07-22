/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import org.microg.gms.common.ForegroundServiceContext
import org.microg.gms.common.ForegroundServiceInfo
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*

@TargetApi(21)
@ForegroundServiceInfo("Exposure Notification")
class ScannerService : LifecycleService() {
    private var scanning = false
    private var lastStartTime = 0L
    private var seenAdvertisements = 0L
    private var lastAdvertisement = 0L
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { onScanResult(it) }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
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
            if (intent?.action == ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(EXTRA_STATE, -1)) {
                    STATE_TURNING_OFF, STATE_OFF -> stopScan()
                    STATE_ON -> startScanIfNeeded()
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val stopLaterRunnable = Runnable { stopScan() }

    // Wake lock for the duration of scan. Otherwise we might fall asleep while scanning
    // resulting in potentially very long scan times
    private val wakeLock: PowerManager.WakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ScannerService::class.java.canonicalName).apply { setReferenceCounted(false) }
    }

    private val scanner: BluetoothLeScanner?
        get() = getDefaultAdapter()?.bluetoothLeScanner
    private val alarmManager: AlarmManager
        get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val powerManager: PowerManager
        get() = getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        Log.d(TAG, "ScannerService.start: $intent")
        super.onStartCommand(intent, flags, startId)
        startScanIfNeeded()
        return START_STICKY
    }

    fun onScanResult(result: ScanResult) {
        val data = result.scanRecord?.serviceData?.get(SERVICE_UUID) ?: return
        if (data.size < 16) return // Ignore invalid advertisements
        seenAdvertisements++
        lastAdvertisement = System.currentTimeMillis()
        lifecycleScope.launchWhenStarted {
            ExposureDatabase.with(this@ScannerService) { database ->
                database.noteAdvertisement(data.sliceArray(0..15), data.drop(16).toByteArray(), result.rssi)
            }
        }
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
        registerReceiver(trigger, IntentFilter().apply { addAction(ACTION_STATE_CHANGED) })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(trigger)
        stopScan()
    }

    @SuppressLint("WakelockTimeout", "MissingPermission")
    @Synchronized
    private fun startScan() {
        if (scanning) return
        val scanner = scanner ?: return
        Log.i(TAG, "Starting scanner for service $SERVICE_UUID for ${SCANNING_TIME_MS}ms")
        seenAdvertisements = 0
        wakeLock.acquire()
        try {
            scanner.startScan(
                    listOf(ScanFilter.Builder()
                            .setServiceUuid(SERVICE_UUID)
                            .setServiceData(SERVICE_UUID, byteArrayOf(0), byteArrayOf(0))
                            .build()),
                    ScanSettings.Builder().build(),
                    callback
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Couldn't start ScannerService, need android.permission.BLUETOOTH_SCAN permission.")
        }
        scanning = true
        lastStartTime = System.currentTimeMillis()
        handler.postDelayed(stopLaterRunnable, SCANNING_TIME_MS)
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun stopScan() {
        if (!scanning) return
        Log.i(TAG, "Stopping scanner for service $SERVICE_UUID, had seen $seenAdvertisements advertisements")
        handler.removeCallbacks(stopLaterRunnable)
        scanning = false
        try {
            scanner?.stopScan(callback)
        } catch (e: Exception) {
            // Ignored
        }
        if (ExposurePreferences(this).enabled) {
            scheduleStartScan(((lastStartTime + SCANNING_INTERVAL_MS) - System.currentTimeMillis()).coerceIn(0, SCANNING_INTERVAL_MS))
        }
        wakeLock.release()
    }

    private fun scheduleStartScan(nextScan: Long) {
        val intent = Intent(this, ScannerService::class.java)
        val pendingIntent = PendingIntentCompat.getService(this, ScannerService::class.java.hashCode(), intent, FLAG_ONE_SHOT or FLAG_UPDATE_CURRENT, false)!!
        if (Build.VERSION.SDK_INT >= 23) {
            // Note: there is no setWindowAndAllowWhileIdle()
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextScan, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextScan - SCANNING_TIME_MS / 2, SCANNING_TIME_MS, pendingIntent)
        }
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Scanning now: $scanning")
        writer?.println("Last scan start: ${Date(lastStartTime)}")
        if (Build.VERSION.SDK_INT >= 29) {
            writer?.println("Scan stop pending: ${handler.hasCallbacks(stopLaterRunnable)}")
        }
        writer?.println("Seen advertisements since last scan start: $seenAdvertisements")
        writer?.println("Last advertisement seen: ${Date(lastAdvertisement)}")
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).enabled
        }

        fun isSupported(context: Context): Boolean? {
            val adapter = getDefaultAdapter()
            return when {
                adapter == null -> false
                adapter.state != STATE_ON -> null
                adapter.bluetoothLeScanner != null -> true
                else -> false
            }
        }
    }
}
