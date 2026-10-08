/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast

import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.cast.ApplicationMetadata
import com.google.android.gms.cast.ApplicationStatus
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastDeviceStatus
import com.google.android.gms.cast.JoinOptions
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.internal.ICastDeviceController
import com.google.android.gms.cast.internal.ICastDeviceControllerListener
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.images.WebImage
import com.google.android.gms.common.internal.BinderWrapper
import org.microg.gms.cast.channel.CastChannel
import org.microg.gms.cast.channel.CastDeviceSession
import org.microg.gms.cast.channel.ReceiverApplication
import org.microg.gms.cast.channel.ReceiverStatus

private const val TAG = "GmsCastDeviceController"

/**
 * Binder side of a Cast device connection for one client.
 *
 * Older clients pass their listener in the service request extras and expect the device to be connected when the
 * service request completes (see [connectBeforeInit]). Newer clients bind without a listener, then call [addListener]
 * and [connect] and wait for [ICastDeviceControllerListener.onConnectedWithResult].
 *
 * [onRelease] is called whenever the controller is disconnected. Newer clients may keep the controller binder for the
 * same device and call [connect] on it again; that opens a new session and calls [onReopen].
 */
class CastDeviceControllerImpl(
    private val packageName: String?,
    extras: Bundle,
    private val onRelease: (CastDeviceControllerImpl) -> Unit,
    private val onReopen: (CastDeviceControllerImpl) -> Unit,
) : ICastDeviceController.Stub() {
    private val castDevice: CastDevice
    private val port: Int

    // Callbacks of a replaced session are ignored
    @Volatile
    private var session: CastDeviceSession

    // Older clients reconnect through a new controller and never call disconnect() on the old one
    private val isLegacyClient: Boolean

    // The application an older client was attached to before it reconnected
    private val lastApplicationId: String?
    private val lastSessionId: String?

    @Volatile
    private var listener: ICastDeviceControllerListener? = null

    // Binder/callback pair whose process exit disconnects this controller, guarded by this.
    // The callback is bound to its binder so a callback already queued for a replaced listener
    // cannot tear down the replacement listener's live controller.
    private var linkedBinder: IBinder? = null
    private var linkedDeath: IBinder.DeathRecipient? = null

    private fun listenerDeath(binder: IBinder) = IBinder.DeathRecipient {
        val current = synchronized(this) {
            if (linkedBinder !== binder) {
                false
            } else {
                linkedBinder = null
                linkedDeath = null
                listener = null
                true
            }
        }
        if (current) {
            Log.d(TAG, "Client $packageName died, disconnecting from ${castDevice.friendlyName}")
            disconnect()
        }
    }

    @Volatile
    private var initCallback: ((Int) -> Unit)? = null

    @Volatile
    private var connectRequested = false

    @Volatile
    private var rejoining = false

    // Application the client is attached to, kept across a dropped channel so reconnect can join it.
    @Volatile
    private var attachedApplicationId: String? = null

    @Volatile
    private var attachedSessionId: String? = null

    @Volatile
    private var reconnecting = false

    @Volatile
    private var reconnectAttempts = 0

    @Volatile
    private var released = false

    init {
        extras.classLoader = BinderWrapper::class.java.classLoader
        castDevice = CastDevice.getFromBundle(extras) ?: throw IllegalArgumentException("No CastDevice in request extras")
        port = castDevice.servicePort.takeIf { it > 0 } ?: CastChannel.DEFAULT_PORT
        session = newSession()
        val listenerWrapper = extras.getParcelable<BinderWrapper>("listener")
        isLegacyClient = listenerWrapper != null
        lastApplicationId = extras.getString("last_application_id")?.takeIf { it.isNotEmpty() }
        lastSessionId = extras.getString("last_session_id")?.takeIf { it.isNotEmpty() }
        // A listener that is already dead is dropped, so no connection is opened for it
        if (listenerWrapper != null) setListener(ICastDeviceControllerListener.Stub.asInterface(listenerWrapper.binder))
    }

    val hasInitialListener: Boolean
        get() = listener != null

    private fun newSession(): CastDeviceSession {
        val callbacks = SessionCallbacks()
        return CastDeviceSession(castDevice.address, port, callbacks).also { callbacks.owner = it }
    }

    /**
     * Connect to the device, then report the result through [callback]: 0 on success, [STATUS_APP_NO_LONGER_RUNNING]
     * if the connection is up but the application the client was attached to before is gone.
     */
    fun connectBeforeInit(callback: (Int) -> Unit) {
        initCallback = callback
        session.connect()
    }

    override fun connect() {
        val reopened = synchronized(this) {
            if (!released) return@synchronized false
            // The client reuses this binder after it disconnected, e.g. when casting to the same device again
            released = false
            reconnecting = false
            reconnectAttempts = 0
            session = newSession()
            true
        }
        if (reopened) {
            onReopen(this)
            if (!synchronized(this) { setListener(listener) }) {
                disconnect()
                return
            }
        }
        connectRequested = true
        session.connect()
    }

    override fun addListener(listener: ICastDeviceControllerListener?) {
        if (!setListener(listener)) disconnect()
    }

    override fun removeListener() {
        setListener(null)
        disconnect()
    }

    override fun disconnect() {
        Log.d(TAG, "disconnect from ${castDevice.friendlyName} ($packageName)")
        val release = synchronized(this) {
            if (released) return
            released = true
            reconnecting = false
            rejoining = false
            connectRequested = false
            attachedApplicationId = null
            attachedSessionId = null
            val pendingInit = initCallback
            initCallback = null
            session to pendingInit
        }
        val (current, pendingInit) = release
        unlinkListener()
        CastChannelRegistry.unregister(castDevice.deviceId, current)
        current.disconnect()
        onRelease(this)
        // A legacy service request can still be waiting for its asynchronous connect result.
        // Complete it as a failure now; a stale session callback must not resurrect this controller.
        pendingInit?.invoke(CastDeviceSession.STATUS_NETWORK_ERROR)
    }

    /** Set the client listener and disconnect once its process dies. Returns false if it is already dead. */
    @Synchronized
    private fun setListener(newListener: ICastDeviceControllerListener?): Boolean {
        val binder = newListener?.asBinder()
        if (binder !== linkedBinder) {
            unlinkListener()
            if (binder != null) {
                val death = listenerDeath(binder)
                try {
                    binder.linkToDeath(death, 0)
                } catch (e: RemoteException) {
                    listener = null
                    return false
                }
                linkedBinder = binder
                linkedDeath = death
            }
        }
        listener = newListener
        return true
    }

    @Synchronized
    private fun unlinkListener() {
        val binder = linkedBinder ?: return
        val death = linkedDeath
        linkedBinder = null
        linkedDeath = null
        if (death != null) runCatching { binder.unlinkToDeath(death, 0) }
    }

    override fun leaveApplication() = session.leaveApplication()

    override fun stopApplication(sessionId: String?) = session.stopApplication(sessionId)

    override fun requestStatus() = session.requestStatus()

    override fun setVolume(level: Double, expectedLevel: Double, expectedMute: Boolean) = session.setVolume(level)

    override fun setMute(mute: Boolean, expectedLevel: Double, expectedMute: Boolean) = session.setMute(mute)

    override fun sendMessage(namespace: String, message: String, requestId: Long) = session.sendMessage(namespace, message, requestId)

    override fun sendBinaryMessage(namespace: String, message: ByteArray, requestId: Long) = session.sendBinaryMessage(namespace, message, requestId)

    override fun registerNamespace(namespace: String) = session.registerNamespace(namespace)

    override fun unregisterNamespace(namespace: String) = session.unregisterNamespace(namespace)

    override fun launchApplication(applicationId: String, launchOptions: LaunchOptions?) =
        session.launchApplication(
            applicationId,
            launchOptions?.relaunchIfRunning ?: false,
            launchOptions?.language,
            launchOptions?.androidReceiverCompatible ?: false,
            launchOptions?.credentialsData?.credentials,
            launchOptions?.credentialsData?.credentialsType,
        )

    override fun joinApplication(applicationId: String?, sessionId: String?, joinOptions: JoinOptions?) =
        session.joinApplication(applicationId, sessionId, joinOptions?.connectionType ?: 0)

    // Session events, called on the session thread of the current session

    private inner class SessionCallbacks : CastDeviceSession.Callbacks {
        lateinit var owner: CastDeviceSession

        private inline fun ifCurrent(block: () -> Unit) {
            if (owner === session && !released) block()
        }

        override fun onConnected() = onConnectResult(owner, CastDeviceSession.STATUS_SUCCESS)
        override fun onConnectionFailed(statusCode: Int) = onConnectResult(owner, statusCode)
        override fun onDisconnected(statusCode: Int) = onSessionDisconnected(owner, statusCode)
        override fun onDeviceStatusChanged(status: ReceiverStatus) = ifCurrent { deviceStatusChanged(status) }
        override fun onApplicationConnected(application: ReceiverApplication, wasLaunched: Boolean) =
            ifCurrent { applicationConnected(application, wasLaunched) }
        override fun onApplicationConnectionFailed(statusCode: Int) = ifCurrent { applicationConnectionFailed(statusCode) }
        override fun onApplicationStatusChanged(statusText: String?) =
            ifCurrent { notify { onApplicationStatusChanged(ApplicationStatus(statusText)) } }
        override fun onApplicationDisconnected(statusCode: Int) = ifCurrent {
            attachedApplicationId = null
            attachedSessionId = null
            notify { onApplicationDisconnected(statusCode) }
        }
        override fun onStopApplicationResult(statusCode: Int) = ifCurrent { notify { onStopApplicationResult(statusCode) } }
        override fun onLeaveApplicationResult(statusCode: Int) = ifCurrent {
            if (statusCode == CastDeviceSession.STATUS_SUCCESS) {
                attachedApplicationId = null
                attachedSessionId = null
            }
            notify { onLeaveApplicationResult(statusCode) }
        }
        override fun onTextMessage(namespace: String, message: String) = ifCurrent { notify { onTextMessageReceived(namespace, message) } }
        override fun onBinaryMessage(namespace: String, data: ByteArray) = ifCurrent { notify { onBinaryMessageReceived(namespace, data) } }
        override fun onSendMessageSuccess(namespace: String, requestId: Long) = ifCurrent { notify { onSendMessageSuccess(namespace, requestId) } }
        override fun onSendMessageFailure(namespace: String, requestId: Long, statusCode: Int) =
            ifCurrent { notify { onSendMessageFailure(namespace, requestId, statusCode) } }
    }

    private fun onConnectResult(owner: CastDeviceSession, statusCode: Int) {
        Log.d(TAG, "Connection to ${castDevice.friendlyName}: $statusCode")
        if (statusCode == CastDeviceSession.STATUS_SUCCESS) {
            CastChannelRegistry.register(castDevice.deviceId, owner)
            // disconnect() may have run during the connect (client died, service destroyed) and unregistered first.
            if (released || owner !== session) CastChannelRegistry.unregister(castDevice.deviceId, owner)
        }
        if (owner !== session || released) return
        if (statusCode != CastDeviceSession.STATUS_SUCCESS) {
            if (reconnecting && !released) {
                retryReconnect(owner)
                return
            }
            finishInit(statusCode)
            if (connectRequested) {
                connectRequested = false
                notify { onConnectedWithResult(statusCode) }
            }
            return
        }
        val wasReconnecting = reconnecting
        reconnecting = false
        reconnectAttempts = 0
        val shouldRejoinAttached = wasReconnecting && attachedApplicationId != null
        if (statusCode == CastDeviceSession.STATUS_SUCCESS && !shouldRejoinAttached && initCallback != null && lastApplicationId != null) {
            // An older client reconnecting after a connection loss continues with the application it was attached to.
            // The init completes once the join result arrives.
            rejoining = true
            owner.joinApplication(lastApplicationId, lastSessionId)
        } else {
            finishInit(statusCode)
        }
        if (connectRequested || wasReconnecting) {
            connectRequested = false
            notify { onConnectedWithResult(statusCode) }
        }
        if (shouldRejoinAttached) {
            rejoining = true
            owner.joinApplication(attachedApplicationId, attachedSessionId)
        }
    }

    private fun finishInit(statusCode: Int) {
        initCallback?.let {
            initCallback = null
            it(statusCode)
        }
    }

    private fun onSessionDisconnected(owner: CastDeviceSession, statusCode: Int) {
        CastChannelRegistry.unregister(castDevice.deviceId, owner)
        if (owner !== session) return
        if (released) {
            notify { onDisconnected(statusCode) }
            return
        }
        // Play services suspends a dropped channel and reconnects instead of ending the session.
        reconnecting = true
        notifySuspend()
        retryReconnect(owner)
    }

    private fun retryReconnect(owner: CastDeviceSession) {
        if (released || owner !== session) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Giving up reconnect to ${castDevice.friendlyName} after $reconnectAttempts attempts")
            reconnecting = false
            notify { onDisconnected(CastDeviceSession.STATUS_NETWORK_ERROR) }
            if (isLegacyClient) disconnect()
            return
        }
        reconnectAttempts++
        Log.d(TAG, "Reconnecting to ${castDevice.friendlyName} (attempt $reconnectAttempts)")
        session.connect()
    }

    /**
     * Temporary loss: do not tear the controller down if the client stub has no
     * [ICastDeviceControllerListener.onConnectionSuspended] transaction.
     */
    private fun notifySuspend() {
        val listener = listener ?: return
        try {
            listener.onConnectionSuspended(GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST)
        } catch (e: RemoteException) {
            Log.d(TAG, "Client has no onConnectionSuspended; reconnecting without it")
        } catch (e: RuntimeException) {
            Log.w(TAG, "onConnectionSuspended failed", e)
        }
    }

    private fun deviceStatusChanged(status: ReceiverStatus) {
        val app = status.applications.firstOrNull()
        notify { onDeviceStatusChanged(CastDeviceStatus(status.volumeLevel, status.muted, status.activeInput, app?.toMetadata(), status.standby)) }
    }

    private fun applicationConnected(application: ReceiverApplication, wasLaunched: Boolean) {
        attachedApplicationId = application.appId
        attachedSessionId = application.sessionId
        notify { onApplicationConnectionSuccess(application.toMetadata(), application.statusText, application.sessionId, wasLaunched) }
        if (rejoining) {
            rejoining = false
            finishInit(CastDeviceSession.STATUS_SUCCESS)
        }
    }

    private fun applicationConnectionFailed(statusCode: Int) {
        if (rejoining) {
            rejoining = false
            if (initCallback != null) {
                // Legacy initialization reports a missing previous application through the
                // service-init status rather than a listener callback.
                finishInit(
                    if (statusCode == CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING) STATUS_APP_NO_LONGER_RUNNING
                    else CastDeviceSession.STATUS_NETWORK_ERROR
                )
            } else {
                // Connectionless reconnect already reported the device connection. A failed
                // automatic rejoin is an application failure, not a second init result. Drop
                // the stale target so another channel reconnect does not keep retrying it.
                attachedApplicationId = null
                attachedSessionId = null
                notify { onApplicationConnectionFailure(statusCode) }
            }
            return
        }
        notify { onApplicationConnectionFailure(statusCode) }
    }

    private fun notify(call: ICastDeviceControllerListener.() -> Unit) {
        val listener = listener ?: return
        try {
            listener.call()
        } catch (e: RemoteException) {
            Log.w(TAG, "Client listener died, disconnecting", e)
            this.listener = null
            disconnect()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Client listener failed", e)
        }
    }

    private fun ReceiverApplication.toMetadata() = ApplicationMetadata().also {
        it.applicationId = appId
        it.name = displayName
        it.namespaces = ArrayList(namespaces)
        it.images = if (iconUrl != null) arrayListOf(WebImage(Uri.parse(iconUrl))) else arrayListOf()
    }

    companion object {
        /** Init status for an older client: connected, but its previous application is not running anymore. */
        const val STATUS_APP_NO_LONGER_RUNNING = 2300

        const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
