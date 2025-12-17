/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.tunnel

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "TunnelWebsocket"

enum class SocketStatus {
    NONE, CONNECTING, CONNECTED, DISCONNECTED
}

interface TunnelWebCallback {
    fun disconnected()
    fun error(error: TunnelException)
    fun connected(response: Response)
    fun message(data: ByteArray)
}

data class TunnelException(val msg: String, val th: Throwable? = null) : RuntimeException(msg, th)

class TunnelWebsocket(val url: String, val callback: TunnelWebCallback) {
    private val threadPool = Executors.defaultThreadFactory()
    private var submitThread: Thread? = null

    @Volatile
    private var socketStatus = SocketStatus.NONE

    @Volatile
    private var socket: WebSocket? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build()
    }

    @Synchronized
    fun close() {
        Log.d(TAG, "close() with state= $socketStatus")
        val ordinal = socketStatus.ordinal
        if (ordinal == 0) {
            socketStatus = SocketStatus.DISCONNECTED
            return
        }
        closeWebsocket()
    }

    @Synchronized
    fun closeWebsocket() {
        if (socketStatus == SocketStatus.DISCONNECTED) {
            return
        }
        Log.d(TAG, "closeWebsocket: ")
        if (this.socket != null) {
            try {
                this.socket!!.close(1000, "Done")
            } catch (e: IOException) {
                throw TunnelException("Socket failed to close", e)
            }
        }
        this.socketStatus = SocketStatus.DISCONNECTED
        this.callback.disconnected()
    }

    @Synchronized
    fun connect() {
        Log.d(TAG, "connect() with state= $socketStatus")
        if (this.socketStatus != SocketStatus.NONE) {
            Log.d(TAG, "connect() has already been called")
            this.callback.error(TunnelException("connect() has already been called"))
            close()
            return
        }
        val threadNewThread = threadPool.newThread {
            Log.d(TAG, "runReader()")
            try {
                synchronized(this) {
                    if (socket != null && socketStatus == SocketStatus.DISCONNECTED) {
                        try {
                            Log.d(TAG, "runReader() called when websocket is disconnected")
                            close()
                        } catch (e: IOException) {
                            Log.w(TAG, "connect: Socket failed to close", e)
                        }
                    } else {
                        val request = Request.Builder().url(url).header("Sec-WebSocket-Protocol", "fido.cable").build()

                        Log.d(TAG, "connect: request: $request")

                        socket = client.newWebSocket(request, object : WebSocketListener() {
                            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                                closeWebsocket()
                            }

                            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                                Log.e(TAG, "Tunnel failure: ${t.message}", t)
                                callback.error(TunnelException("Websocket failed", t))
                            }

                            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                                Log.d(TAG, "Received ${bytes.size} bytes")
                                callback.message(bytes.toByteArray())
                            }

                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                socketStatus = SocketStatus.CONNECTED
                                callback.connected(response)
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "connect: ", e)
                callback.error(TunnelException("Websocket connect failed", e))
            }
        }
        this.submitThread = threadNewThread
        threadNewThread.setName("TunnelWebSocket")
        this.socketStatus = SocketStatus.CONNECTING
        this.submitThread?.start()
    }

    @Synchronized
    fun send(bArr: ByteArray) {
        Log.d(TAG, "send() with state= $socketStatus")
        if (this.socketStatus != SocketStatus.CONNECTED) {
            Log.d(TAG, "send() called when websocket is not connected")
            this.callback.error(TunnelException("sending data error: websocket is not connected"))
            return
        }
        try {
            socket?.send(ByteString.of(*bArr))
        } catch (e: Exception) {
            Log.d(TAG, "Failed to send frame")
            this.callback.error(TunnelException("Failed to send frame", e))
            close()
        }
    }
}