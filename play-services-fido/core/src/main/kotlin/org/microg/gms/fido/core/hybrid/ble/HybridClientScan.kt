/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.hybrid.EMPTY_SERVICE_DATA
import org.microg.gms.fido.core.hybrid.EMPTY_SERVICE_DATA_MASK
import org.microg.gms.fido.core.hybrid.UUID_ANDROID
import org.microg.gms.fido.core.hybrid.UUID_IOS
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "HybridClientScan"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class HybridClientScan(
    private val bluetoothLeAdapter: BluetoothAdapter?, private val onScanSuccess: (ByteArray) -> Unit, private val onScanFailed: (Throwable) -> Unit
) : ScanCallback() {

    private val scanStatus = AtomicBoolean(false)

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        stopScanning()
        val scanRecord = result.scanRecord
        if (scanRecord == null) {
            Log.d(TAG, "processDevice: ScanResult is missing ScanRecord")
            return onScanFailed(RequestHandlingException(ErrorCode.DATA_ERR, "ScanResult is missing ScanRecord."))
        }
        var serviceData = scanRecord.getServiceData(UUID_ANDROID)
        if (serviceData == null) {
            Log.d(TAG, "processDevice: No service data, checking iOS UUID")
            serviceData = scanRecord.getServiceData(UUID_IOS)
        }
        if (serviceData == null) {
            Log.d(TAG, "processDevice: ScanRecord does not contain service data.")
            return onScanFailed(RequestHandlingException(ErrorCode.DATA_ERR, "ScanRecord does not contain service data."))
        }
        if (serviceData.size != 20) {
            Log.d(TAG, "processDevice: Service data is incorrect size.")
            return onScanFailed(RequestHandlingException(ErrorCode.DATA_ERR, "Service data is incorrect size."))
        }
        Log.d(TAG, "Target device with EID: ${serviceData.joinToString("") { "%02x".format(it) }}")
        onScanSuccess(serviceData)
    }

    override fun onScanFailed(errorCode: Int) {
        onScanFailed(RequestHandlingException(ErrorCode.UNKNOWN_ERR, "BLE scan failed: $errorCode"))
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (scanStatus.compareAndSet(false, true)) {
            try {
                val adapter = bluetoothLeAdapter ?: throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR, "BluetoothAdapter null")
                if (!adapter.isEnabled) {
                    val enabled = adapter.enable()
                    if (!enabled) {
                        throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR, "Unable to enable Bluetooth")
                    }
                }
                val scanner = adapter.bluetoothLeScanner ?: throw RequestHandlingException(ErrorCode.NOT_SUPPORTED_ERR, "BluetoothLeScanner null")

                val filters = listOf(
                    ScanFilter.Builder().setServiceData(UUID_ANDROID, EMPTY_SERVICE_DATA, EMPTY_SERVICE_DATA_MASK).build(),
                    ScanFilter.Builder().setServiceData(UUID_IOS, EMPTY_SERVICE_DATA, EMPTY_SERVICE_DATA_MASK).build()
                )
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                scanner.startScan(filters, settings, this)
                Log.d(TAG, "BLE scanning started")
            } catch (t: Throwable) {
                onScanFailed(RequestHandlingException(ErrorCode.UNKNOWN_ERR, "startScan failed: ${t.message}"))
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (scanStatus.compareAndSet(true, false)) {
            try {
                val bluetoothLeScanner = bluetoothLeAdapter?.bluetoothLeScanner
                bluetoothLeScanner?.stopScan(this)
                Log.d(TAG, "BLE scanning stopped")
            } catch (t: Throwable) {
                onScanFailed(RequestHandlingException(ErrorCode.UNKNOWN_ERR, "stopScan failed: ${t.message}"))
            }
        }
    }
}