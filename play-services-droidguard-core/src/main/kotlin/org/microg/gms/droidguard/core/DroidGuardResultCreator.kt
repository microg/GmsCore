/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.util.Base64
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.await
import org.microg.gms.droidguard.DroidGuardClient
import org.microg.gms.droidguard.DroidGuardClientImpl
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface DroidGuardResultCreator {
    suspend fun getResult(flow: String, data: Map<String, String>): ByteArray

    companion object {
        fun getInstance(context: Context): DroidGuardResultCreator =
                if (DroidGuardPreferences.isEnabled(context)) {
                    when (DroidGuardPreferences.getMode(context)) {
                        DroidGuardPreferences.Mode.Embedded -> EmbeddedDroidGuardResultCreator(context)
                        DroidGuardPreferences.Mode.Network -> NetworkDroidGuardResultCreator(context)
                    }
                } else {
                    throw RuntimeException("DroidGuard disabled")
                }

        suspend fun getResult(context: Context, flow: String, data: Map<String, String>): ByteArray =
                getInstance(context).getResult(flow, data)
    }
}

private class NetworkDroidGuardResultCreator(private val context: Context) : DroidGuardResultCreator {
    private val queue = Volley.newRequestQueue(context)
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override suspend fun getResult(flow: String, data: Map<String, String>): ByteArray = suspendCoroutine { continuation ->
        queue.add(PostParamsStringRequest("$url?flow=$flow", data, {
            continuation.resume(Base64.decode(it, Base64.NO_WRAP + Base64.NO_PADDING + Base64.URL_SAFE))
        }, {
            continuation.resumeWithException(it.cause ?: it)
        }))
    }

    companion object {
        class PostParamsStringRequest(url: String, private val data: Map<String, String>, listener: (String) -> Unit, errorListener: (VolleyError) -> Unit) : StringRequest(Method.POST, url, listener, errorListener) {
            override fun getParams(): Map<String, String> = data
        }
    }
}

private class EmbeddedDroidGuardResultCreator(private val context: Context) : DroidGuardResultCreator {
    private val client: DroidGuardClient by lazy { DroidGuardClientImpl(context) }
    override suspend fun getResult(flow: String, data: Map<String, String>): ByteArray {
        val handle = client.getHandle().await()
        try {
            handle.init(flow)
            return handle.guard(data)
        } finally {
            try {
                handle.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
