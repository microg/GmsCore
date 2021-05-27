/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Bundle
import android.util.Base64
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.microg.gms.checkin.LastCheckinInfo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface DroidGuardResultCreator {
    suspend fun getResult(flow: String, data: Map<String, String>): ByteArray

    companion object {
        fun getInstance(context: Context): DroidGuardResultCreator = when (DroidGuardPreferences(context).mode) {
            DroidGuardPreferences.Mode.Disabled -> throw RuntimeException("DroidGuard disabled")
            DroidGuardPreferences.Mode.Connector -> ConnectorDroidGuardResultCreator(context)
            DroidGuardPreferences.Mode.Network -> NetworkDroidGuardResultCreator(context)
        }

        suspend fun getResult(context: Context, flow: String, data: Map<String, String>): ByteArray =
                getInstance(context).getResult(flow, data)
    }
}

private class ConnectorDroidGuardResultCreator(private val context: Context) : DroidGuardResultCreator {
    override suspend fun getResult(flow: String, data: Map<String, String>): ByteArray = suspendCoroutine { continuation ->
        Thread {
            val bundle = Bundle()
            for (entry in data) {
                bundle.putString(entry.key, entry.value)
            }
            val conn = RemoteDroidGuardConnector(context)
            val dg = conn.guard(flow, LastCheckinInfo.read(context).androidId.toString(), bundle)
            if (dg == null) {
                continuation.resumeWithException(RuntimeException("No DroidGuard result"))
            } else if (dg.statusCode == 0 && dg.result != null) {
                continuation.resume(dg.result)
            } else {
                continuation.resumeWithException(RuntimeException("Status: " + dg.statusCode + ", error:" + dg.errorMsg))
            }
        }.start()
    }
}

private class NetworkDroidGuardResultCreator(private val context: Context) : DroidGuardResultCreator {
    private val queue = Volley.newRequestQueue(context)
    private val url: String
        get() = DroidGuardPreferences(context).networkServerUrl ?: throw RuntimeException("Network URL required")

    override suspend fun getResult(flow: String, data: Map<String, String>): ByteArray = suspendCoroutine { continuation ->
        queue.add(PostParamsStringRequest("$url?flow=$flow", data, {
            continuation.resume(Base64.decode(it, Base64.NO_WRAP + Base64.NO_PADDING + Base64.URL_SAFE))
        }, {
            continuation.resumeWithException(RuntimeException(it))
        }))
    }
}

class PostParamsStringRequest(url: String, private val data: Map<String, String>, listener: (String) -> Unit, errorListener: (VolleyError) -> Unit) : StringRequest(Method.POST, url, listener, errorListener) {
    override fun getParams(): Map<String, String> = data
}
