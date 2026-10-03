/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast.channel

import org.json.JSONException
import org.json.JSONObject
import org.microg.gms.cast.proto.CastMessage
import java.io.IOException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/** An application running on the receiver, as reported in RECEIVER_STATUS. */
data class ReceiverApplication(
    val appId: String,
    val displayName: String?,
    val sessionId: String,
    val transportId: String,
    val statusText: String?,
    val iconUrl: String?,
    val namespaces: List<String>,
)

data class ReceiverStatus(
    val volumeLevel: Double,
    val muted: Boolean,
    val stepInterval: Double,
    /** 1 if the receiver is the active input, 0 if not, -1 if unknown. */
    val activeInput: Int,
    /** 1 if the receiver is in standby, 0 if not, -1 if unknown. */
    val standby: Int,
    val applications: List<ReceiverApplication>,
    /** False if the status carried no volume level, so [volumeLevel] is not the device volume. */
    val hasVolumeLevel: Boolean = true,
)

/**
 * Sender-side state for one receiver: the control channel, the receiver namespace (status, launch, join, stop, volume)
 * and the virtual connection to the application the client is attached to.
 *
 * All methods return immediately; work and [Callbacks] run on one session thread, in call order.
 */
class CastDeviceSession(
    private val host: String,
    private val port: Int,
    private val callbacks: Callbacks,
) : CastChannel.Listener {

    interface Callbacks {
        fun onConnected()
        fun onConnectionFailed(statusCode: Int)
        fun onDisconnected(statusCode: Int)
        fun onDeviceStatusChanged(status: ReceiverStatus)
        fun onApplicationConnected(application: ReceiverApplication, wasLaunched: Boolean)
        fun onApplicationConnectionFailed(statusCode: Int)
        fun onApplicationStatusChanged(statusText: String?)
        fun onApplicationDisconnected(statusCode: Int)
        fun onStopApplicationResult(statusCode: Int)
        fun onLeaveApplicationResult(statusCode: Int)
        fun onTextMessage(namespace: String, message: String)
        fun onBinaryMessage(namespace: String, data: ByteArray)
        fun onSendMessageSuccess(namespace: String, requestId: Long)
        fun onSendMessageFailure(namespace: String, requestId: Long, statusCode: Int)
    }

    private class PendingRequest(val onReply: (JSONObject?) -> Unit) {
        var timeout: ScheduledFuture<*>? = null
    }

    private val executor = ScheduledThreadPoolExecutor(1) { Thread(it, "CastDeviceSession-$host").apply { isDaemon = true } }.apply {
        executeExistingDelayedTasksAfterShutdownPolicy = false
    }
    private var channel: CastChannel? = null
    private var nextRequestId = 1L
    private val pendingRequests = HashMap<Long, PendingRequest>()
    private val namespaces = LinkedHashSet<String>()
    private var receiverStatus: ReceiverStatus? = null
    private var application: ReceiverApplication? = null
    private var disconnectRequested = false
    private var stoppingSessionId: String? = null

    fun connect() = post {
        if (disconnectRequested) {
            // Queued before disconnect() shut the session down; a channel opened now would never be closed
            callbacks.onConnectionFailed(STATUS_NETWORK_ERROR)
            return@post
        }
        if (channel != null) {
            if (channel?.isConnected == true) callbacks.onConnected() else callbacks.onConnectionFailed(STATUS_NETWORK_ERROR)
            return@post
        }
        val newChannel = CastChannel(host, port, this)
        channel = newChannel
        try {
            newChannel.connect()
        } catch (e: IOException) {
            Log.log(Level.WARNING, "Connecting to $host:$port failed", e)
            channel = null
            callbacks.onConnectionFailed(if (e is DeviceAuthException) STATUS_AUTHENTICATION_FAILED else STATUS_NETWORK_ERROR)
            return@post
        }
        callbacks.onConnected()
        requestReceiver(JSONObject().put("type", "GET_STATUS")) { }
    }

    fun disconnect() = post {
        disconnectRequested = true
        application = null
        channel?.close()
        channel = null
        executor.shutdown()
    }

    fun requestStatus() = post {
        requestReceiver(JSONObject().put("type", "GET_STATUS")) { }
    }

    fun launchApplication(appId: String, relaunchIfRunning: Boolean, language: String?) = post {
        if (relaunchIfRunning) {
            sendLaunch(appId, language)
            return@post
        }
        // Decide on the current status: the one requested on connect may not have arrived yet
        requestReceiver(JSONObject().put("type", "GET_STATUS")) { reply ->
            if (reply == null) {
                callbacks.onApplicationConnectionFailed(STATUS_TIMEOUT)
                return@requestReceiver
            }
            val running = receiverStatus?.applications?.firstOrNull { it.appId == appId }
            if (running != null) attachApplication(running, false)
            else sendLaunch(appId, language)
        }
    }

    private fun sendLaunch(appId: String, language: String?) {
        val request = JSONObject().put("type", "LAUNCH").put("appId", appId)
        if (!language.isNullOrEmpty()) request.put("language", language)
        requestReceiver(request, LAUNCH_TIMEOUT_MILLIS) { reply ->
            when (reply?.optString("type")) {
                null -> callbacks.onApplicationConnectionFailed(STATUS_TIMEOUT)
                "RECEIVER_STATUS" -> {
                    val launched = receiverStatus?.applications?.firstOrNull { it.appId == appId }
                    if (launched != null) attachApplication(launched, true)
                    else callbacks.onApplicationConnectionFailed(STATUS_APPLICATION_NOT_RUNNING)
                }

                "LAUNCH_ERROR" -> callbacks.onApplicationConnectionFailed(launchErrorStatus(reply.optString("reason")))
                else -> callbacks.onApplicationConnectionFailed(STATUS_INVALID_REQUEST)
            }
        }
    }

    /** Attach to a running application. A null [appId] or [sessionId] matches any. */
    fun joinApplication(appId: String?, sessionId: String?) = post {
        requestReceiver(JSONObject().put("type", "GET_STATUS")) { reply ->
            if (reply == null) {
                callbacks.onApplicationConnectionFailed(STATUS_TIMEOUT)
                return@requestReceiver
            }
            val running = receiverStatus?.applications?.firstOrNull {
                (appId.isNullOrEmpty() || it.appId == appId) && (sessionId.isNullOrEmpty() || it.sessionId == sessionId)
            }
            if (running != null) attachApplication(running, false)
            else callbacks.onApplicationConnectionFailed(STATUS_APPLICATION_NOT_RUNNING)
        }
    }

    fun leaveApplication() = post {
        val app = application
        if (app == null) {
            callbacks.onLeaveApplicationResult(STATUS_INVALID_REQUEST)
            return@post
        }
        application = null
        try {
            channel?.closeTransport(app.transportId)
            callbacks.onLeaveApplicationResult(STATUS_SUCCESS)
        } catch (e: IOException) {
            callbacks.onLeaveApplicationResult(STATUS_NETWORK_ERROR)
        }
    }

    fun stopApplication(sessionId: String?) = post {
        val target = sessionId?.takeIf { it.isNotEmpty() } ?: application?.sessionId
        if (target == null) {
            callbacks.onStopApplicationResult(STATUS_INVALID_REQUEST)
            return@post
        }
        stoppingSessionId = target
        requestReceiver(JSONObject().put("type", "STOP").put("sessionId", target)) { reply ->
            stoppingSessionId = null
            when (reply?.optString("type")) {
                null -> callbacks.onStopApplicationResult(STATUS_TIMEOUT)
                "RECEIVER_STATUS" -> callbacks.onStopApplicationResult(STATUS_SUCCESS)
                else -> callbacks.onStopApplicationResult(STATUS_INVALID_REQUEST)
            }
        }
    }

    fun setVolume(level: Double) = post {
        requestReceiver(JSONObject().put("type", "SET_VOLUME").put("volume", JSONObject().put("level", level.coerceIn(0.0, 1.0)))) { }
    }

    fun setMute(muted: Boolean) = post {
        requestReceiver(JSONObject().put("type", "SET_VOLUME").put("volume", JSONObject().put("muted", muted))) { }
    }

    fun registerNamespace(namespace: String) = post { namespaces.add(namespace) }

    fun unregisterNamespace(namespace: String) = post { namespaces.remove(namespace) }

    fun sendMessage(namespace: String, message: String, requestId: Long) = post {
        sendToApplication(namespace, requestId) { channel, transportId -> channel.send(transportId, namespace, message) }
    }

    fun sendBinaryMessage(namespace: String, data: ByteArray, requestId: Long) = post {
        sendToApplication(namespace, requestId) { channel, transportId -> channel.send(transportId, namespace, data) }
    }

    private fun sendToApplication(namespace: String, requestId: Long, send: (CastChannel, String) -> Unit) {
        val channel = channel
        val app = application
        when {
            isPlatformNamespace(namespace) -> callbacks.onSendMessageFailure(namespace, requestId, STATUS_INVALID_REQUEST)
            channel == null || app == null -> callbacks.onSendMessageFailure(namespace, requestId, STATUS_APPLICATION_NOT_RUNNING)
            else -> try {
                send(channel, app.transportId)
                callbacks.onSendMessageSuccess(namespace, requestId)
            } catch (e: MessageTooLargeException) {
                callbacks.onSendMessageFailure(namespace, requestId, STATUS_MESSAGE_TOO_LARGE)
            } catch (e: IOException) {
                callbacks.onSendMessageFailure(namespace, requestId, STATUS_NETWORK_ERROR)
            }
        }
    }

    private fun attachApplication(app: ReceiverApplication, wasLaunched: Boolean) {
        try {
            channel?.connectTransport(app.transportId) ?: throw IOException("Not connected")
        } catch (e: IOException) {
            callbacks.onApplicationConnectionFailed(STATUS_NETWORK_ERROR)
            return
        }
        application = app
        callbacks.onApplicationConnected(app, wasLaunched)
    }

    private fun requestReceiver(request: JSONObject, timeoutMillis: Long = REQUEST_TIMEOUT_MILLIS, onReply: (JSONObject?) -> Unit) {
        val channel = channel
        if (channel == null) {
            onReply(null)
            return
        }
        val requestId = nextRequestId++
        val pending = PendingRequest(onReply)
        pendingRequests[requestId] = pending
        try {
            channel.send(RECEIVER_ID, NAMESPACE_RECEIVER, request.put("requestId", requestId).toString())
        } catch (e: IOException) {
            pendingRequests.remove(requestId)
            onReply(null)
            return
        }
        pending.timeout = executor.schedule({
            if (pendingRequests.remove(requestId) != null) onReply(null)
        }, timeoutMillis, TimeUnit.MILLISECONDS)
    }

    private fun handleReceiverMessage(json: JSONObject) {
        if (json.optString("type") != "RECEIVER_STATUS") return
        val statusJson = json.optJSONObject("status") ?: return
        val status = parseReceiverStatus(statusJson)
        receiverStatus = status
        val current = application
        if (current != null) {
            val updated = status.applications.firstOrNull { it.sessionId == current.sessionId }
            if (updated == null) {
                application = null
                runCatching { channel?.closeTransport(current.transportId) }
                callbacks.onApplicationDisconnected(if (current.sessionId == stoppingSessionId) STATUS_SUCCESS else STATUS_APPLICATION_NOT_RUNNING)
            } else {
                application = updated
                if (updated.statusText != current.statusText) callbacks.onApplicationStatusChanged(updated.statusText)
            }
        }
        callbacks.onDeviceStatusChanged(status)
    }

    // Called on the channel reader thread
    override fun onMessage(message: CastMessage) = post {
        if (message.namespace == NAMESPACE_RECEIVER && message.source_id == RECEIVER_ID) {
            val json = try {
                JSONObject(message.payload_utf8 ?: return@post)
            } catch (e: JSONException) {
                Log.warning("Invalid receiver message from $host")
                return@post
            }
            handleReceiverMessage(json)
            val requestId = json.optLong("requestId", 0)
            val pending = if (requestId != 0L) pendingRequests.remove(requestId) else null
            if (pending != null) {
                pending.timeout?.cancel(false)
                pending.onReply(json)
            }
            return@post
        }
        val app = application ?: return@post
        if (message.source_id != app.transportId) return@post
        if (message.destination_id != channel?.senderId && message.destination_id != BROADCAST_ID) return@post
        // Like Play services, only deliver namespaces the client registered a callback for
        if (message.namespace !in namespaces) return@post
        when (message.payload_type) {
            CastMessage.PayloadType.STRING -> callbacks.onTextMessage(message.namespace, message.payload_utf8 ?: "")
            CastMessage.PayloadType.BINARY -> callbacks.onBinaryMessage(message.namespace, message.payload_binary?.toByteArray() ?: ByteArray(0))
        }
    }

    override fun onTransportClosed(transportId: String) = post {
        val current = application
        if (current?.transportId == transportId) {
            application = null
            callbacks.onApplicationDisconnected(if (current.sessionId == stoppingSessionId) STATUS_SUCCESS else STATUS_APPLICATION_NOT_RUNNING)
        }
    }

    override fun onClosed(error: IOException?) = post {
        if (error != null) Log.log(Level.INFO, "Connection to $host closed", error)
        channel = null
        application = null
        receiverStatus = null
        for (pending in pendingRequests.values) {
            pending.timeout?.cancel(false)
            pending.onReply(null)
        }
        pendingRequests.clear()
        if (!disconnectRequested) callbacks.onDisconnected(if (error == null) STATUS_SUCCESS else STATUS_NETWORK_ERROR)
    }

    private fun post(block: () -> Unit) {
        if (executor.isShutdown) return
        try {
            executor.execute {
                try {
                    block()
                } catch (e: RuntimeException) {
                    Log.log(Level.WARNING, "Cast session task failed", e)
                }
            }
        } catch (e: RejectedExecutionException) {
            // session already disconnected
        }
    }

    companion object {
        private val Log = Logger.getLogger("GmsCastSession")

        // Values of CastStatusCodes / CommonStatusCodes
        const val STATUS_SUCCESS = 0
        const val STATUS_NETWORK_ERROR = 7
        const val STATUS_TIMEOUT = 15
        const val STATUS_AUTHENTICATION_FAILED = 2000
        const val STATUS_INVALID_REQUEST = 2001
        const val STATUS_CANCELED = 2002
        const val STATUS_NOT_ALLOWED = 2003
        const val STATUS_APPLICATION_NOT_FOUND = 2004
        const val STATUS_APPLICATION_NOT_RUNNING = 2005
        const val STATUS_MESSAGE_TOO_LARGE = 2006
        const val STATUS_FAILED = 2100

        const val REQUEST_TIMEOUT_MILLIS = 10_000L
        const val LAUNCH_TIMEOUT_MILLIS = 30_000L

        fun isPlatformNamespace(namespace: String) =
            namespace.startsWith("urn:x-cast:com.google.cast.tp.") || namespace == NAMESPACE_RECEIVER

        fun launchErrorStatus(reason: String?) = when (reason) {
            "NOT_FOUND" -> STATUS_APPLICATION_NOT_FOUND
            "CANCELLED" -> STATUS_CANCELED
            "NOT_ALLOWED" -> STATUS_NOT_ALLOWED
            "TIMEOUT" -> STATUS_TIMEOUT
            else -> STATUS_FAILED
        }

        fun parseReceiverStatus(status: JSONObject): ReceiverStatus {
            val volume = status.optJSONObject("volume")
            val applications = ArrayList<ReceiverApplication>()
            val apps = status.optJSONArray("applications")
            if (apps != null) for (i in 0 until apps.length()) {
                val app = apps.optJSONObject(i) ?: continue
                val sessionId = app.optString("sessionId").takeIf { it.isNotEmpty() } ?: continue
                val namespaces = ArrayList<String>()
                val namespacesJson = app.optJSONArray("namespaces")
                if (namespacesJson != null) for (j in 0 until namespacesJson.length()) {
                    namespacesJson.optJSONObject(j)?.optString("name")?.takeIf { it.isNotEmpty() }?.let { namespaces.add(it) }
                }
                applications.add(
                    ReceiverApplication(
                        appId = app.optString("appId"),
                        displayName = app.optString("displayName").takeIf { it.isNotEmpty() },
                        sessionId = sessionId,
                        transportId = app.optString("transportId").takeIf { it.isNotEmpty() } ?: sessionId,
                        statusText = app.optString("statusText").takeIf { it.isNotEmpty() },
                        iconUrl = app.optString("iconUrl").takeIf { it.isNotEmpty() },
                        namespaces = namespaces,
                    )
                )
            }
            return ReceiverStatus(
                volumeLevel = volume?.optDouble("level", 0.0) ?: 0.0,
                muted = volume?.optBoolean("muted", false) ?: false,
                stepInterval = volume?.optDouble("stepInterval", 0.05) ?: 0.05,
                activeInput = if (status.has("isActiveInput")) (if (status.optBoolean("isActiveInput")) 1 else 0) else -1,
                standby = if (status.has("isStandBy")) (if (status.optBoolean("isStandBy")) 1 else 0) else -1,
                applications = applications,
                hasVolumeLevel = volume?.has("level") == true,
            )
        }
    }
}
