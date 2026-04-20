/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
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
        // Stock GMS uses 60s timeouts (decompiled). OkHttp default is 10s.
        // Server regularly takes 11-15s during MO SMS challenge flows.
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Android-Package", packageName)
                .header("X-Android-Cert", certSha1 ?: "")
                // Stock GMS user-agent: "grpc-java-cronet/1.79.0-SNAPSHOT" (imcx.java:189-192)
                .header("User-Agent", "grpc-java-cronet/1.79.0-SNAPSHOT")
            // Stock GMS sends X-Goog-Spatula on every Constellation RPC (beeb.java:217-219)
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
        // Stock GMS (Cronet gRPC) does NOT compress outgoing messages.
        // Wire default minMessageToCompress=0 sends grpc-encoding:gzip on ALL requests.
        // Server may reject gzip-encoded protos with INVALID_ARGUMENT.
        .minMessageToCompress(Long.MAX_VALUE)
        .build()

    private val verificationClient = GrpcPhoneDeviceVerificationClient(grpcClient)
    private val phoneNumberClient = GrpcPhoneNumberClient(grpcClient)

    // ── DroidGuard state ────────────────────────────────────────────────

    private val dgCachePrefs: SharedPreferences =
        context.getSharedPreferences(ConstellationConstants.PREFS_CONSTELLATION, Context.MODE_PRIVATE)

    private var dgHandle: DroidGuardHandle? = null
    private var dgHandleFlow: String? = null

    // DG flow override knobs (read once at construction)
    private val globalFlowOverride: String?
    private val rpcFlowOverrides: Map<String, String>

    init {
        globalFlowOverride = Settings.Global.getString(
            context.contentResolver,
            DG_FLOW_OVERRIDE_GLOBAL_KEY
        )?.trim()?.takeIf { it.isNotEmpty() }

        rpcFlowOverrides = parseRpcFlowOverrides(
            Settings.Global.getString(context.contentResolver, DG_FLOW_OVERRIDE_RPC_MAP_KEY)
        )

        if (globalFlowOverride != null) {
            Log.w(TAG, "DG flow experiment: global override enabled '$globalFlowOverride'")
        }
        if (rpcFlowOverrides.isNotEmpty()) {
            Log.w(TAG, "DG flow experiment: per-RPC override map enabled $rpcFlowOverrides")
        }
    }

    // ── DG flow resolution ──────────────────────────────────────────────

    fun resolveDroidGuardFlow(rpcMethod: String): String {
        return rpcFlowOverrides[rpcMethod]
            ?: globalFlowOverride
            ?: "constellation_verify"
    }

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

    /** Returns (cachedToken, ttlMillis, cachedIid). */
    fun getCachedDroidGuardToken(flow: String): Triple<String?, Long, String?> {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        return Triple(
            dgCachePrefs.getString(tokenKey, null),
            dgCachePrefs.getLong(ttlKey, 0L),
            dgCachePrefs.getString(iidKey, null)
        )
    }

    /**
     * Cache DroidGuard token from server response (GMS bewt.java:1111-1128).
     * Also stores the IID token used to generate this DG token so we can
     * invalidate the cache if the IID changes (DG tokens are session-bound via iidHash).
     */
    fun cacheDroidGuardToken(flow: String, token: String, ttlMillis: Long, currentIid: String) {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        Log.i(TAG, "Caching DroidGuard token for flow '$flow': ${token.length} chars, TTL=${java.util.Date(ttlMillis)}, IID=${currentIid.take(20)}...")
        dgCachePrefs.edit()
            .putString(tokenKey, token)
            .putLong(ttlKey, ttlMillis)
            .putString(iidKey, currentIid)
            .apply()
    }

    /** Clear cached token on auth errors (GMS bewt.java:180-191). */
    fun clearDroidGuardTokenCache(flow: String, reason: String) {
        val (tokenKey, ttlKey, iidKey) = flowCacheKeys(flow)
        Log.w(TAG, "Clearing DroidGuard token cache for flow '$flow': $reason")
        dgCachePrefs.edit()
            .remove(tokenKey)
            .remove(ttlKey)
            .remove(iidKey)
            .apply()
    }

    // ── DG handle lifecycle ─────────────────────────────────────────────

    /**
     * Open a new DG handle or reuse the existing one if it matches [dgFlow].
     * Stock GMS (bfox.java) creates ONE handle per session and calls snapshot()
     * with different rpc bindings.
     */
    private fun openOrReuseDgHandle(dgFlow: String): DroidGuardHandle? {
        val existing = dgHandle
        if (existing != null && existing.isOpened() && dgHandleFlow == dgFlow) {
            Log.d(TAG, "DG handle REUSE (flow=$dgFlow)")
            return existing
        }
        // Close stale handle if any
        if (existing != null) {
            try { existing.close() } catch (_: Exception) {}
            dgHandle = null
            dgHandleFlow = null
        }
        Log.i(TAG, "DG handle OPEN (flow=$dgFlow) - calling DroidGuardClientImpl.init()")
        val droidGuard = DroidGuardClientImpl(context)
        val handleTask = droidGuard.init(dgFlow, null)
        return try {
            Log.i(TAG, "DG Tasks.await() START (30s timeout)")
            val handle = Tasks.await(handleTask, 30, TimeUnit.SECONDS)
            Log.i(TAG, "DG Tasks.await() COMPLETED - handle=${handle?.javaClass?.simpleName}")
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
     * CRITICAL: Each API method REQUIRES its own token with matching RPC binding.
     * GMS bewt.java passes LOWERCASE METHOD NAME as "rpc" binding:
     *   bewt.java:480 -> "getConsent"
     *   bewt.java:694 -> "sync"
     * DroidGuard HMAC-binds the token to these inputs.
     *
     * @param rpcMethod lowercase RPC name (e.g. "sync", "getConsent", "proceed")
     * @param currentIid the current IID token, used for cache invalidation on IID change
     */
    fun getDroidGuardToken(rpcMethod: String, currentIid: String): String? {
        Log.d(TAG, "getDroidGuardToken($rpcMethod) called")

        val dgFlow = resolveDroidGuardFlow(rpcMethod)
        if (dgFlow != "constellation_verify") {
            Log.w(TAG, "DG flow experiment ACTIVE: rpc=$rpcMethod flow=$dgFlow")
        }

        // Step 1: Check cache first (like GMS does)
        val (cachedToken, cachedTtl, cachedIid) = getCachedDroidGuardToken(dgFlow)
        val now = System.currentTimeMillis()

        if (cachedToken != null && cachedTtl > 0) {
            if (cachedIid != null && cachedIid != currentIid) {
                Log.w(TAG, "DG cache invalidated for $rpcMethod: IID changed")
                clearDroidGuardTokenCache(dgFlow, "IID token changed")
            } else {
                val ttlRemaining = cachedTtl - now
                if (ttlRemaining > 0) {
                    Log.d(TAG, "DG cache HIT for $rpcMethod: ${cachedToken.length} chars, TTL ~${ttlRemaining / 3600000}h")
                    return cachedToken
                } else {
                    Log.d(TAG, "DG cache EXPIRED for $rpcMethod")
                    clearDroidGuardTokenCache(dgFlow, "TTL expired")
                }
            }
        }

        // Generate fresh token via reused DG handle (matches stock bfox pattern)
        Log.d(TAG, "DG VM call: rpc=$rpcMethod flow=$dgFlow")

        val handle = openOrReuseDgHandle(dgFlow) ?: return null
        val dgBindings = mapOf(
            "iidHash" to iidHash,
            "rpc" to rpcMethod
        )
        return try {
            val token = handle.snapshot(dgBindings)
            if (token != null) {
                Log.i("MicroGRcs", "constellation DG=${token.length}chars rpc=$rpcMethod")
                Log.d(TAG, "DG token for $rpcMethod: ${token.length} chars, prefix=${token.take(10)}...")
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
            Log.d(TAG, "DG handle CLOSED")
        }
        dgHandle = null
        dgHandleFlow = null
    }

    companion object {
        private const val TAG = "GmsConstellationRpc"

        private const val DG_FLOW_OVERRIDE_GLOBAL_KEY = "microg_constellation_dg_flow_override"
        private const val DG_FLOW_OVERRIDE_RPC_MAP_KEY = "microg_constellation_dg_flow_rpc_map"

        private fun parseRpcFlowOverrides(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            val out = mutableMapOf<String, String>()
            raw.split(',', ';', '\n').forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isEmpty()) return@forEach
                val idx = trimmed.indexOf('=')
                if (idx <= 0 || idx >= trimmed.length - 1) return@forEach
                val rpc = trimmed.substring(0, idx).trim()
                val flow = trimmed.substring(idx + 1).trim()
                if (rpc.isNotEmpty() && flow.isNotEmpty()) {
                    out[rpc] = flow
                }
            }
            return out
        }
    }
}
