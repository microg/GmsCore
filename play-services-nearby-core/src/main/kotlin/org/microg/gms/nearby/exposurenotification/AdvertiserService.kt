/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseSettings.*
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import org.microg.gms.common.ForegroundServiceContext
import java.io.FileDescriptor
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

@TargetApi(21)
class AdvertiserService : LifecycleService() {
    private val version = VERSION_1_0
    private var looping = false
    private var callback: AdvertiseCallback? = null
    private val advertiser: BluetoothLeAdvertiser?
        get() = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser
    private lateinit var database: ExposureDatabase
    private val trigger = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.bluetooth.adapter.action.STATE_CHANGED") {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> stopAdvertising()
                    BluetoothAdapter.STATE_ON -> {
                        if (looping) {
                            lifecycleScope.launchWhenStarted { restartAdvertising() }
                        } else {
                            loopAdvertising()
                        }
                    }
                }
            }
        }
    }

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
        registerReceiver(trigger, IntentFilter().also { it.addAction("android.bluetooth.adapter.action.STATE_CHANGED") })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        super.onStartCommand(intent, flags, startId)
        if (ExposurePreferences(this).enabled) {
            loopAdvertising()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(trigger)
        stopAdvertising()
        database.unref()
    }

    @Synchronized
    fun loopAdvertising() {
        if (looping) return
        looping = true
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "Looping advertising")
            try {
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
                    val nextSend = (nextKeyMillis + Random.nextInt(-ADVERTISER_OFFSET, ADVERTISER_OFFSET)).coerceIn(0, 180000)
                    startAdvertising(payload, nextSend.toInt())
                    if (callback != null) delay(nextSend)
                } while (callback != null)
            } catch (e: Exception) {
                Log.w(TAG, "Error during advertising loop", e)
            }
            Log.d(TAG, "No longer advertising")
            synchronized(this@AdvertiserService) {
                looping = false
            }
        }
    }

    var startTime = System.currentTimeMillis()
    var sendingBytes = ByteArray(0)
    var sendingNext = 0
    suspend fun startAdvertising(bytes: ByteArray, nextSend: Int) {
        startTime = System.currentTimeMillis()
        sendingBytes = bytes
        sendingNext = nextSend
        continueAdvertising(bytes, nextSend)
    }

    private suspend fun continueAdvertising(bytes: ByteArray, nextSend: Int) {
        stopAdvertising()
        val data = AdvertiseData.Builder().addServiceUuid(SERVICE_UUID).addServiceData(SERVICE_UUID, bytes).build()
        val settings = Builder()
                .setTimeout(nextSend)
                .setAdvertiseMode(ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build()
        val (uuid, aem) = ByteBuffer.wrap(bytes).let { UUID(it.long, it.long) to it.int }
        Log.d(TAG, "RPI: $uuid, Version: 0x${version.toString(16)}, TX Power: ${currentDeviceInfo.txPowerCorrection + TX_POWER_LOW}, AEM: 0x${aem.toLong().let { if (it < 0) 0x100000000L + it else it }.toString(16)}, Timeout: ${nextSend}ms")
        callback = advertiser?.startAdvertising(settings, data)
    }

    suspend fun restartAdvertising() {
        val startTime = startTime
        val bytes = sendingBytes
        val next = sendingNext
        if (next == 0 || bytes.isEmpty()) return
        val nextSend = (startTime - System.currentTimeMillis() + next).toInt()
        if (nextSend < 5000) return
        continueAdvertising(bytes, nextSend)
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Looping: $looping")
        writer?.println("Active: ${callback != null}")
        try {
            val startTime = startTime
            val bytes = sendingBytes
            val (uuid, aem) = ByteBuffer.wrap(bytes).let { UUID(it.long, it.long) to it.int }
            writer?.println("""
                Last advertising:
                    Since: ${Date(startTime)}
                    RPI: $uuid
                    Version: 0x${version.toString(16)}
                    TX Power: ${currentDeviceInfo.txPowerCorrection + TX_POWER_LOW}
                    AEM: 0x${aem.toLong().let { if (it < 0) 0x100000000L + it else it }.toString(16)}
                """.trimIndent())
        } catch (e: Exception) {
            writer?.println("Last advertising: ${e.message ?: e.toString()}")
        }
    }

    @Synchronized
    fun stopAdvertising() {
        callback?.let { advertiser?.stopAdvertising(it) }
        callback = null
    }

    companion object {
        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).enabled
        }
    }
}
