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
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import java.io.FileDescriptor
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@TargetApi(21)
class AdvertiserService : LifecycleService() {
    private val version = VERSION_1_0
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
        database = ExposureDatabase.ref(this)
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
        database.unref()
    }

    fun startAdvertising() {
        lifecycleScope.launchWhenStarted {
            do {
                val aem = when (version) {
                    VERSION_1_0 -> byteArrayOf(
                            version, // Version and flags
                            (currentDeviceInfo.txPowerCorrection + TX_POWER_LOW).toByte(), // TX power
                            0x00, // Reserved
                            0x00  // Reserved
                    )
                    VERSION_1_1 -> byteArrayOf(
                            (version + currentDeviceInfo.confidence * 4).toByte(), // Version and flags
                            (currentDeviceInfo.txPowerCorrection + TX_POWER_LOW).toByte(), // TX power
                            0x00, // Reserved
                            0x00  // Reserved
                    )
                    else -> return@launchWhenStarted
                }
                val payload = database.generateCurrentPayload(aem)
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
        val (uuid, aem) = ByteBuffer.wrap(bytes).let { UUID(it.long, it.long) to it.int }
        Log.d(TAG, "RPI: $uuid, Version: 0x${version.toString(16)}, TX Power: ${currentDeviceInfo.txPowerCorrection + TX_POWER_LOW}, AEM: 0x${aem.toLong().let { if (it < 0) 0x100000000L + it else it }.toString(16)}, Timeout: ${nextSend}ms")
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
