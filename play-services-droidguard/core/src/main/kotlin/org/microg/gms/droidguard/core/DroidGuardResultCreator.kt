/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.droidguard.DroidGuardClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface DroidGuardResultCreator {
    suspend fun getResults(flow: String, data: Map<String, String>): String

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

        suspend fun getResults(context: Context, flow: String, data: Map<String, String>): String =
                getInstance(context).getResults(flow, data)
    }
}

private class NetworkDroidGuardResultCreator(private val context: Context) : DroidGuardResultCreator {
    private val queue = Volley.newRequestQueue(context)
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override suspend fun getResults(flow: String, data: Map<String, String>): String = suspendCoroutine { continuation ->
        queue.add(PostParamsStringRequest("$url?flow=$flow", data, {
            continuation.resume(it)
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
    override suspend fun getResults(flow: String, data: Map<String, String>): String {
        return DroidGuardClient.getResults(context, flow, data).await()
    }
}
