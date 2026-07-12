package org.microg.gms.wearable

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

/**
 * Lightweight Wearable API service entry for microG.
 * Wires [WearableCompanionManager] so clients can bind without crashing.
 * Full DataClient/MessageClient/NodeClient implementations remain follow-up work.
 */
class WearableApiService : BaseService(TAG, GmsService.WEARABLE) {

    private lateinit var companion: WearableCompanionManager

    override fun onCreate() {
        super.onCreate()
        companion = WearableCompanionManager.get(this)
        Log.i(TAG, "WearableApiService created; companion state=${companion.getState()}")
    }

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        try {
            // Accept bind so Wear companion apps / GMS clients do not fail immediately.
            // Return a minimal binder; expand with real Wearable service binder later.
            val binder = WearableBinder(companion)
            callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, null)
            Log.d(TAG, "Wearable service bound for package=${request.packageName}")
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to complete Wearable bind", e)
        }
    }

    /**
     * Minimal binder surface used until full AIDL/Wearable service is ported.
     */
    class WearableBinder(
        private val companion: WearableCompanionManager
    ) : android.os.Binder() {
        fun getPairingStateName(): String = companion.getState().name
        fun getActiveNodeId(): String? = companion.getActiveNode()?.id
        fun startDiscovery() = companion.startDiscovery()
        fun disconnect() = companion.disconnect()
    }

    companion object {
        private const val TAG = "GmsWearableApi"
    }
}

/**
 * Optional standalone service for process isolation experiments.
 * Prefer [WearableApiService] via microG BaseService registration in production.
 */
class WearableCompanionService : Service() {
    private lateinit var companion: WearableCompanionManager

    override fun onCreate() {
        super.onCreate()
        companion = WearableCompanionManager.get(this)
    }

    override fun onBind(intent: Intent?): IBinder {
        return WearableApiService.WearableBinder(companion)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DISCOVERY -> companion.startDiscovery()
            ACTION_DISCONNECT -> companion.disconnect()
            ACTION_ECHO_NOTIFICATION -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                companion.echoNotification(pkg, title, text)
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_START_DISCOVERY = "org.microg.gms.wearable.START_DISCOVERY"
        const val ACTION_DISCONNECT = "org.microg.gms.wearable.DISCONNECT"
        const val ACTION_ECHO_NOTIFICATION = "org.microg.gms.wearable.ECHO_NOTIFICATION"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
