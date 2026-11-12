/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.content.Context
import android.net.nsd.NsdManager
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.internal.IConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.internal.IConnectionsResultCallback
import com.google.android.gms.nearby.connection.internal.IConnectionsService
import com.google.android.gms.nearby.connection.internal.IEndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.internal.IPayloadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service side of the Nearby Connections API, hosted by
 * NearbySharingService. Owns the engine: a [NearbySharingServer] for
 * advertising, a [NearbySharingScanner] for discovery, and one
 * [NearbyConnectionsSession] / [NearbyConnectionsServerSession] per
 * endpoint connection. Every AIDL method returns immediately; outcomes
 * are reported through the result callback and app events through the
 * lifecycle/discovery/payload callbacks.
 *
 * Documented deviations from the real Google API, all forced by the
 * engine this service is built on:
 * - The transport is the Nearby Share mDNS/TCP wire protocol, so the
 *   requested serviceId is echoed in DiscoveredEndpointInfo but does not
 *   filter mDNS results, and every strategy behaves as point-to-point.
 * - requestConnection's Task completes when the request is on the wire
 *   (handshake done); acceptance/rejection arrive through
 *   onConnectionResult, as in the real API.
 * - stopAllEndpoints also stops advertising and discovery (full cleanup).
 * - Incoming FILE/STREAM payloads are delivered as read-only file
 *   descriptors to cache files named payload_<id>; the Connections
 *   protocol carries no file name.
 */
class ConnectionsServiceImpl(context: Context) : IConnectionsService.Stub() {
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val myEndpointId: String = generateEndpointId()

    private val advertisingLock = Any()
    private var server: NearbySharingServer? = null
    private var advertisingLifecycle: IConnectionLifecycleCallback? = null
    private var advertisingResult: IConnectionsResultCallback? = null
    private var advertisingStarted = false

    @Volatile
    private var scanner: NearbySharingScanner? = null
    @Volatile
    private var discoveryCallback: IEndpointDiscoveryCallback? = null
    @Volatile
    private var discoveryServiceId: String? = null
    private val discovered = ConcurrentHashMap<String, NearbyDevice>()

    private val outgoing = ConcurrentHashMap<String, NearbyConnectionsSession>()
    private val incoming = ConcurrentHashMap<String, NearbyConnectionsServerSession>()
    private val lifecycleCallbacks = ConcurrentHashMap<String, IConnectionLifecycleCallback>()
    private val payloadCallbacks = ConcurrentHashMap<String, IPayloadCallback>()
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()
    private val inFlightPayloads = ConcurrentHashMap<Long, String>()

    private class PendingRequest(val result: IConnectionsResultCallback) {
        val sent = AtomicBoolean(false)
    }

    private val incomingListener = object : NearbyConnectionsServerSession.Listener {
        override fun onConnectionInitiated(endpointId: String, remoteName: String, pinCode: String, endpointInfo: ByteArray?) {
            val lifecycle = synchronized(advertisingLock) { advertisingLifecycle }
            if (lifecycle != null) {
                lifecycleCallbacks[endpointId] = lifecycle
                safeCall { lifecycle.onConnectionInitiated(endpointId, remoteName, pinCode, endpointInfo, true) }
            }
        }

        override fun onConnectionResult(endpointId: String, statusCode: Int) {
            lifecycleCallbacks[endpointId]?.let { safeCall { it.onConnectionResult(endpointId, statusCode) } }
        }

        override fun onPayloadReceived(endpointId: String, payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?) {
            val cb = payloadCallbacks[endpointId]
            if (cb == null) {
                Log.w(NearbySharingConstants.TAG, "Dropping payload $payloadId for $endpointId: no payload callback registered")
                return
            }
            safeCall { cb.onPayloadReceived(endpointId, payloadId, type, bytes, pfd) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long) {
            payloadCallbacks[endpointId]?.let { cb ->
                safeCall { cb.onPayloadTransferUpdate(endpointId, payloadId, status, bytesTransferred, totalBytes) }
            }
        }

        override fun onDisconnected(endpointId: String) {
            lifecycleCallbacks[endpointId]?.let { safeCall { it.onDisconnected(endpointId) } }
            cleanupEndpoint(endpointId)
        }

        override fun onError(endpointId: String, error: Throwable) {
            Log.w(NearbySharingConstants.TAG, "Incoming session $endpointId error", error)
            if (lifecycleCallbacks.containsKey(endpointId)) {
                lifecycleCallbacks[endpointId]?.let { safeCall { it.onDisconnected(endpointId) } }
            }
            cleanupEndpoint(endpointId)
        }
    }

    override fun startAdvertising(userNickName: String?, serviceId: String?, lifecycle: IConnectionLifecycleCallback?, result: IConnectionsResultCallback?) {
        var toStart: NearbySharingServer? = null
        var already = false
        synchronized(advertisingLock) {
            if (server != null) {
                already = true
            } else {
                advertisingLifecycle = lifecycle
                advertisingResult = result
                advertisingStarted = false
                val deviceName = userNickName ?: "Android Device"
                val created = NearbySharingServer(appContext, deviceName, NearbyDeviceType.PHONE, object : NearbySharingServer.Listener {
                override fun onServerStarted(port: Int) {
                    // Capture under lock, fire outside: the result callback
                    // is a Binder call into the app, which must never run
                    // while holding advertisingLock (re-entrant service
                    // calls from the app would deadlock).
                    val toFire = synchronized(advertisingLock) {
                        advertisingStarted = true
                        advertisingResult.also { advertisingResult = null }
                    }
                    toFire?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
                }

                override fun onConnectionInitiated(remoteDeviceName: String, pinCode: String) {
                    // Unused in Connections mode: accepted sockets are
                    // routed to connectionsHandler, not handleClient.
                }

                override fun onTransferStarted(files: List<com.google.android.gms.nearby.sharing.FileMetadata>) {}
                override fun onTransferProgress(bytesTransferred: Long, totalBytes: Long) {}
                override fun onTransferCompleted(savedFiles: List<java.io.File>) {}
                override fun onError(error: Throwable) {
                    val toFire = synchronized(advertisingLock) {
                        if (!advertisingStarted) {
                            advertisingResult.also { advertisingResult = null }
                        } else {
                            null
                        }
                    }
                    toFire?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR) }
                    Log.w(NearbySharingConstants.TAG, "Advertising server error", error)
                }
            })
            created.connectionsHandler = { socket ->
                val session = NearbyConnectionsServerSession(appContext, socket, incomingListener)
                incoming[session.endpointId] = session
                session.start()
            }
            server = created
            toStart = created
        }
        if (already) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) }
            return
        }
        // Start outside the lock: a synchronous mDNS error fires the
        // result callback, which must never run under advertisingLock.
        toStart?.start()
    }

    override fun stopAdvertising(result: IConnectionsResultCallback?) {
        synchronized(advertisingLock) {
            server?.stop()
            server = null
            advertisingLifecycle = null
            advertisingResult = null
            advertisingStarted = false
        }
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
    }

    override fun startDiscovery(serviceId: String?, discovery: IEndpointDiscoveryCallback?, result: IConnectionsResultCallback?) {
        if (scanner != null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) }
            return
        }
        if (appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager == null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_NETWORK_NOT_CONNECTED) }
            return
        }
        discoveryCallback = discovery
        discoveryServiceId = serviceId
        val newScanner = NearbySharingScanner(appContext,
            onDeviceFound = { device ->
                discovered[device.id] = device
                val cb = discoveryCallback
                val sid = discoveryServiceId
                if (cb != null && sid != null) {
                    safeCall { cb.onEndpointFound(device.id, sid, device.name, device.endpointInfo) }
                }
            },
            onDeviceLost = { device ->
                discovered.remove(device.id)
                discoveryCallback?.let { cb -> safeCall { cb.onEndpointLost(device.id) } }
            })
        scanner = newScanner
        newScanner.startDiscovery()
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
    }

    override fun stopDiscovery(result: IConnectionsResultCallback?) {
        scanner?.stopDiscovery()
        scanner = null
        discoveryCallback = null
        discoveryServiceId = null
        discovered.clear()
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
    }

    override fun requestConnection(userNickName: String?, endpointId: String?, endpointInfo: ByteArray?, lifecycle: IConnectionLifecycleCallback?, result: IConnectionsResultCallback?) {
        if (endpointId == null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR) }
            return
        }
        val device = discovered[endpointId]
        if (device == null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR) }
            return
        }
        if (outgoing.containsKey(endpointId)) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT) }
            return
        }
        val request = PendingRequest(result ?: return)
        pendingRequests[endpointId] = request
        if (lifecycle != null) lifecycleCallbacks[endpointId] = lifecycle
        val session = NearbyConnectionsSession(appContext, userNickName ?: "Android Device", myEndpointId, device, object : NearbyConnectionsSession.Listener {
            override fun onRequestSent() {
                if (request.sent.compareAndSet(false, true)) {
                    fireResult(request.result, ConnectionsStatusCodes.STATUS_OK)
                }
            }

            override fun onConnectionInitiated(pinCode: String) {
                lifecycle?.let {
                    safeCall {
                        it.onConnectionInitiated(endpointId, device.name, pinCode, device.endpointInfo, false)
                    }
                }
            }

            override fun onConnectionResult(statusCode: Int) {
                lifecycle?.let { safeCall { it.onConnectionResult(endpointId, statusCode) } }
            }

            override fun onPayloadReceived(payloadId: Long, type: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?) {
                val cb = payloadCallbacks[endpointId]
                if (cb == null) {
                    Log.w(NearbySharingConstants.TAG, "Dropping payload $payloadId for $endpointId: no payload callback registered")
                    return
                }
                safeCall { cb.onPayloadReceived(endpointId, payloadId, type, bytes, pfd) }
            }

            override fun onPayloadTransferUpdate(payloadId: Long, status: Int, bytesTransferred: Long, totalBytes: Long) {
                payloadCallbacks[endpointId]?.let { cb ->
                    safeCall { cb.onPayloadTransferUpdate(endpointId, payloadId, status, bytesTransferred, totalBytes) }
                }
            }

            override fun onDisconnected() {
                lifecycle?.let { safeCall { it.onDisconnected(endpointId) } }
                cleanupEndpoint(endpointId)
            }

            override fun onError(error: Throwable) {
                if (!request.sent.get()) {
                    if (request.sent.compareAndSet(false, true)) {
                        fireResult(request.result, ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR)
                    }
                    cleanupEndpoint(endpointId)
                } else {
                    Log.w(NearbySharingConstants.TAG, "Outgoing session $endpointId error", error)
                    lifecycle?.let { safeCall { it.onDisconnected(endpointId) } }
                    cleanupEndpoint(endpointId)
                }
            }
        })
        outgoing[endpointId] = session
        session.start()
    }

    override fun acceptConnection(endpointId: String?, payload: IPayloadCallback?, result: IConnectionsResultCallback?) {
        if (endpointId == null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) }
            return
        }
        if (payload != null) payloadCallbacks[endpointId] = payload
        val inSession = incoming[endpointId]
        if (inSession != null) {
            inSession.accept()
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
            return
        }
        val outSession = outgoing[endpointId]
        if (outSession != null) {
            outSession.accept()
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
            return
        }
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) }
    }

    override fun rejectConnection(endpointId: String?, result: IConnectionsResultCallback?) {
        if (endpointId == null) {
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) }
            return
        }
        val inSession = incoming[endpointId]
        if (inSession != null) {
            inSession.reject()
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
            return
        }
        val outSession = outgoing[endpointId]
        if (outSession != null) {
            outSession.reject()
            result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
            return
        }
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL) }
    }

    override fun sendPayload(endpointId: String?, payloadId: Long, payloadType: Int, bytes: ByteArray?, pfd: ParcelFileDescriptor?, result: IConnectionsResultCallback?) {
        val cb = result ?: return
        if (endpointId == null) {
            fireResult(cb, ConnectionsStatusCodes.STATUS_NOT_CONNECTED_TO_ENDPOINT)
            return
        }
        val outSession = outgoing[endpointId]
        val inSession = incoming[endpointId]
        if (outSession == null && inSession == null) {
            fireResult(cb, ConnectionsStatusCodes.STATUS_NOT_CONNECTED_TO_ENDPOINT)
            return
        }
        val established = (outSession?.established == true) || (inSession?.established == true)
        if (!established) {
            fireResult(cb, ConnectionsStatusCodes.STATUS_NOT_CONNECTED_TO_ENDPOINT)
            return
        }
        if (payloadType == NearbyConnectionsChannel.PAYLOAD_TYPE_BYTES && bytes == null) {
            fireResult(cb, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR)
            return
        }
        if ((payloadType == NearbyConnectionsChannel.PAYLOAD_TYPE_FILE || payloadType == NearbyConnectionsChannel.PAYLOAD_TYPE_STREAM) && pfd == null) {
            fireResult(cb, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR)
            return
        }
        inFlightPayloads[payloadId] = endpointId
        scope.launch {
            try {
                val sent = when (payloadType) {
                    NearbyConnectionsChannel.PAYLOAD_TYPE_BYTES -> {
                        (outSession?.sendBytes(payloadId, bytes!!)
                            ?: inSession?.sendBytes(payloadId, bytes!!)) == true
                    }
                    NearbyConnectionsChannel.PAYLOAD_TYPE_FILE, NearbyConnectionsChannel.PAYLOAD_TYPE_STREAM -> {
                        val size = pfd!!.statSize
                        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
                        try {
                            (outSession?.sendStream(payloadId, payloadType, stream, if (size < 0) 0L else size)
                                ?: inSession?.sendStream(payloadId, payloadType, stream, if (size < 0) 0L else size)) == true
                        } finally {
                            try {
                                stream.close()
                            } catch (ignored: IOException) {
                            }
                            try {
                                pfd.close()
                            } catch (ignored: IOException) {
                            }
                        }
                    }
                    else -> {
                        fireResult(cb, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR)
                        return@launch
                    }
                }
                fireResult(cb, if (sent) ConnectionsStatusCodes.STATUS_OK else CommonStatusCodes.CANCELED)
            } catch (e: IOException) {
                Log.w(NearbySharingConstants.TAG, "sendPayload $payloadId failed", e)
                fireResult(cb, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR)
            } catch (e: Exception) {
                Log.w(NearbySharingConstants.TAG, "sendPayload $payloadId failed", e)
                fireResult(cb, ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR)
            } finally {
                inFlightPayloads.remove(payloadId)
            }
        }
    }

    override fun cancelPayload(payloadId: Long, result: IConnectionsResultCallback?) {
        val endpointId = inFlightPayloads[payloadId]
        if (endpointId != null) {
            outgoing[endpointId]?.cancelPayload(payloadId)
            incoming[endpointId]?.cancelPayload(payloadId)
        }
        // Idempotent: cancelling an unknown or already finished payload is
        // still a success; the transfer update (if any) reports CANCELED.
        result?.let { fireResult(it, ConnectionsStatusCodes.STATUS_OK) }
    }

    override fun disconnectFromEndpoint(endpointId: String?) {
        if (endpointId == null) return
        outgoing.remove(endpointId)?.close()
        incoming.remove(endpointId)?.close()
        lifecycleCallbacks.remove(endpointId)
        payloadCallbacks.remove(endpointId)
        pendingRequests.remove(endpointId)
    }

    override fun stopAllEndpoints() {
        synchronized(advertisingLock) {
            server?.stop()
            server = null
            advertisingLifecycle = null
            advertisingResult = null
            advertisingStarted = false
        }
        scanner?.stopDiscovery()
        scanner = null
        discoveryCallback = null
        discoveryServiceId = null
        discovered.clear()
        outgoing.values.forEach { it.close() }
        incoming.values.forEach { it.close() }
        outgoing.clear()
        incoming.clear()
        lifecycleCallbacks.clear()
        payloadCallbacks.clear()
        pendingRequests.clear()
        inFlightPayloads.clear()
    }

    fun shutdown() {
        stopAllEndpoints()
        scope.cancel()
    }

    private fun cleanupEndpoint(endpointId: String) {
        outgoing.remove(endpointId)
        incoming.remove(endpointId)
        lifecycleCallbacks.remove(endpointId)
        payloadCallbacks.remove(endpointId)
        pendingRequests.remove(endpointId)
    }

    private fun fireResult(result: IConnectionsResultCallback, statusCode: Int) {
        try {
            result.onResult(statusCode)
        } catch (e: RemoteException) {
            Log.w(NearbySharingConstants.TAG, "Failed to deliver result $statusCode", e)
        }
    }

    private inline fun safeCall(call: () -> Unit) {
        try {
            call()
        } catch (e: RemoteException) {
            Log.w(NearbySharingConstants.TAG, "Callback delivery failed", e)
        } catch (e: Exception) {
            Log.w(NearbySharingConstants.TAG, "Callback failed", e)
        }
    }

    private fun generateEndpointId(): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = SecureRandom()
        val sb = StringBuilder(4)
        for (i in 0 until 4) {
            sb.append(alphabet[random.nextInt(alphabet.length)])
        }
        return sb.toString()
    }
}
