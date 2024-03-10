package org.microg.vending.billing.core

import android.content.Context
import android.net.Uri
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import org.json.JSONObject
import org.microg.gms.utils.singleInstanceOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val POST_TIMEOUT = 15000

class HttpClient(context: Context) {
    private val requestQueue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    suspend fun <O> get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        adapter: ProtoAdapter<O>,
        cache: Boolean = true
    ): O = suspendCoroutine { continuation ->
        val uriBuilder = Uri.parse(url).buildUpon()
        params.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        requestQueue.add(object : Request<O>(Method.GET, uriBuilder.build().toString(), null) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
                if (response.statusCode != 200) throw VolleyError(response)
                return Response.success(adapter.decode(response.data), HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun deliverResponse(response: O) {
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError) {
                continuation.resumeWithException(error)
            }

            override fun getHeaders(): Map<String, String> = headers
        }.setShouldCache(cache))
    }



    suspend fun <I : Message<I, *>, O> post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        payload: I,
        adapter: ProtoAdapter<O>,
        cache: Boolean = false
    ): O = suspendCoroutine { continuation ->
        val uriBuilder = Uri.parse(url).buildUpon()
        params.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        requestQueue.add(object : Request<O>(Method.POST, uriBuilder.build().toString(), null) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
                if (response.statusCode != 200) throw VolleyError(response)
                return Response.success(adapter.decode(response.data), HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun deliverResponse(response: O) {
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError) {
                continuation.resumeWithException(error)
            }

            override fun getHeaders(): Map<String, String> = headers
            override fun getBody(): ByteArray = payload.encode()
            override fun getBodyContentType(): String = "application/x-protobuf"
        }.setShouldCache(cache).setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }

    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        payload: JSONObject,
        cache: Boolean = false
    ): JSONObject = suspendCoroutine { continuation ->
        val uriBuilder = Uri.parse(url).buildUpon()
        params.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        requestQueue.add(object : JsonObjectRequest(Method.POST, uriBuilder.build().toString(), payload, null, null) {

            override fun deliverResponse(response: JSONObject) {
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError) {
                continuation.resumeWithException(error)
            }

            override fun getHeaders(): Map<String, String> = headers
        }.setShouldCache(cache).setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }

    suspend fun <O> post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        form: Map<String, String> = emptyMap(),
        adapter: ProtoAdapter<O>,
        cache: Boolean = false
    ): O = suspendCoroutine { continuation ->
        val uriBuilder = Uri.parse(url).buildUpon()
        params.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        requestQueue.add(object : Request<O>(Method.POST, uriBuilder.build().toString(), null) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<O> {
                if (response.statusCode != 200) throw VolleyError(response)
                return Response.success(adapter.decode(response.data), HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun deliverResponse(response: O) {
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError) {
                continuation.resumeWithException(error)
            }

            override fun getHeaders(): Map<String, String> = headers
            override fun getParams(): Map<String, String> = form
        }.setShouldCache(cache).setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }
}