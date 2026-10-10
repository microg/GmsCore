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
import java.net.HttpURLConnection
import java.net.URL

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    // Explicit URL fragment opts in; it is never sent to the server.
    private var session: RemoteSession? = null
    private var sessionMode = false

    @Synchronized
    override fun init(flow: String?) {
        initWithRequest(flow, null)
    }

    @Synchronized
    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        if (sessionMode) {
            val remote = checkNotNull(session) { "Remote handle is closed" }
            val parameters = map.orEmpty().entries.associate { (key, value) ->
                require(key is String && value is String) { "Snapshot requires string keys and values" }
                "data-$key" to value
            }
            return Base64.decode(remote.snapshot(parameters), Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
        }
        val paramsMap = mutableMapOf("flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        val payload = map.orEmpty().map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        try {
            connection.outputStream.use { it.write(payload.encodeToByteArray()) }
            val bytes = connection.inputStream.use { it.readBytes() }.decodeToString()
            return Base64.decode(bytes, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    override fun close() {
        try {
            session?.close()
        } finally {
            session = null
            request = null
            flow = null
        }
    }

    @Synchronized
    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        close()
        this.flow = flow
        this.request = request
        val endpoint = URL(url)
        sessionMode = endpoint.ref == "droidguard-session-v1"
        if (sessionMode) {
            require(endpoint.protocol == "https" || endpoint.protocol == "http")
            require(endpoint.query == null) { "Session endpoint must not contain a query" }
            val base = url.substringBefore('#').trimEnd('/')
            val remote = RemoteSession { operation, parameters -> postSession(base, operation, parameters) }
            // Retain an uninitialized session on failure, so snapshot cannot fall back to legacy.
            session = remote
            val parameters = mutableMapOf("source" to packageName)
            flow?.let { parameters["flow"] = it }
            for (key in request?.bundle?.keySet().orEmpty()) {
                when (val value = request?.bundle?.get(key)) {
                    is String, is Int, is Long, is Boolean -> parameters["request-$key"] = value.toString()
                    null -> Unit
                    else -> throw IllegalArgumentException("Unsupported remote request field: $key")
                }
            }
            remote.init(parameters)
        }
        return null
    }

    private fun postSession(base: String, operation: String, parameters: Map<String, String>): String {
        val connection = URL("$base/$operation").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = (request?.timeoutMillis ?: 60000).coerceIn(1, 120000)
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.doOutput = true
            val payload = parameters.entries.joinToString("&") { Uri.encode(it.key) + "=" + Uri.encode(it.value) }
            connection.outputStream.use { it.write(payload.encodeToByteArray()) }
            check(connection.responseCode == 200) { "Remote DroidGuard HTTP ${connection.responseCode}" }
            return connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    check(output.size() + count <= 1048576) { "Remote DroidGuard response too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray().decodeToString()
            }
        } finally {
            connection.disconnect()
        }
    }
}
