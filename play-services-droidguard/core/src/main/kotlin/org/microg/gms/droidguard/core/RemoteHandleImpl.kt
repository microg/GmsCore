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
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RemoteGuardImpl"

/**
 * Remote (network) DroidGuard handle.
 *
 * Legacy behavior: every [snapshot] is a single, stateless POST carrying the
 * flow + source + request params in the query string (single-step flows such
 * as ad attestation). This path is kept byte-for-byte identical so existing
 * deployments keep working against simple servers.
 *
 * Request-backed flows (Play Integrity / App Check) on [com.google.android.gms.droidguard.internal.IDroidGuardService.guardWithRequest]
 * evaluate the device across *multiple* internal steps. For those, this handle
 * speaks the multi-step session protocol understood by the reference server
 * (begin/snapshot/close), so the network backend can maintain per-request
 * state the way a local DroidGuard would.
 */
class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private var sessionId: String? = null
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        this.flow = flow
        this.request = null
        this.sessionId = null
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")
        return if (request != null) {
            snapshotWithSession(map.orEmpty())
        } else {
            snapshotLegacy(map.orEmpty())
        }
    }

    private fun snapshotLegacy(map: Map<Any?, Any?>): ByteArray {
        val paramsMap = mutableMapOf("flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val payload = map.map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
        val bytes = post("$url?$params", payload)
        return decode(bytes)
    }

    private fun snapshotWithSession(map: Map<Any?, Any?>): ByteArray {
        if (sessionId == null) {
            sessionId = beginSession()
        }
        val params = "action=snapshot&sessionId=$sessionId"
        val payload = map.map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
        Log.d(TAG, "session snapshot $sessionId step: $map")
        return decode(post("$url?$params", payload))
    }

    /** Ask the backend to create a session for this flow/request. */
    private fun beginSession(): String {
        val paramsMap = mutableMapOf("action" to "begin", "flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val params = paramsMap.map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
        val response = post("$url?$params", "")
        val kv = response.split("&").mapNotNull {
            val pair = it.split("=", limit = 2)
            if (pair.size == 2) pair[0] to pair[1] else null
        }.toMap()
        return kv["sessionId"] ?: throw IllegalStateException("Server did not establish a session: $response")
    }

    private fun post(url: String, payload: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        Log.d(TAG, "POST $url: $payload")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.requestMethod = "POST"
        connection.doInput = true
        if (payload.isNotEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { it.write(payload.encodeToByteArray()) }
        }
        return connection.inputStream.use { it.readBytes() }.decodeToString()
    }

    private fun decode(blob: String): ByteArray =
        Base64.decode(blob, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)

    override fun close() {
        Log.d(TAG, "close()")
        if (sessionId != null) {
            try {
                post("$url?action=close&sessionId=$sessionId", "")
            } catch (e: Exception) {
                Log.w(TAG, "Error closing remote session", e)
            }
        }
        this.request = null
        this.flow = null
        this.sessionId = null
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        this.sessionId = null
        return null
    }
}