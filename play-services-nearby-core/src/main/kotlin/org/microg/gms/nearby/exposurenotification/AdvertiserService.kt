/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.le.*
import android.bluetooth.le.AdvertiseSettings.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.common.ForegroundServiceContext
import java.io.FileDescriptor
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.*

@TargetApi(21)
class AdvertiserService : LifecycleService() {
    private val version = VERSION_1_0
    private var advertising = false
    private var wantStartAdvertising = false
    private val advertiser: BluetoothLeAdvertiser?
        get() = getDefaultAdapter()?.bluetoothLeAdvertiser
    private val alarmManager: AlarmManager
        get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val callback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising active for ${settingsInEffect?.timeout}ms")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "Advertising failed: $errorCode")
            stopOrRestartAdvertising()
        }
    }

    @TargetApi(23)
    private var setCallback: Any? = null
    private val trigger = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STATE_CHANGED) {
                when (intent.getIntExtra(EXTRA_STATE, -1)) {
                    STATE_TURNING_OFF, STATE_OFF -> stopOrRestartAdvertising()
                    STATE_ON -> startAdvertisingIfNeeded()
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val startLaterRunnable = Runnable { startAdvertisingIfNeeded() }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(trigger, IntentFilter().apply { addAction(ACTION_STATE_CHANGED) })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ForegroundServiceContext.completeForegroundService(this, intent, TAG)
        Log.d(TAG, "AdvertisingService.start: $intent")
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_RESTART_ADVERTISING && advertising) {
            stopOrRestartAdvertising()
        } else {
            startAdvertisingIfNeeded()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(trigger)
        stopOrRestartAdvertising()
        handler.removeCallbacks(startLaterRunnable)
    }

    private fun startAdvertisingIfNeeded() {
        if (ExposurePreferences(this).enabled) {
            lifecycleScope.launchWhenStarted {
                withContext(Dispatchers.IO) {
                    startAdvertising()
                }
            }
        } else {
            stopSelf()
        }
    }

    private var lastStartTime = System.currentTimeMillis()
    private var sendingBytes = ByteArray(0)
    private var starting = false

    private suspend fun startAdvertising() {
        val advertiser = synchronized(this) {
            if (advertising || starting) return
            val advertiser = advertiser ?: return
            wantStartAdvertising = false
            starting = true
            advertiser
        }
        try {
            val aemBytes = when (version) {
                VERSION_1_0 -> byteArrayOf(
                        version, // Version and flags
                        currentDeviceInfo.txPowerCorrection, // TX power
                        0x00, // Reserved
                        0x00  // Reserved
                )
                VERSION_1_1 -> byteArrayOf(
                        (version + currentDeviceInfo.confidence.toByte() * 4).toByte(), // Version and flags
                        currentDeviceInfo.txPowerCorrection, // TX power
                        0x00, // Reserved
                        0x00  // Reserved
                )
                else -> return
            }
            var nextSend = nextKeyMillis.coerceAtLeast(10000)
            val payload = ExposureDatabase.with(this@AdvertiserService) { database ->
                database.generateCurrentPayload(aemBytes)
            }
            val data = AdvertiseData.Builder().addServiceUuid(SERVICE_UUID).addServiceData(SERVICE_UUID, payload).build()
            val (uuid, _) = ByteBuffer.wrap(payload).let { UUID(it.long, it.long) to it.int }
            Log.i(TAG, "Starting advertiser for RPI $uuid")
            if (Build.VERSION.SDK_INT >= 26) {
                setCallback = SetCallback()
                val params = AdvertisingSetParameters.Builder()
                        .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
                        .setLegacyMode(true)
                        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_LOW)
                        .setConnectable(false)
                        .build()
                advertiser.startAdvertisingSet(params, data, null, null, null, setCallback as AdvertisingSetCallback)
            } else {
                nextSend = nextSend.coerceAtMost(180000)
                val settings = Builder()
                        .setTimeout(nextSend.toInt())
                        .setAdvertiseMode(ADVERTISE_MODE_BALANCED)
                        .setTxPowerLevel(ADVERTISE_TX_POWER_LOW)
                        .setConnectable(false)
                        .build()
                advertiser.startAdvertising(settings, data, callback)
            }
            synchronized(this) { advertising = true }
            sendingBytes = payload
            lastStartTime = System.currentTimeMillis()
            scheduleRestartAdvertising(nextSend)
        } finally {
            synchronized(this) { starting = false }
        }
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Advertising: $advertising")
        try {
            val startTime = lastStartTime
            val bytes = sendingBytes
            val (uuid, aem) = ByteBuffer.wrap(bytes).let { UUID(it.long, it.long) to it.int }
            writer?.println("""
                Last advertising:
                    Since: ${Date(startTime)}
                    RPI: $uuid
                    Version: 0x${version.toString(16)}
                    TX Power: ${currentDeviceInfo.txPowerCorrection}
                    AEM: 0x${aem.toLong().let { if (it < 0) 0x100000000L + it else it }.toString(16)}
                """.trimIndent())
        } catch (e: Exception) {
            writer?.println("Last advertising: ${e.message ?: e.toString()}")
        }
    }

    private fun scheduleRestartAdvertising(nextSend: Long) {
        val intent = Intent(this, AdvertiserService::class.java).apply { action = ACTION_RESTART_ADVERTISING }
        val pendingIntent = PendingIntent.getService(this, ACTION_RESTART_ADVERTISING.hashCode(), intent, FLAG_ONE_SHOT and FLAG_UPDATE_CURRENT)
        when {
            Build.VERSION.SDK_INT >= 23 ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextSend, pendingIntent)
            else ->
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + nextSend, pendingIntent)
        }
    }

    @Synchronized
    private fun stopOrRestartAdvertising() {
        if (!advertising) return
        val (uuid, _) = ByteBuffer.wrap(sendingBytes).let { UUID(it.long, it.long) to it.int }
        Log.i(TAG, "Stopping advertiser for RPI $uuid")
        advertising = false
        if (Build.VERSION.SDK_INT >= 26) {
            wantStartAdvertising = true
            advertiser?.stopAdvertisingSet(setCallback as AdvertisingSetCallback)
        } else {
            advertiser?.stopAdvertising(callback)
        }
        handler.postDelayed(startLaterRunnable, 1000)
    }

    @TargetApi(26)
    inner class SetCallback : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            Log.d(TAG, "Advertising active, status=$status")
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            Log.d(TAG, "Advertising stopped")
            if (wantStartAdvertising) {
                startAdvertisingIfNeeded()
            } else {
                stopOrRestartAdvertising()
            }
        }
    }


    companion object {
        private const val ACTION_RESTART_ADVERTISING = "org.microg.gms.nearby.exposurenotification.RESTART_ADVERTISING"

        fun isNeeded(context: Context): Boolean {
            return ExposurePreferences(context).enabled
        }

        fun isSupported(context: Context): Boolean? {
            val adapter = getDefaultAdapter()
            return when {
                adapter == null -> false
                Build.VERSION.SDK_INT >= 26 && (adapter.isLeExtendedAdvertisingSupported || adapter.isLePeriodicAdvertisingSupported) -> true
                adapter.state != STATE_ON -> null
                adapter.bluetoothLeAdvertiser != null -> true
                else -> false
            }
        }
    }
}
