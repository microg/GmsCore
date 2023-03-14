/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.Volley
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.RecaptchaResultData
import com.google.android.gms.recaptcha.internal.ExecuteParams
import com.google.android.gms.recaptcha.internal.InitParams
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CompletableDeferred
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import org.microg.gms.droidguard.core.VersionUtil
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "RecaptchaGuard"

class RecaptchaGuardImpl(private val context: Context, private val packageName: String) : RecaptchaImpl {
    private val queue = Volley.newRequestQueue(context)
    private var lastToken: String? = null

    override suspend fun init(params: InitParams): RecaptchaHandle {
        val response = ProtobufPostRequest(
            "https://www.recaptcha.net/recaptcha/api3/ac", RecaptchaInitRequest(
                data_ = RecaptchaInitRequest.Data(
                    siteKey = params.siteKey,
                    packageName = packageName,
                    version = "${VersionUtil(context).versionCode};${params.version}"
                )
            ), RecaptchaInitResponse.ADAPTER
        ).sendAndAwait(queue)
        lastToken = response.token
        return RecaptchaHandle(params.siteKey, packageName, response.acceptableAdditionalArgs.toList())
    }

    override suspend fun execute(params: ExecuteParams): RecaptchaResultData {
        if (params.handle.clientPackageName != null && params.handle.clientPackageName != packageName) throw IllegalArgumentException("invalid handle")
        val timestamp = System.currentTimeMillis()
        val additionalArgs = mutableMapOf<String, String>()
        val guardMap = mutableMapOf<String, String>()
        for (key in params.action.additionalArgs.keySet()) {
            val value = params.action.additionalArgs.getString(key)
                ?: throw Exception("Only string values are allowed as an additional arg in RecaptchaAction")
            if (key !in params.handle.acceptableAdditionalArgs)
                throw Exception("AdditionalArgs key[ \"$key\" ] is not accepted by reCATPCHA server")
            additionalArgs.put(key, value)
        }
        Log.d(TAG, "Additional arguments: $additionalArgs")
        if (lastToken == null) {
            init(InitParams().apply { siteKey = params.handle.siteKey; version = params.version })
        }
        val token = lastToken!!
        guardMap["token"] = token
        guardMap["action"] = params.action.toString()
        guardMap["timestamp_millis"] to timestamp.toString()
        guardMap.putAll(additionalArgs)
        if (params.action.verificationHistoryToken != null)
            guardMap["verification_history_token"] = params.action.verificationHistoryToken
        val dg = DroidGuardResultCreator.getResults(context, "recaptcha-android", guardMap)
        val response = ProtobufPostRequest(
            "https://www.recaptcha.net/recaptcha/api3/ae", RecaptchaExecuteRequest(
                token = token,
                action = params.action.toString(),
                timestamp = timestamp,
                dg = dg,
                additionalArgs = additionalArgs,
                verificationHistoryToken = params.action.verificationHistoryToken
            ), RecaptchaExecuteResponse.ADAPTER
        ).sendAndAwait(queue)
        return RecaptchaResultData(response.token)
    }

    override suspend fun close(handle: RecaptchaHandle): Boolean {
        if (handle.clientPackageName != null && handle.clientPackageName != packageName) throw IllegalArgumentException("invalid handle")
        val closed = lastToken != null
        lastToken = null
        return closed
    }
}

class ProtobufPostRequest<I : Message<I, *>, O>(url: String, private val i: I, private val oAdapter: ProtoAdapter<O>) :
    Request<O>(Request.Method.POST, url, null) {
    private val deferred = CompletableDeferred<O>()

    override fun getHeaders(): Map<String, String> {
        val headers = HashMap(super.getHeaders())
        headers["Accept-Language"] = if (Build.VERSION.SDK_INT >= 24) LocaleList.getDefault().toLanguageTags() else Locale.getDefault().language
        return headers
    }

    override fun getBody(): ByteArray = i.encode()

    override fun getBodyContentType(): String = "application/x-protobuf"

    override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
        try {
            return Response.success(oAdapter.decode(response.data), null)
        } catch (e: VolleyError) {
            return Response.error(e)
        } catch (e: Exception) {
            return Response.error(VolleyError())
        }
    }

    override fun deliverResponse(response: O) {
        Log.d(TAG, "Got response: $response")
        deferred.complete(response)
    }

    override fun deliverError(error: VolleyError) {
        deferred.completeExceptionally(error)
    }

    suspend fun await(): O = deferred.await()

    suspend fun sendAndAwait(queue: RequestQueue): O {
        Log.d(TAG, "Sending request: $i")
        queue.add(this)
        return await()
    }
}