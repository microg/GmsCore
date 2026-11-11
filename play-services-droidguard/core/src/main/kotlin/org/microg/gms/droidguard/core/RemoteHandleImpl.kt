/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val TAG = "RemoteGuardImpl"

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private var sessionId: String? = null
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context)?.trim()?.trimEnd('/')
            ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        initWithRequest(flow, null)
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        this.sessionId = UUID.randomUUID().toString()
        // Return an empty DroidGuardInitReply so DroidGuardApiClient does not redundantly call init(flow)
        // while keeping cross-process Play Integrity (pia_attest_e1 / pia_express) execution delegated
        // to the donor Android device running RemoteDroidGuardServer.
        return DroidGuardInitReply(null, null)
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")
        val paramsMap = buildQueryParams(flow, packageName, sessionId, request)
        val params = paramsMap.entries.joinToString("&") { (k, v) ->
            Uri.encode(k) + "=" + Uri.encode(v)
        }
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        val timeoutMs = request?.timeoutMillis?.takeIf { it > 0 } ?: 60_000
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        val payload = encodeSnapshotMap(map)
        Log.d(TAG, "POST ${connection.url}: (${payload.length} chars)")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        sessionId?.let { connection.setRequestProperty("X-DroidGuard-Session", it) }
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        connection.outputStream.use { it.write(payload.encodeToByteArray()) }
        val status = connection.responseCode
        if (status !in 200..299) {
            val errText = runCatching { connection.errorStream?.use { it.readBytes().decodeToString() } }.getOrNull()
            throw IllegalStateException("Remote DroidGuard server HTTP $status: ${errText ?: connection.responseMessage}")
        }
        val bytes = connection.inputStream.use { it.readBytes() }.decodeToString().trim()
        return Base64.decode(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    override fun close() {
        Log.d(TAG, "close()")
        this.request = null
        this.flow = null
        this.sessionId = null
    }

    companion object {
        private const val B64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun buildQueryParams(
            flow: String?,
            packageName: String,
            sessionId: String?,
            request: DroidGuardResultsRequest?,
        ): LinkedHashMap<String, String> {
            val paramsMap = linkedMapOf<String, String>()
            if (flow != null) paramsMap["flow"] = flow
            paramsMap["source"] = packageName
            if (!sessionId.isNullOrEmpty()) paramsMap["session"] = sessionId
            val bundle: Bundle? = request?.bundle
            if (bundle != null) {
                for (key in bundle.keySet()) {
                    @Suppress("DEPRECATION")
                    when (val raw = bundle.get(key)) {
                        is String -> paramsMap["x-request-$key"] = raw
                        is ByteArray -> paramsMap["x-request-b64-$key"] = Base64.encodeToString(raw, B64_FLAGS)
                        is Int, is Long, is Boolean -> paramsMap["x-request-meta-$key"] = raw.toString()
                    }
                }
            }
            return paramsMap
        }

        fun encodeSnapshotMap(map: Map<Any?, Any?>?): String {
            return map.orEmpty().entries.mapNotNull { (k, v) ->
                val keyStr = k?.toString() ?: return@mapNotNull null
                when (v) {
                    is ByteArray -> Uri.encode("b64:$keyStr") + "=" + Uri.encode(Base64.encodeToString(v, B64_FLAGS))
                    null -> null
                    else -> Uri.encode(keyStr) + "=" + Uri.encode(v.toString())
                }
            }.joinToString("&")
        }
    }
}
