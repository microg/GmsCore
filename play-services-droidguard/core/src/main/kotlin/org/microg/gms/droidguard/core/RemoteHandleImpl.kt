/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle

private const val TAG = "RemoteGuardImpl"
private const val DEFAULT_TIMEOUT_MILLIS = 60000

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private var remoteSession: RemoteDroidGuardSession? = null

    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init()")
        closeRemoteSession()
        this.flow = flow
        startRemoteSession()
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot(fields=${map?.size ?: 0})")
        val response = remoteSession?.snapshot(map) ?: doSnapshot(flow, request, map.orEmpty())
        return Base64.decode(response.trim(), Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
    }

    override fun close() {
        Log.d(TAG, "close()")
        closeRemoteSession()
        request = null
        flow = null
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest(requestFields=${request?.bundle?.size() ?: 0})")
        closeRemoteSession()
        this.flow = flow
        this.request = request
        return null
    }

    private fun startRemoteSession() {
        val timeoutMillis = (request?.timeoutMillis ?: DEFAULT_TIMEOUT_MILLIS).coerceAtLeast(1)
        try {
            val session = RemoteDroidGuardSession(
                RemoteDroidGuardHttpClient(url, timeoutMillis),
                buildRequestParameters(flow, request)
            )
            session.begin()
            remoteSession = session
        } catch (e: Exception) {
            Log.w(TAG, "Remote server does not provide session setup; using single-request mode", e)
            remoteSession = null
        }
    }

    private fun closeRemoteSession() {
        val session = remoteSession ?: return
        remoteSession = null
        try {
            session.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close remote DroidGuard session", e)
        }
    }

    private fun doSnapshot(
        flow: String?,
        request: DroidGuardResultsRequest?,
        map: Map<Any?, Any?>
    ): String {
        val timeoutMillis = (request?.timeoutMillis ?: DEFAULT_TIMEOUT_MILLIS).coerceAtLeast(1)
        val client = RemoteDroidGuardHttpClient(url, timeoutMillis)
        return client.post(null, buildRequestParameters(flow, request), RemoteDroidGuardHttpClient.encodeForm(map))
    }

    private fun buildRequestParameters(flow: String?, request: DroidGuardResultsRequest?): Map<String, String> {
        val parameters = linkedMapOf("flow" to (flow ?: ""), "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty().sorted()) {
            val value = request?.bundle?.getRemoteScalar(key) ?: continue
            parameters["x-request-$key"] = value
        }
        return parameters
    }

    @Suppress("DEPRECATION")
    private fun android.os.Bundle.getRemoteScalar(key: String): String? = get(key).asRemoteScalar()

    private fun Any?.asRemoteScalar(): String? = when (this) {
        is String -> this
        is Number, is Boolean -> toString()
        else -> null
    }
}
