/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.droidguard.DroidGuardHandle
import com.google.android.gms.tasks.Tasks
import com.squareup.wire.GrpcClient
import google.internal.communications.phonedeviceverification.v1.GetConsentRequest
import google.internal.communications.phonedeviceverification.v1.GetConsentResponse
import google.internal.communications.phonedeviceverification.v1.GetVerifiedPhoneNumbersRequest
import google.internal.communications.phonedeviceverification.v1.GetVerifiedPhoneNumbersResponse
import google.internal.communications.phonedeviceverification.v1.GrpcPhoneDeviceVerificationClient
import google.internal.communications.phonedeviceverification.v1.GrpcPhoneNumberClient
import google.internal.communications.phonedeviceverification.v1.ProceedRequest
import google.internal.communications.phonedeviceverification.v1.ProceedResponse
import google.internal.communications.phonedeviceverification.v1.SetConsentRequest
import google.internal.communications.phonedeviceverification.v1.SetConsentResponse
import google.internal.communications.phonedeviceverification.v1.SyncRequest
import google.internal.communications.phonedeviceverification.v1.SyncResponse
import okhttp3.OkHttpClient
import org.microg.gms.droidguard.DroidGuardClientImpl
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Encapsulates the gRPC transport and DroidGuard token lifecycle for Constellation RPCs.
 *
 * Holds mutable state (DG handle, gRPC channel) so must be used as a scoped instance
 * and closed when done. Typical usage:
 *
 *     ConstellationRpcClient(context, ...).use { rpc ->
 *         val consent = rpc.getConsent(request)
 *         val token = rpc.getDroidGuardToken("sync", currentIid)
 *         val sync = rpc.sync(syncRequest)
 *     }
 */
class ConstellationRpcClient(
    private val context: Context,
    apiKey: String,
    packageName: String,
    certSha1: String?,
    spatulaHeader: String?,
    private val iidHash: String,
) : Closeable {

    // ── gRPC transport ──────────────────────────────────────────────────

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        // Server takes 11-15s during MO SMS challenge flows; default 10s is too short.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Android-Package", packageName)
                .header("X-Android-Cert", certSha1 ?: "")
                .header("User-Agent", "grpc-java-cronet/1.79.0-SNAPSHOT")

            if (!spatulaHeader.isNullOrEmpty()) {
                requestBuilder.header("X-Goog-Spatula", spatulaHeader)
            }
            chain.proceed(
                requestBuilder.method(original.method, original.body).build()
            )
        }
        .build()

    private val grpcClient: GrpcClient = GrpcClient.Builder()
        .client(okHttpClient)
        .baseUrl("https://phonedeviceverification-pa.googleapis.com/")
        // Server rejects gzip-encoded protos with INVALID_ARGUMENT; disable compression.
        .minMessageToCompress(Long.MAX_VALUE)
        .build()

    private val verificationClient = GrpcPhoneDeviceVerificationClient(grpcClient)
    private val phoneNumberClient = GrpcPhoneNumberClient(grpcClient)

    // ── DroidGuard state ────────────────────────────────────────────────

    private val dgCachePrefs: SharedPreferences =
        context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)

    private var dgHandle: DroidGuardHandle? = null
    private var dgHandleFlow: String? = null

    fun resolveDroidGuardFlow(rpcMethod: String): String = "constellation_verify"

    private fun flowCacheKeys(flow: String): Triple<String, String, String> {
        if (flow == "constellation_verify") {
            return Triple("droidguard_token", "droidguard_token_ttl", "droidguard_token_iid")
        }
        val safeFlow = flow.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return Triple(
            "droidguard_token_$safeFlow",
            "droidguard_token_ttl_$safeFlow",
            "droidguard_token_iid_$safeFlow"
        )
    }

    // ── DG token cache ──────────────────────────────────────────────────

    /** Returns (cachedToken, expiryEpochMillis, cachedIid). */
    fun getCachedDroidGuardToken(flow: String): Triple<String?, Long, String?> {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        return Triple(
            dgCachePrefs.getString(tokenKey, null),
            dgCachePrefs.getLong(ttlKey, 0L),
            dgCachePrefs.getString(iidKey, null)
        )
    }

    /**
     * Cache DroidGuard token from server response.
     * @param expiryEpochMillis absolute epoch millis when this token expires
     */
    fun cacheDroidGuardToken(flow: String, token: String, expiryEpochMillis: Long, currentIid: String) {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        Log.d(TAG, "Caching DroidGuard token for flow '$flow'")
        dgCachePrefs.edit()
            .putString(tokenKey, token)
            .putLong(ttlKey, expiryEpochMillis)
            .putString(iidKey, currentIid)
            .apply()
    }

    /** Clear cached token on auth errors. */
    fun clearDroidGuardTokenCache(flow: String, reason: String) {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        Log.d(TAG, "Clearing DroidGuard token cache for '$flow'")
        dgCachePrefs.edit()
            .remove(tokenKey)
            .remove(ttlKey)
            .remove(iidKey)
            .apply()
    }

    // ── DG handle lifecycle ─────────────────────────────────────────────

    /**
     * Open a new DG handle or reuse the existing one if it matches [dgFlow].
     * Reuses a single handle per session, calling snapshot()
     * with different rpc bindings.
     */
    private fun openOrReuseDgHandle(dgFlow: String): DroidGuardHandle? {
        val existing = dgHandle
        if (existing != null && existing.isOpened() && dgHandleFlow == dgFlow) {
            return existing
        }
        // Close stale handle if any
        if (existing != null) {
            try { existing.close() } catch (_: Exception) {}
            dgHandle = null
            dgHandleFlow = null
        }
        Log.d(TAG, "Opening DroidGuard handle")
        val droidGuard = DroidGuardClientImpl(context)
        val handleTask = droidGuard.init(dgFlow, null)
        return try {
            val handle = Tasks.await(handleTask, 30, TimeUnit.SECONDS)
            dgHandle = handle
            dgHandleFlow = dgFlow
            handle
        } catch (e: Exception) {
            Log.e(TAG, "DG handle init failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // ── getDroidGuardToken ──────────────────────────────────────────────

    /**
     * Get a DroidGuard token for the given RPC method.
     *
     * Checks the cache first, then generates a fresh token via the DG VM.
     * Each RPC method requires its own token with matching lowercase method name binding.
     * DroidGuard HMAC-binds the token to these inputs.
     *
     * @param rpcMethod lowercase RPC name (e.g. "sync", "getConsent", "proceed")
     * @param currentIid the current IID token, used for cache invalidation on IID change
     */
    fun getDroidGuardToken(rpcMethod: String, currentIid: String): String? {
        val dgFlow = resolveDroidGuardFlow(rpcMethod)
        if (dgFlow != "constellation_verify") {
            Log.w(TAG, "DG flow experiment ACTIVE: rpc=$rpcMethod flow=$dgFlow")
        }

        // Step 1: Check cache first (like GMS does)
        val (cachedToken, cachedExpiry, cachedIid) = getCachedDroidGuardToken(dgFlow)
        val now = System.currentTimeMillis()

        if (cachedToken != null && cachedExpiry > 0) {
            if (cachedIid != null && cachedIid != currentIid) {
                Log.w(TAG, "DG cache invalidated for $rpcMethod: IID changed")
                clearDroidGuardTokenCache(dgFlow, "IID token changed")
            } else {
                if (cachedExpiry > now) {
                    Log.d(TAG, "DG cache hit for $rpcMethod")
                    return cachedToken
                } else {
                    clearDroidGuardTokenCache(dgFlow, "TTL expired")
                }
            }
        }

        // Generate fresh token via reused DG handle
        val handle = openOrReuseDgHandle(dgFlow) ?: return null
        val dgBindings = mapOf(
            "iidHash" to iidHash,
            "rpc" to rpcMethod
        )
        return try {
            val token = handle.snapshot(dgBindings)
            if (token != null) {
                Log.i("MicroGRcs", "constellation DG=${token.length}chars rpc=$rpcMethod")
            } else {
                Log.e(TAG, "DroidGuard VM returned NULL for $rpcMethod")
            }
            token
        } catch (e: Exception) {
            Log.e(TAG, "DroidGuard VM failed for $rpcMethod: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // ── gRPC RPCs ───────────────────────────────────────────────────────

    suspend fun getConsent(request: GetConsentRequest): GetConsentResponse {
        return verificationClient.GetConsent().execute(request)
    }

    suspend fun setConsent(request: SetConsentRequest): SetConsentResponse {
        return verificationClient.SetConsent().execute(request)
    }

    suspend fun sync(request: SyncRequest): SyncResponse {
        return verificationClient.Sync().execute(request)
    }

    suspend fun proceed(request: ProceedRequest): ProceedResponse {
        return verificationClient.Proceed().execute(request)
    }

    suspend fun getVerifiedPhoneNumbers(request: GetVerifiedPhoneNumbersRequest): GetVerifiedPhoneNumbersResponse {
        return phoneNumberClient.GetVerifiedPhoneNumbers().execute(request)
    }

    // ── Closeable ───────────────────────────────────────────────────────

    override fun close() {
        dgHandle?.let {
            try { it.close() } catch (_: Exception) {}
        }
        dgHandle = null
        dgHandleFlow = null
    }

    companion object {
        private const val TAG = "GmsConstellationRpc"
    }
}
