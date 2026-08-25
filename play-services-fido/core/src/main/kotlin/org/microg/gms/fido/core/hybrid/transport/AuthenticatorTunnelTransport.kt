/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.transport

import android.util.Log
import okhttp3.Response
import okio.ByteString.Companion.decodeHex
import org.microg.gms.fido.core.hybrid.buildWebSocketNewUrl
import org.microg.gms.fido.core.hybrid.tunnel.TunnelException
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebCallback
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebsocket

private const val TAG = "AuthenticatorTransport"

class AuthenticatorTunnelTransport(val tunnelId: ByteArray, val callback: TunnelCallback) : TunnelWebCallback {

    private var websocket: TunnelWebsocket? = null

    fun startConnecting() {
        Log.d(TAG, "startConnecting: ")
        val webSocketConnectUrl = buildWebSocketNewUrl(tunnelId)
        Log.d(TAG, "startConnecting: webSocketConnectUrl=$webSocketConnectUrl")
        if (websocket == null) {
            websocket = TunnelWebsocket(webSocketConnectUrl, this)
        }
        websocket?.connect()
    }

    fun stopConnecting() {
        Log.d(TAG, "stopConnecting: ")
        websocket?.close()
    }

    override fun disconnected() {
        Log.d(TAG, "disconnected: ")
        callback.onSocketClose()
    }

    override fun error(error: TunnelException) {
        Log.d(TAG, "error: ", error)
        callback.onSocketError(error)
    }

    override fun connected(response: Response) {
        val routingId = runCatching { response.header("X-Cable-Routing-Id")?.decodeHex()?.toByteArray() }.getOrNull()
        Log.d(TAG, "Routing ID from server: (${routingId?.size} bytes)")
        if (routingId == null || routingId.size < 3) {
            callback.onSocketError(TunnelException("routingId is null"))
            return
        }
        callback.onSocketConnect(websocket, routingId)
    }

    override fun message(data: ByteArray) {
        callback.onMessage(websocket, data)
    }

}