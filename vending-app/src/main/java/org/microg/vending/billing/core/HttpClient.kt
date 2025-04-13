package org.microg.vending.billing.core

import android.content.Context
import android.net.Uri
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersImpl
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.pool.ByteArrayPool
import org.json.JSONObject
import org.microg.gms.utils.singleInstanceOf
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

private const val POST_TIMEOUT = 8000L

class HttpClient {

    private val client = singleInstanceOf { HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout)
    } }

    private val clientWithCache = singleInstanceOf { HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpCache)
        install(HttpTimeout)
    } }

    suspend fun download(
        url: String,
        downloadFile: File,
        params: Map<String, String> = emptyMap()
    ): File = downloadFile.also { toFile ->
        val parentDir = downloadFile.getParentFile()
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw IOException("Failed to create directories: ${parentDir.absolutePath}")
        }

        FileOutputStream(toFile).use { download(url, it, params) }
    }

    suspend fun download(
        url: String,
        downloadTo: OutputStream,
        params: Map<String, String> = emptyMap(),
        emitProgress: (bytesDownloaded: Long) -> Unit = {}
    ) {
        client.prepareGet(url.asUrl(params)).execute { response ->
            val body: ByteReadChannel = response.body()

            // Modified version of `ByteReadChannel.copyTo(OutputStream, Long)` to indicate progress
            val buffer = ByteArrayPool.borrow()
            try {
                var copied = 0L
                val bufferSize = buffer.size

                do {
                    val rc = body.readAvailable(buffer, 0, bufferSize)
                    copied += rc
                    if (rc > 0) {
                        downloadTo.write(buffer, 0, rc)
                        emitProgress(copied)
                    }
                } while (rc > 0)
            } finally {
                ByteArrayPool.recycle(buffer)
            }
            // don't close `downloadTo` yet
        }
    }

    suspend fun <O> get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        adapter: ProtoAdapter<O>,
        cache: Boolean = true
    ): O {

        val response = (if (cache) clientWithCache else client).get(url.asUrl(params)) {
            headers {
                headers.forEach {
                    append(it.key, it.value)
                }
            }
        }
        if (response.status != HttpStatusCode.OK) throw IOException("Server responded with status ${response.status}")
        else return adapter.decode(response.body<ByteArray>())
    }

    /**
     * Post empty body.
     */
    suspend fun <I : Message<I, *>, O> post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        adapter: ProtoAdapter<O>,
        cache: Boolean = false
    ): O {
        val response = (if (cache) clientWithCache else client).post(url.asUrl(params)) {
            setBody(ByteArray(0))
            headers {
                headers.forEach {
                    append(it.key, it.value)
                }

                append(HttpHeaders.ContentType, "application/x-protobuf")
            }
            timeout {
                requestTimeoutMillis = POST_TIMEOUT
            }
        }
        return adapter.decode(response.body<ByteArray>())
    }

    /**
     * Post protobuf-encoded body.
     */
    suspend fun <I : Message<I, *>, O> post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        payload: I,
        adapter: ProtoAdapter<O>,
        cache: Boolean = false
    ): O {
        val response = (if (cache) clientWithCache else client).post(url.asUrl(params)) {
            setBody(ByteReadChannel(payload.encode()))
            headers {
                headers.forEach {
                    append(it.key, it.value)
                }

                append(HttpHeaders.ContentType, "application/x-protobuf")
            }
            timeout {
                requestTimeoutMillis = POST_TIMEOUT
            }
        }
        return adapter.decode(response.body<ByteArray>())
    }

    /**
     * Post JSON body.
     */
    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        payload: JSONObject,
        cache: Boolean = false
    ): JSONObject {
        val response = (if (cache) clientWithCache else client).post(url.asUrl(params)) {
            setBody(payload.toString())
            headers {
                headers.forEach {
                    append(it.key, it.value)
                }

                append(HttpHeaders.ContentType, "application/json")
            }
            timeout {
                requestTimeoutMillis = POST_TIMEOUT
            }
        }
        return JSONObject(response.body<String>())
    }

    /**
     * Post form body.
     */
    suspend fun <O> post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        form: Map<String, String> = emptyMap(),
        adapter: ProtoAdapter<O>,
        cache: Boolean = false
    ): O {
        val response = (if (cache) clientWithCache else client).submitForm(
            formParameters = ParametersImpl(form.mapValues { listOf(it.key) }),
            encodeInQuery = false
        ) {
            url(url.asUrl(params))
            headers { // Content-Type is set to `x-www-form-urlencode` automatically
                headers.forEach {
                    append(it.key, it.value)
                }
            }
            timeout {
                requestTimeoutMillis = POST_TIMEOUT
            }
        }
        return adapter.decode(response.body<ByteArray>())
    }

    private fun String.asUrl(params: Map<String, String>): Url = URLBuilder(this).apply {
        params.forEach {
            parameters.append(it.key, it.value)
        }
    }.build()
}