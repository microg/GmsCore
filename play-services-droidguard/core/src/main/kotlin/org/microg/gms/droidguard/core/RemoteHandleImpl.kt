/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

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
        // For remote, we may need to init session if multi-step
        ensureSession()
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")
        ensureSession()
        val paramsMap = mutableMapOf<String, String>("flow" to (flow ?: ""), "source" to packageName)
        if (sessionId != null) paramsMap["sid"] = sessionId!!
        request?.bundle?.keySet()?.forEach { key ->
            when (val value = request?.bundle?.get(key)) {
                is String -> paramsMap["x-request-$key"] = value
                is ByteArray -> paramsMap["x-request-$key"] = Base64.encodeToString(value, Base64.NO_WRAP)
                is java.util.ArrayList<*> -> {
                    if (value.isNotEmpty() && value[0] is String) {
                        paramsMap["x-request-$key"] = value.joinToString(",")
                    } else {
                        paramsMap["x-request-$key"] = value.toString()
                    }
                }
                else -> if (value != null) paramsMap["x-request-$key"] = value.toString()
            }
        }
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        val payload = map.orEmpty().map { 
            val k = it.key as? String ?: it.key.toString()
            val v = it.value as? String ?: it.value.toString()
            Uri.encode(k) + "=" + Uri.encode(v) 
        }.joinToString("&")
        Log.d(TAG, "POST ${connection.url}: $payload")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        connection.outputStream.use { it.write(payload.encodeToByteArray()) }
        val response = connection.inputStream.use { it.readBytes() }.decodeToString()
        // Support server returning sid|base64result for sessioned multi-step
        val resultStr = if (response.contains("|") && sessionId == null) {
            val parts = response.split("|", limit = 2)
            sessionId = parts[0]
            parts[1]
        } else response
        return Base64.decode(resultStr, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
    }

    override fun close() {
        Log.d(TAG, "close()")
        if (sessionId != null) {
            try {
                val paramsMap = mapOf("flow" to (flow ?: ""), "source" to packageName, "sid" to sessionId!!, "action" to "close")
                val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
                val connection = URL("$url?$params").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.outputStream.use { it.write("close".toByteArray()) }
                connection.inputStream.use { it.readBytes() } // drain
            } catch (e: Exception) {
                Log.w(TAG, "close remote session failed", e)
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
        ensureSession()
        return null // remote doesn't return PFD reply for now
    }

    private fun ensureSession() {
        if (sessionId != null) return
        try {
            val paramsMap = mutableMapOf<String, String>("flow" to (flow ?: ""), "source" to packageName, "action" to "init")
            request?.bundle?.keySet()?.forEach { key ->
                when (val value = request?.bundle?.get(key)) {
                    is String -> paramsMap["x-request-$key"] = value
                    is ByteArray -> paramsMap["x-request-$key"] = Base64.encodeToString(value, Base64.NO_WRAP)
                    else -> if (value != null) paramsMap["x-request-$key"] = value.toString()
                }
            }
            val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
            val connection = URL("$url?$params").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.outputStream.use { it.write(ByteArray(0)) }
            val resp = connection.inputStream.use { it.readBytes() }.decodeToString().trim()
            if (resp.contains("|")) {
                sessionId = resp.substringBefore("|")
            } else if (resp.isNotEmpty() && !resp.startsWith("ERROR")) {
                // server may return sid directly for init
                sessionId = resp
            }
            Log.d(TAG, "remote session init: $sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "ensureSession failed, will use stateless", e)
        }
    }
}