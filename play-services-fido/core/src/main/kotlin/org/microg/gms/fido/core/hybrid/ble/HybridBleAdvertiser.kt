/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import org.microg.gms.fido.core.hybrid.UUID_ANDROID
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "HybridBleAdvertiser"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HybridBleAdvertiser(
    private val bluetoothLeAdapter: BluetoothAdapter?,
) : AdvertiseCallback() {
    private val advertiserStatus = AtomicBoolean(false)
    private val timer = Timer()
    private val stopTimeTask = object : TimerTask() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun run() {
            stopAdvertising()
        }
    }

    private val bluetoothLeAdvertiser by lazy {
        if (bluetoothLeAdapter != null) {
            bluetoothLeAdapter.bluetoothLeAdvertiser
        } else {
            Log.d(TAG, "BLE_HARDWARE ERROR")
            null
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(eid: ByteArray) {
        if (advertiserStatus.compareAndSet(false, true)) {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .addServiceUuid(UUID_ANDROID)
                .addServiceData(UUID_ANDROID, eid)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

            bluetoothLeAdvertiser?.startAdvertising(settings, data, this)

            timer.schedule(stopTimeTask, 10000L)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        if (this.advertiserStatus.compareAndSet(true, false)) {
            timer.cancel()
            Log.d(TAG, "BLE_ADVERTISING_STOP")
            bluetoothLeAdvertiser?.stopAdvertising(this)
        }
    }

    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
        super.onStartSuccess(settingsInEffect)
        Log.d(TAG, String.format("BLE advertising onStartSuccess: %s", settingsInEffect))
    }

    override fun onStartFailure(errorCode: Int) {
        super.onStartFailure(errorCode)
        Log.d(TAG, String.format("BLE advertising onStartFailure: %d", errorCode))
    }
}