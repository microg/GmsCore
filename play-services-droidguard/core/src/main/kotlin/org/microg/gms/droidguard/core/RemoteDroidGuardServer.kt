/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.tasks.Tasks
import org.microg.gms.droidguard.DroidGuardClientImpl
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Embedded HTTP server that runs on a Play-Integrity-passing donor Android phone.
 *
 * Accepts requests from `RemoteHandleImpl`, reconstructs the exact `DroidGuardResultsRequest`
 * (including `ByteArray` bundle entries such as `piaExpressDeviceKey` for `pia_express` and
 * `thirdPartyCallerAppPackageName` for `pia_attest_e1`), executes the full multi-step
 * cross-process `DroidGuardClientImpl(context, callerPkg).getResults(flow, data, request)`
 * on the donor device, and returns the URL-safe Base64 token.
 */
class RemoteDroidGuardServer(
    private val context: Context,
    private val port: Int = DEFAULT_PORT,
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        val sock = ServerSocket()
        sock.reuseAddress = true
        sock.bind(InetSocketAddress("0.0.0.0", port))
        serverSocket = sock
        executor.execute {
            Log.i(TAG, "RemoteDroidGuardServer listening on port $port")
            while (running.get()) {
                try {
                    val client = sock.accept()
                    executor.execute { handleClient(client) }
                } catch (e: Exception) {
                    if (running.get()) Log.w(TAG, "Accept error", e)
                }
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 60_000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0)?.uppercase() ?: "GET"
            val rawTarget = parts.getOrNull(1) ?: "/"

            var contentLength = 0
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            if (method == "GET" && rawTarget.startsWith("/health")) {
                writeResponse(s, 200, "OK")
                return
            }
            if (method != "POST") {
                writeResponse(s, 405, "Method Not Allowed")
                return
            }

            val bodyChars = CharArray(contentLength.coerceAtLeast(0))
            var offset = 0
            while (offset < bodyChars.size) {
                val read = reader.read(bodyChars, offset, bodyChars.size - offset)
                if (read <= 0) break
                offset += read
            }
            val bodyString = String(bodyChars, 0, offset)

            try {
                val parsed = parseIncomingRequest(rawTarget, bodyString)
                val client = DroidGuardClientImpl(context, parsed.sourcePackage)
                val tokenBase64 = Tasks.await(
                    client.getResults(parsed.flow, parsed.snapshotData, parsed.resultsRequest),
                    60,
                    TimeUnit.SECONDS,
                )
                writeResponse(s, 200, tokenBase64.trim())
            } catch (e: Exception) {
                Log.w(TAG, "Error handling remote DroidGuard request", e)
                writeResponse(s, 500, "ERROR : ${e.message}")
            }
        }
    }

    private fun writeResponse(socket: Socket, status: Int, body: String) {
        val bodyBytes = body.encodeToByteArray()
        val statusText = when (status) {
            200 -> "OK"
            405 -> "Method Not Allowed"
            else -> "Internal Server Error"
        }
        val header = "HTTP/1.1 $status $statusText\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "Content-Length: ${bodyBytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        val out = socket.getOutputStream()
        out.write(header.encodeToByteArray())
        out.write(bodyBytes)
        out.flush()
    }

    data class ParsedRemoteGuardRequest(
        val flow: String,
        val sourcePackage: String,
        val sessionId: String?,
        val resultsRequest: DroidGuardResultsRequest,
        val snapshotData: Map<String, String>,
    )

    companion object {
        private const val TAG = "RemoteDGServer"
        const val DEFAULT_PORT = 8789
        private const val B64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun parseIncomingRequest(rawTarget: String, formBody: String): ParsedRemoteGuardRequest {
            val queryString = rawTarget.substringAfter("?", "")
            val queryMap = parseUrlEncodedPairs(queryString)
            val flow = queryMap["flow"] ?: "pia_attest_e1"
            val sourcePackage = queryMap["source"] ?: "com.google.android.gms"
            val sessionId = queryMap["session"]

            val request = DroidGuardResultsRequest()
            for ((k, v) in queryMap) {
                when {
                    k.startsWith("x-request-b64-") -> {
                        val bundleKey = k.removePrefix("x-request-b64-")
                        request.bundle.putByteArray(bundleKey, Base64.decode(v, B64_FLAGS))
                    }
                    k.startsWith("x-request-meta-") -> {
                        val bundleKey = k.removePrefix("x-request-meta-")
                        val intVal = v.toIntOrNull()
                        if (intVal != null) {
                            request.bundle.putInt(bundleKey, intVal)
                        } else {
                            request.bundle.putString(bundleKey, v)
                        }
                    }
                    k.startsWith("x-request-") -> {
                        val bundleKey = k.removePrefix("x-request-")
                        request.bundle.putString(bundleKey, v)
                    }
                }
            }
            if (request.bundle.getString("thirdPartyCallerAppPackageName") == null && sourcePackage.isNotEmpty()) {
                request.bundle.putString("thirdPartyCallerAppPackageName", sourcePackage)
            }

            val snapshotData = linkedMapOf<String, String>()
            for ((k, v) in parseUrlEncodedPairs(formBody)) {
                if (k.startsWith("b64:")) {
                    val actualKey = k.removePrefix("b64:")
                    snapshotData[actualKey] = String(Base64.decode(v, B64_FLAGS), Charsets.ISO_8859_1)
                } else {
                    snapshotData[k] = v
                }
            }

            return ParsedRemoteGuardRequest(
                flow = flow,
                sourcePackage = sourcePackage,
                sessionId = sessionId,
                resultsRequest = request,
                snapshotData = snapshotData,
            )
        }

        fun parseUrlEncodedPairs(encoded: String): Map<String, String> {
            if (encoded.isBlank()) return emptyMap()
            val result = linkedMapOf<String, String>()
            for (pair in encoded.split("&")) {
                if (pair.isEmpty()) continue
                val idx = pair.indexOf('=')
                if (idx < 0) {
                    result[Uri.decode(pair)] = ""
                } else {
                    val k = Uri.decode(pair.substring(0, idx))
                    val v = Uri.decode(pair.substring(idx + 1))
                    result[k] = v
                }
            }
            return result
        }
    }
}
