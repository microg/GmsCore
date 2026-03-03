package org.microg.gms.wearable

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

private const val TAG = "WearableService"

/**
 * Stub service for WearOS listener binding. Returns null binder until fully implemented.
 */
class WearableService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "WearableService bound with intent action ${intent.action}")
        // TODO: implement actual WearableListener.Stub
        return null
    }
}