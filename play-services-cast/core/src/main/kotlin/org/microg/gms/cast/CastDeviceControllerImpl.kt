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

    // Binder of the listener whose death disconnects this controller, guarded by this
    private var linkedBinder: IBinder? = null

    private val listenerDeath = IBinder.DeathRecipient {
        Log.d(TAG, "Client $packageName died, disconnecting from ${castDevice.friendlyName}")
        listener = null
        disconnect()
    }

    @Volatile
    private var initCallback: ((Int) -> Unit)? = null

    @Volatile
    private var connectRequested = false

    @Volatile
    private var rejoining = false

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
            session = newSession()
            true
        }
        if (reopened) onReopen(this)
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
        val current = synchronized(this) {
            released = true
            session
        }
        unlinkListener()
        CastChannelRegistry.unregister(castDevice.deviceId, current)
        current.disconnect()
        onRelease(this)
    }

    /** Set the client listener and disconnect once its process dies. Returns false if it is already dead. */
    @Synchronized
    private fun setListener(newListener: ICastDeviceControllerListener?): Boolean {
        val binder = newListener?.asBinder()
        if (binder !== linkedBinder) {
            unlinkListener()
            if (binder != null) {
                try {
                    binder.linkToDeath(listenerDeath, 0)
                } catch (e: RemoteException) {
                    listener = null
                    return false
                }
                linkedBinder = binder
            }
        }
        listener = newListener
        return true
    }

    @Synchronized
    private fun unlinkListener() {
        val binder = linkedBinder ?: return
        linkedBinder = null
        runCatching { binder.unlinkToDeath(listenerDeath, 0) }
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
        session.launchApplication(applicationId, launchOptions?.relaunchIfRunning ?: false, launchOptions?.language)

    override fun joinApplication(applicationId: String?, sessionId: String?, joinOptions: JoinOptions?) =
        session.joinApplication(applicationId, sessionId)

    // Session events, called on the session thread of the current session

    private inner class SessionCallbacks : CastDeviceSession.Callbacks {
        lateinit var owner: CastDeviceSession

        private inline fun ifCurrent(block: () -> Unit) {
            if (owner === session) block()
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
        override fun onApplicationDisconnected(statusCode: Int) = ifCurrent { notify { onApplicationDisconnected(statusCode) } }
        override fun onStopApplicationResult(statusCode: Int) = ifCurrent { notify { onStopApplicationResult(statusCode) } }
        override fun onLeaveApplicationResult(statusCode: Int) = ifCurrent { notify { onLeaveApplicationResult(statusCode) } }
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
        if (owner !== session) return
        if (statusCode == CastDeviceSession.STATUS_SUCCESS && initCallback != null && lastApplicationId != null) {
            // An older client reconnecting after a connection loss continues with the application it was attached to.
            // The init completes once the join result arrives.
            rejoining = true
            owner.joinApplication(lastApplicationId, lastSessionId)
        } else {
            finishInit(statusCode)
        }
        if (connectRequested) {
            connectRequested = false
            notify { onConnectedWithResult(statusCode) }
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
        notify { onDisconnected(statusCode) }
        // Older clients drop their listener on onDisconnected and reconnect through a new controller
        if (isLegacyClient) disconnect()
    }

    private fun deviceStatusChanged(status: ReceiverStatus) {
        val app = status.applications.firstOrNull()
        notify { onDeviceStatusChanged(CastDeviceStatus(status.volumeLevel, status.muted, status.activeInput, app?.toMetadata(), status.standby)) }
    }

    private fun applicationConnected(application: ReceiverApplication, wasLaunched: Boolean) {
        notify { onApplicationConnectionSuccess(application.toMetadata(), application.statusText, application.sessionId, wasLaunched) }
        if (rejoining) {
            rejoining = false
            finishInit(CastDeviceSession.STATUS_SUCCESS)
        }
    }

    private fun applicationConnectionFailed(statusCode: Int) {
        if (rejoining) {
            rejoining = false
            // Only a missing application is reported as a successful connection with Cast.EXTRA_APP_NO_LONGER_RUNNING;
            // a timeout or a dropped channel means the reconnect itself failed.
            finishInit(
                if (statusCode == CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING) STATUS_APP_NO_LONGER_RUNNING
                else CastDeviceSession.STATUS_NETWORK_ERROR
            )
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
    }
}
