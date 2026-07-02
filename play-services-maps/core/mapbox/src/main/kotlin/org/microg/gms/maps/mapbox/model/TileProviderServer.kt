/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps.mapbox.model

import android.util.Log
import com.google.android.gms.maps.model.TileProvider
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Bridges the Google Maps [TileProvider] callback API (which hands back tile bitmaps for a given
 * x/y/zoom) to MapLibre, whose raster sources can only fetch tiles from a URL. A tiny loopback HTTP
 * server serves each registered tile provider under `http://127.0.0.1:<port>/<overlayId>/{z}/{x}/{y}`,
 * so a [com.mapbox.mapboxsdk.style.sources.RasterSource] can render provider tiles (e.g. the Google
 * Photos photo-density heatmap) without any remote service.
 */
internal object TileProviderServer {
    private const val TAG = "GmsTileServer"

    private val providers = ConcurrentHashMap<String, TileProvider>()
    private val tokenCounter = AtomicLong(0)
    private val executor = Executors.newCachedThreadPool { r -> Thread(r, "GmsTileServer").apply { isDaemon = true } }

    @Volatile
    private var serverSocket: ServerSocket? = null

    var port: Int = -1
        private set

    @Synchronized
    private fun ensureStarted() {
        if (serverSocket != null) return
        val socket = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        port = socket.localPort
        serverSocket = socket
        Thread({
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    executor.execute { handle(client) }
                } catch (e: Exception) {
                    if (!socket.isClosed) Log.w(TAG, "accept failed", e)
                }
            }
        }, "GmsTileServer-accept").apply { isDaemon = true }.start()
        Log.d(TAG, "Tile provider server listening on 127.0.0.1:$port")
    }

    /** Register [provider] and return a globally-unique token used to build its tile URL. */
    fun register(provider: TileProvider): String {
        ensureStarted()
        val token = tokenCounter.incrementAndGet().toString()
        providers[token] = provider
        return token
    }

    fun unregister(token: String) {
        providers.remove(token)
    }

    /** Build the MapLibre raster tile URL template for a registered provider [token]. */
    fun tileUrl(token: String): String = "http://127.0.0.1:$port/$token/{z}/{x}/{y}"

    private fun handle(socket: Socket) {
        try {
            socket.use {
                val input = it.getInputStream().bufferedReader()
                val requestLine = input.readLine() ?: return
                // Drain the rest of the request headers.
                while (true) {
                    val line = input.readLine()
                    if (line == null || line.isEmpty()) break
                }
                val output = it.getOutputStream()
                // requestLine: "GET /<id>/<z>/<x>/<y> HTTP/1.1"
                val target = requestLine.split(' ').getOrNull(1)
                val segments = target?.trim('/')?.split('/')
                if (segments == null || segments.size < 4) {
                    writeStatus(output, 400)
                    return
                }
                val id = segments[0]
                val z = segments[1].toIntOrNull()
                val x = segments[2].toIntOrNull()
                val y = segments[3].substringBefore('.').toIntOrNull()
                val provider = providers[id]
                if (provider == null || z == null || x == null || y == null) {
                    writeStatus(output, 404)
                    return
                }
                val tile = try {
                    provider.getTile(x, y, z)
                } catch (e: Exception) {
                    Log.w(TAG, "getTile($x,$y,$z) failed", e)
                    null
                }
                val data = tile?.data
                if (tile == null || tile === TileProvider.NO_TILE || data == null) {
                    writeStatus(output, 404)
                    return
                }
                output.write(
                    ("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: image/png\r\n" +
                            "Content-Length: ${data.size}\r\n" +
                            "Cache-Control: max-age=86400\r\n" +
                            "Connection: close\r\n\r\n").toByteArray()
                )
                output.write(data)
                output.flush()
            }
        } catch (e: Exception) {
            // Client hang-ups are routine while panning; keep them quiet.
            Log.v(TAG, "request failed: ${e.message}")
        }
    }

    private fun writeStatus(output: OutputStream, code: Int) {
        try {
            output.write(("HTTP/1.1 $code Status\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").toByteArray())
            output.flush()
        } catch (e: Exception) {
            // ignore
        }
    }
}
