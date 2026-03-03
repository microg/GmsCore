package org.microg.gms.wearable

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class WearableListenerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("WearableListenerService", "Bound for action=${intent?.action}")
        // No-op stub for WearOS binding
        return null
    }
}