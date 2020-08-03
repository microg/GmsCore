/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.os.IBinder

@TargetApi(21)
class ScannerService : Service() {
    private var started = false
    private lateinit var db: ExposureDatabase
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val data = result?.scanRecord?.serviceData?.get(SERVICE_UUID) ?: return
            if (data.size < 16) return // Ignore invalid advertisements
            db.noteAdvertisement(data.sliceArray(0..15), data.drop(16).toByteArray(), result.rssi)
        }
    }
    private val scanner: BluetoothLeScanner
        get() = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (ExposurePreferences(this).scannerEnabled) {
            startScan()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScan()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Synchronized
    private fun startScan() {
        if (started) return
        db = ExposureDatabase(this)
        scanner.startScan(
                listOf(ScanFilter.Builder().setServiceUuid(SERVICE_UUID).setServiceData(SERVICE_UUID, byteArrayOf(0), byteArrayOf(0)).build()),
                ScanSettings.Builder().build(),
                callback
        )
        started = true
    }

    @Synchronized
    private fun stopScan() {
        if (!started) return
        scanner.stopScan(callback)
        db.close()
        started = false
    }
}
