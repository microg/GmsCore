/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import org.microg.gms.common.IIntentMessenger

class LocationManagerService : Service() {
    private val queue = ArrayDeque<Intent>()
    private var connecting = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val intentMessenger = IIntentMessenger.Stub.asInterface(service)
                while (!queue.isEmpty()) {
                    intentMessenger.sendIntent(queue.removeFirst())
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            } finally {
                runCatching { unbindService(this) }
                connecting = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connecting = false
        }

        override fun onBindingDied(name: ComponentName?) {
            connecting = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            queue.add(intent)
            if (!connecting) {
                connecting = true
                val intent = Intent(this, GoogleLocationManagerService::class.java).apply { action = GoogleLocationManagerService.ACTION_BIND_INTERNAL }
                bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_REPORT_LOCATION = "org.microg.gms.location.manager.ACTION_REPORT_LOCATION"
    }
}