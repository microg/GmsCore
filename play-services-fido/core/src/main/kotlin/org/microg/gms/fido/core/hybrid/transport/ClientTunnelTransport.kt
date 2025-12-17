/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.transport

import android.util.Log
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import okhttp3.Response
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.hybrid.buildWebSocketConnectUrl
import org.microg.gms.fido.core.hybrid.tunnel.TunnelException
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebCallback
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebsocket
import org.microg.gms.fido.core.hybrid.utils.CryptoHelper

private const val TAG = "ClientTunnelTransport"

class ClientTunnelTransport(val eid: ByteArray, val randomSeed: ByteArray, val callback: TunnelCallback) : TunnelWebCallback {

    private var websocket: TunnelWebsocket? = null
    private var decryptEid: ByteArray? = null

    fun startConnecting() {
        Log.d(TAG, "startConnecting: ")
        decryptEid = decryptEid()
        val routingId = decryptEid!!.sliceArray(11..13)
        val domainId = ((decryptEid!![15].toInt() and 0xFF) shl 8) or (decryptEid!![14].toInt() and 0xFF)
        val tunnelId = CryptoHelper.endif(ikm = randomSeed, salt = ByteArray(0), info = byteArrayOf(2, 0, 0, 0), length = 16)

        val webSocketConnectUrl = buildWebSocketConnectUrl(domainId, routingId, tunnelId)
        Log.d(TAG, "startConnecting: webSocketConnectUrl=$webSocketConnectUrl")
        if (websocket == null) {
            websocket = TunnelWebsocket(webSocketConnectUrl, this)
        }
        websocket?.connect()
    }

    private fun decryptEid(): ByteArray {
        val decryptEid = CryptoHelper.decryptEid(eid, randomSeed) ?: throw RequestHandlingException(ErrorCode.UNKNOWN_ERR, "EID decrypt failed")
        if (decryptEid.size != 16 || decryptEid[0] != 0.toByte()) {
            throw RequestHandlingException(ErrorCode.UNKNOWN_ERR, "EID structure invalid")
        }
        return decryptEid
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
        val pt = decryptEid ?: decryptEid()

        Log.d(TAG, "connected: $pt response: $response")

        val socketHashKey = CryptoHelper.endif(ikm = randomSeed, salt = pt, info = byteArrayOf(3, 0, 0, 0), length = 32)
        callback.onSocketConnect(websocket, socketHashKey)
    }

    override fun message(data: ByteArray) {
        callback.onMessage(websocket, data)
    }

}