/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.snapshot

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.google.android.gms.common.BuildConfig
import com.squareup.wire.GrpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.games.CommitSnapshotRevisionRequest
import org.microg.gms.games.DeleteSnapshotInfo
import org.microg.gms.games.EmptyResult
import org.microg.gms.games.GetSnapshotRequest
import org.microg.gms.games.SnapshotsExtendedClient
import org.microg.gms.games.GetSnapshotResponse
import org.microg.gms.games.PrepareSnapshotRevisionRequest
import org.microg.gms.games.PrepareSnapshotRevisionResponse
import org.microg.gms.games.ResolveSnapshotHeadRequest
import org.microg.gms.games.ResolveSnapshotHeadResponse
import org.microg.gms.games.requestGamesInfo
import org.microg.gms.profile.Build
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "SnapshotsApiClient"

/**
 * https://developers.google.com/games/services/web/api/rest#rest-resource:-snapshots
 *
 * Google Play Games Services can only support obtaining snapshot lists.
 * There is no interface for saving or deleting snapshots.
 */
object SnapshotsApiClient {

    private const val POST_TIMEOUT = 15000
    const val SNAPSHOT_UPLOAD_LINK_DATA = 1
    const val SNAPSHOT_UPLOAD_LINK_IMAGE = 2

    suspend fun prepareSnapshotRevision(context: Context, oauthToken: String,
                                        prepareSnapshotRevisionRequest: PrepareSnapshotRevisionRequest) : PrepareSnapshotRevisionResponse {
        val snapshotClient = getGrpcClient(context, oauthToken)
        return withContext(Dispatchers.IO) { snapshotClient.PrepareSnapshotRevision().execute(prepareSnapshotRevisionRequest) }
    }

    suspend fun uploadDataByUrl(oauthToken: String, url: String, requestQueue: RequestQueue, body: ByteArray): String = suspendCoroutine { continuation ->
        requestQueue.add(object : Request<String>(Method.PUT, url, null) {

            override fun deliverResponse(response: String) {
                Log.d(TAG, "deliverResponse: $response")
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError?) {
                error?.let {
                    continuation.resumeWithException(error)
                }
            }

            override fun getBody(): ByteArray {
                return body
            }

            override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                var result = ""
                try {
                    val json = JSONObject(response.data.toString(Charsets.UTF_8))
                    result = json.getString("resourceId")
                } catch (e: Exception) {
                    Log.w(TAG, "parseNetworkResponse: ", e)
                }
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["authorization"] = "OAuth $oauthToken"
                headers["x-goog-upload-command"] = "upload, finalize"
                headers["x-goog-upload-protocol"] = "resumable"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                headers["x-goog-upload-offset"] = "0"
                headers["User-Agent"]  = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};"
                return headers
            }
        }.setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }

    suspend fun getDataFromDrive(oauthToken: String, url: String, requestQueue: RequestQueue) : ByteArray = suspendCoroutine { continuation ->
        requestQueue.add(object : Request<ByteArray>(Method.GET, url, null) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> {
                return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun deliverResponse(response: ByteArray) {
                Log.d(TAG, "deliverResponse: $response")
                continuation.resume(response)
            }

            override fun deliverError(error: VolleyError?) {
                error?.let {
                    continuation.resumeWithException(error)
                }
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["authorization"] = "OAuth $oauthToken"
                headers["User-Agent"]  = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};"
                return headers
            }

        }.setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }

    suspend fun getRealUploadUrl(oauthToken: String, url: String, requestQueue: RequestQueue) : String = suspendCoroutine { continuation ->
        requestQueue.add(object : Request<String>(Method.POST, url, null) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                val responseHeaders = response.headers
                return Response.success(responseHeaders?.get("X-Goog-Upload-URL"), HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun deliverResponse(response: String?) {
                Log.d(TAG, "deliverResponse: $response")
                continuation.resume(response?:"")
            }

            override fun deliverError(error: VolleyError?) {
                error?.let {
                    continuation.resumeWithException(error)
                }
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["authorization"] = "OAuth $oauthToken"
                headers["x-goog-upload-command"] = "start"
                headers["x-goog-upload-protocol"] = "resumable"
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                headers["User-Agent"]  = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};"
                return headers
            }

        }.setRetryPolicy(DefaultRetryPolicy(POST_TIMEOUT, 0, 0.0F)))
    }

    /**
     *  Get the request content by capturing the packet.
     *  Currently only supports getting list data.
     */
    /**
     *  Get the request content by capturing the packet.
     *  Currently only supports getting list data.
     */
    suspend fun requestSnapshotList(context: Context, oauthToken: String): GetSnapshotResponse {
        val snapshotClient = getGrpcClient(context, oauthToken)
        val snapshotRequestBody = GetSnapshotRequest.Builder().apply {
            unknownFileIntList3 = listOf(2, 3, 1)
            unknownFileInt4 = 25
            unknownFileInt6 = 3
        }.build()
        return withContext(Dispatchers.IO) { snapshotClient.SyncSnapshots().execute(snapshotRequestBody) }
    }

    suspend fun deleteSnapshot(context: Context, oauthToken: String, snapshot: Snapshot): EmptyResult {
        val snapshotClient = getGrpcClient(context, oauthToken)
        val deleteSnapshotInfo = DeleteSnapshotInfo.Builder().apply {
            snapshotName = snapshot.title
            snapshotId = snapshot.id
        }.build()
        return withContext(Dispatchers.IO) { snapshotClient.DeleteSnapshot().execute(deleteSnapshotInfo) }
    }

    suspend fun resolveSnapshotHead(context: Context, oauthToken: String, resolveSnapshotHeadRequest: ResolveSnapshotHeadRequest): ResolveSnapshotHeadResponse {
        val snapshotClient = getGrpcClient(context, oauthToken)
        return withContext(Dispatchers.IO) { snapshotClient.ResolveSnapshotHead().execute(resolveSnapshotHeadRequest) }
    }

    suspend fun commitSnapshotRevision(context: Context, oauthToken: String,
                                       commitSnapshotRevisionRequest: CommitSnapshotRevisionRequest): EmptyResult {
        val snapshotClient = getGrpcClient(context, oauthToken)
        return withContext(Dispatchers.IO) { snapshotClient.CommitSnapshotRevision().execute(commitSnapshotRevisionRequest) }
    }

    private fun getGrpcClient(context: Context, oauthToken: String) : SnapshotsExtendedClient {
        val client = OkHttpClient().newBuilder().addInterceptor(
            HeaderInterceptor(context, oauthToken)
        ).build()
        val grpcClient = GrpcClient.Builder().client(client).baseUrl("https://games.googleapis.com").build()
        return grpcClient.create(SnapshotsExtendedClient::class)
    }

    class HeaderInterceptor(
        private val context: Context,
        private val oauthToken: String,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("authorization", "Bearer $oauthToken")
                .header("te", "trailers")
                .header("x-play-games-agent", createPlayGamesAgent())
                .header("x-device-id", LastCheckinInfo.read(context).androidId.toString(16))
                .header("user-agent", "grpc-java-okhttp/1.66.0-SNAPSHOT")
            val request = requestBuilder.build()
            Log.d(TAG, "request: $request")
            return chain.proceed(request)
        }

        private fun createPlayGamesAgent(): String {
            var playGamesAgent =
                "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID};"
            playGamesAgent +=  context.packageName + "/" + BuildConfig.VERSION_CODE + ";"
            playGamesAgent += "FastParser/1.1; Games Android SDK/1.0-1052947;"
            playGamesAgent += "com.google.android.play.games/517322040; (gzip); Games module/242632000"
            return playGamesAgent
        }
    }
}
