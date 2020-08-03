/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.bluetooth.le.AdvertiseSettings.*
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import java.io.FileDescriptor
import java.io.PrintWriter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@TargetApi(21)
class AdvertiserService : LifecycleService() {
    private var callback: AdvertiseCallback? = null
    private val advertiser: BluetoothLeAdvertiser
        get() = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    private lateinit var database: ExposureDatabase

    private suspend fun BluetoothLeAdvertiser.startAdvertising(settings: AdvertiseSettings, advertiseData: AdvertiseData): AdvertiseCallback = suspendCoroutine {
        startAdvertising(settings, advertiseData, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                it.resume(this)
            }

            override fun onStartFailure(errorCode: Int) {
                it.resumeWithException(RuntimeException("Error code: $errorCode"))
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        database = ExposureDatabase(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (ExposurePreferences(this).advertiserEnabled) {
            startAdvertising()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
        database.close()
    }

    fun startAdvertising() {
        lifecycleScope.launchWhenStarted {
            do {
                val payload = database.generateCurrentPayload(byteArrayOf(
                        0x40, // Version 1.0
                        currentDeviceInfo.txPowerCorrection.toByte(),  // TX Power (TODO)
                        0x00, // Reserved
                        0x00  // Reserved
                ))
                var nextSend = nextKeyMillis.coerceAtMost(180000)
                startAdvertising(payload, nextSend.toInt())
                delay(nextSend)
            } while (callback != null)
        }
    }

    suspend fun startAdvertising(bytes: ByteArray, nextSend: Int) {
        stopAdvertising()
        val data = AdvertiseData.Builder().addServiceUuid(SERVICE_UUID).addServiceData(SERVICE_UUID, bytes).build()
        val settings = AdvertiseSettings.Builder()
                .setTimeout(nextSend)
                .setAdvertiseMode(ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build()
        callback = advertiser.startAdvertising(settings, data)
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Active: ${callback != null}")
        writer?.println("Currently advertising: ${database.currentRpiId}")
        writer?.println("Next key change in ${nextKeyMillis}ms")
    }

    @Synchronized
    fun stopAdvertising() {
        callback?.let { advertiser.stopAdvertising(it) }
        callback = null
    }
}
