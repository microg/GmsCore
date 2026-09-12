/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val TAG = "RemoteGuardImpl"

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private var sessionId: String? = null
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        this.flow = flow
        beginSession(flow)
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        beginSession(flow)
        return null
    }

    private fun beginSession(flow: String?) {
        try {
            val paramsMap = mutableMapOf(
                "action" to "begin",
                "flow" to (flow ?: ""),
                "source" to packageName
            )
            for (key in request?.bundle?.keySet().orEmpty()) {
                request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
            }
            val response = postToServer(paramsMap)
            val responseParams = parseResponse(response)
            sessionId = responseParams["sessionId"]
            Log.d(TAG, "Session started: sessionId=$sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start session, falling back to single-step mode", e)
            sessionId = null
        }
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")

        if (sessionId != null) {
            return snapshotWithSession(map)
        }

        val paramsMap = mutableMapOf(
            "action" to "snapshot",
            "flow" to (flow ?: ""),
            "source" to packageName
        )
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val payload = buildPayload(map)
        val response = postToServer(paramsMap, payload)
        return decodeResponse(response)
    }

    private fun snapshotWithSession(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshotWithSession(sessionId=$sessionId)")
        val paramsMap = mutableMapOf(
            "action" to "snapshot",
            "sessionId" to (sessionId ?: ""),
            "flow" to (flow ?: ""),
            "source" to packageName
        )
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val payload = buildPayload(map)
        val response = postToServer(paramsMap, payload)
        return decodeResponse(response)
    }

    override fun close() {
        Log.d(TAG, "close()")
        if (sessionId != null) {
            try {
                val paramsMap = mutableMapOf(
                    "action" to "close",
                    "sessionId" to (sessionId ?: "")
                )
                postToServer(paramsMap)
                Log.d(TAG, "Session closed: sessionId=$sessionId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to close session on server", e)
            }
        }
        this.sessionId = null
        this.request = null
        this.flow = null
    }

    private fun buildPayload(map: Map<Any?, Any?>?): String {
        return map.orEmpty().map { (key, value) ->
            Uri.encode(key.toString()) + "=" + Uri.encode(value.toString())
        }.joinToString("&")
    }

    private fun postToServer(paramsMap: Map<String, String>, payload: String? = null): String {
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        Log.d(TAG, "POST ${connection.url}${if (payload != null) " body=$payload" else ""}")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        if (payload != null) {
            connection.outputStream.use { it.write(payload.encodeToByteArray()) }
        }
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        return reader.use { it.readText() }
    }

    private fun parseResponse(response: String): Map<String, String> {
        return response.split("&").associate {
            val parts = it.split("=", limit = 2)
            Uri.decode(parts[0]) to if (parts.size > 1) Uri.decode(parts[1]) else ""
        }
    }

    private fun decodeResponse(response: String): ByteArray {
        return Base64.decode(response.trim(), Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
    }
}