/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import org.microg.gms.common.ForegroundServiceContext
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@TargetApi(21)
class ScannerService : LifecycleService() {
    private val version = VERSION_1_0
    private var started = false
    private var looping = false
    private var startLoopTime = 0L
    private var lastScanTime = 0L
    private var nextScanTime = 0L
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
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> stopScanLoop()
                    BluetoothAdapter.STATE_ON -> {
                        if (looping) {
                            lifecycleScope.launchWhenStarted { restartScanLoop() }
                        } else {
                            startScanLoopIfNeeded()
                        }
                    }
                }
            }
        }
    }

    private val scanner: BluetoothLeScanner?
        get() = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner

    override fun onCreate() {
        super.onCreate()
        database = ExposureDatabase.ref(this)
        registerReceiver(trigger, IntentFilter().also { it.addAction("android.bluetooth.adapter.action.STATE_CHANGED") })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(trigger)
        stopScanLoop()
        database.unref()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        super.onStartCommand(intent, flags, startId)
        startScanLoopIfNeeded() 
        return START_STICKY
    }

    fun onScanResult(result: ScanResult) {
        val data = result.scanRecord?.serviceData?.get(SERVICE_UUID) ?: return
        if (data.size < 16) return // Ignore invalid advertisements
        database.noteAdvertisement(data.sliceArray(0..15), data.drop(16).toByteArray(), result.rssi)
        seenAdvertisements++
        lastAdvertisement = System.currentTimeMillis()
    }
    
    fun startScanLoopIfNeeded() {
        if (ExposurePreferences(this).enabled) {
            startScanLoop()
        } else {
            stopSelf()
        }
    }
    
    @Synchronized
    private fun startScan() {
        if (started) return
        val scanner = scanner ?: return
        Log.d(TAG, "Starting scanner for service $SERVICE_UUID")
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
        started = true
        lastScanTime = System.currentTimeMillis()
    }

    @Synchronized
    private fun stopScan() {
        if (!started) return
        Log.d(TAG, "Stopping scanner for service $SERVICE_UUID")
        started = false
        scanner?.stopScan(callback)
    }
    
    @Synchronized
    fun startScanLoop() {
        if (looping) return
        startLoopTime = System.currentTimeMillis()
        looping = true
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "Looping ScannerService")
            try {
                do {
                    nextScanTime = System.currentTimeMillis()+SCANNING_INTERVAL_MS
                    startScan()
                    delay(SCANNING_TIME_MS.toLong())
                    stopScan()
                    val delayNextScan = nextScanTime-System.currentTimeMillis()
                    if (delayNextScan > 0) {
                        delay(delayNextScan.toLong())
                    }
                } while (looping)
            } catch (e: Exception) {
                Log.w(TAG, "Error during ScannerService loop", e)
            }
            Log.d(TAG, "No longer looping ScannerService")
            synchronized(this@ScannerService) {
                looping = false
            }
        }
    }

    @Synchronized
    fun stopScanLoop() {
        stopScan()
        looping = false
    }

    suspend fun restartScanLoop() {
        stopScanLoop()
        startScanLoop()
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Looping: $looping")
        writer?.println("Active: $started")
        writer?.println("Seen advertisements: $seenAdvertisements")
        if (looping) {
            writer?.println("Looping since ${Date(startLoopTime)}")
            writer?.println("Last advertisement: ${Date(lastAdvertisement)}")
            writer?.println("Last scan: ${Date(lastScanTime)}")
            writer?.println("Next scan: ${Date(nextScanTime)}")
        }
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).enabled
        }
    }
}
