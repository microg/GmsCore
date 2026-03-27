/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.hybrid.transport

import org.microg.gms.fido.core.hybrid.tunnel.TunnelException
import org.microg.gms.fido.core.hybrid.tunnel.TunnelWebsocket

interface TunnelCallback {
    fun onSocketConnect(websocket: TunnelWebsocket?, bytes: ByteArray)
    fun onSocketError(error: TunnelException)
    fun onSocketClose()
    fun onMessage(websocket: TunnelWebsocket?, data: ByteArray)
}