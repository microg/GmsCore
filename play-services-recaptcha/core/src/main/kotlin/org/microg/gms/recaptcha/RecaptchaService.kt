/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.recaptcha

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.Parcel
import android.util.Base64
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import org.microg.gms.BaseService
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.RecaptchaResultData
import com.google.android.gms.recaptcha.internal.*
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CompletableDeferred
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.core.DroidGuardResultCreator
import org.microg.gms.droidguard.core.VersionUtil
import org.microg.gms.utils.warnOnTransactionIssues
import java.nio.charset.Charset
import java.util.Locale

private const val TAG = "RecaptchaService"

class RecaptchaService : BaseService(TAG, GmsService.RECAPTCHA) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RecaptchaServiceImpl(this, request.packageName, lifecycle),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("verify_with_recaptcha_v2_internal", 1),
                    Feature("init", 3),
                    Feature("execute", 5),
                    Feature("close", 2)
                )
            }
        )
    }
}

class RecaptchaServiceImpl(
    private val context: Context,
    private val packageName: String,
    private val lifecycle: Lifecycle
) : IRecaptchaService.Stub(), LifecycleOwner {
    private val queue = Volley.newRequestQueue(context)
    private var lastToken: String? = null

    override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    override fun verifyWithRecaptcha(callback: IExecuteCallback, siteKey: String, packageName: String) {
        Log.d(TAG, "Not yet implemented: verifyWithRecaptcha($siteKey, $packageName)")
    }

    override fun init(callback: IInitCallback, siteKey: String) {
        init2(callback, InitParams().also {
            it.siteKey = siteKey
            it.version = LEGACY_VERSION
        })
    }

    override fun execute(callback: IExecuteCallback, handle: RecaptchaHandle, action: RecaptchaAction) {
        execute2(callback, ExecuteParams().also {
            it.handle = handle
            it.action = action
            it.version = LEGACY_VERSION
        })
    }

    override fun close(callback: ICloseCallback, handle: RecaptchaHandle) {
        Log.d(TAG, "close($handle)")
        lifecycleScope.launchWhenStarted {
            try {
                val closed = lastToken != null
                lastToken = null
                callback.onClosed(Status.SUCCESS, closed)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    suspend fun runInit(siteKey: String, version: String): RecaptchaInitResponse {
        val response = ProtobufPostRequest(
            "https://www.recaptcha.net/recaptcha/api3/ac", RecaptchaInitRequest(
                data_ = RecaptchaInitRequest.Data(
                    siteKey = siteKey,
                    packageName = packageName,
                    version = "${VersionUtil(context).versionCode};${version}"
                )
            ), RecaptchaInitResponse.ADAPTER
        ).sendAndAwait(queue)
        lastToken = response.token
        return response
    }

    override fun init2(callback: IInitCallback, params: InitParams) {
        Log.d(TAG, "init($params)")
        lifecycleScope.launchWhenStarted {
            try {
                val response = runInit(params.siteKey, params.version)
                val handle = RecaptchaHandle(params.siteKey, packageName, response.acceptableAdditionalArgs.toList())
                if (params.version == LEGACY_VERSION) {
                    callback.onHandle(Status.SUCCESS, handle)
                } else {
                    callback.onResults(Status.SUCCESS, InitResults().also { it.handle = handle })
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                try {
                    if (params.version == LEGACY_VERSION) {
                        callback.onHandle(Status.INTERNAL_ERROR, null)
                    } else {
                        callback.onResults(Status.INTERNAL_ERROR, InitResults())
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }

    }

    override fun execute2(callback: IExecuteCallback, params: ExecuteParams) {
        Log.d(TAG, "execute($params)")
        lifecycleScope.launchWhenStarted {
            try {
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
                val token = lastToken ?: runInit(params.handle.siteKey, params.version).token!!
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
                val data = RecaptchaResultData(response.token)
                if (params.version == LEGACY_VERSION) {
                    callback.onData(Status.SUCCESS, data)
                } else {
                    callback.onResults(Status.SUCCESS, ExecuteResults().also { it.data = data })
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                try {
                    if (params.version == LEGACY_VERSION) {
                        callback.onData(Status.INTERNAL_ERROR, null)
                    } else {
                        callback.onResults(Status.INTERNAL_ERROR, ExecuteResults())
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }

    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags) {
            super.onTransact(code, data, reply, flags)
        }

    companion object {
        const val LEGACY_VERSION = "16.0.0"
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
