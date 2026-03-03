package org.microg.gms.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Basic WearOS listener stub to handle messages and data events from wearable devices.
 */
class NotificationBridgeService : WearableListenerService() {
    companion object {
        private const val TAG = "GmsWearBridge"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}, data=${String(messageEvent.data)}")
        // TODO: handle messages (e.g., notification echo, media controls)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged: events=${dataEvents.count}")
        // TODO: handle data sync (e.g., app data, assets)
    }
}