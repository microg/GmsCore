/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.findmydevice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import org.microg.gms.profile.Build

class AlarmRingService : Service() {

    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRing()
            ACTION_STOP -> stopRing()
        }
        return START_STICKY
    }

    private fun startRing() {
        if (ringtone?.isPlaying == true) return

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        ringtone = RingtoneManager.getRingtone(this, uri).apply {
            if (Build.VERSION.SDK_INT >= 21) {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= 28) {
                isLooping = true
            }
            play()
        }
    }

    private fun stopRing() {
        ringtone?.let {
            if (it.isPlaying) it.stop()
        }
        ringtone = null
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmRingService::WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        stopRing()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_START = "alarm_ring_start"
        private const val ACTION_STOP = "alarm_ring_stop"

        fun stopRing(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun startRing(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}