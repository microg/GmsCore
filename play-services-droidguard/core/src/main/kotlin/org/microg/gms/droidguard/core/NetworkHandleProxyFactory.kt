/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.of
import org.microg.gms.droidguard.*
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.utils.singleInstanceOf
import java.io.File
import java.util.*
import com.android.volley.Request as VolleyRequest
import com.android.volley.Response as VolleyResponse

class NetworkHandleProxyFactory(private val context: Context) : HandleProxyFactory(context) {
    private val dgDb: DgDatabaseHelper = DgDatabaseHelper(context)
    private val version = VersionUtil(context)
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    fun createHandle(packageName: String, flow: String?, callback: GuardCallback, request: DroidGuardResultsRequest?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = readFromDatabase(flow) ?: fetchFromServer(flow, packageName)
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, request)
    }

    fun createPingHandle(packageName: String, flow: String, callback: GuardCallback, pingData: PingData?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = fetchFromServer(flow, createRequest(flow, packageName, pingData))
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, DroidGuardResultsRequest().also { it.clientVersion = 0 })
    }

    fun createLowLatencyHandle(flow: String?, callback: GuardCallback, request: DroidGuardResultsRequest?): HandleProxy {
        if (!DroidGuardPreferences.isLocalAvailable(context)) throw IllegalAccessException("DroidGuard should not be available locally")
        val (vmKey, byteCode, bytes) = readFromDatabase("fast") ?: throw Exception("low latency (fast) flow not available")
        return createHandleProxy(flow, vmKey, byteCode, bytes, callback, request)
    }

    fun SignedResponse.unpack(): Response {
        if (SignatureVerifier.verifySignature(data_!!.toByteArray(), signature!!.toByteArray())) {
            return Response.ADAPTER.decode(data_!!)
        } else {
            throw SecurityException("Signature invalid")
        }
    }

    private fun readFromDatabase(flow: String?): Triple<String, ByteArray, ByteArray>? {
        ProfileManager.ensureInitialized(context)
        val id = "$flow/${version.versionString}/${Build.FINGERPRINT}"
        return dgDb.get(id)
    }

    fun createRequest(flow: String?, packageName: String, pingData: PingData? = null, extra: ByteArray? = null): Request {
        ProfileManager.ensureInitialized(context)
        return Request(
                usage = Usage(flow, packageName),
                info = listOf(
                        KeyValuePair("BOARD", Build.BOARD),
                        KeyValuePair("BOOTLOADER", Build.BOOTLOADER),
                        KeyValuePair("BRAND", Build.BRAND),
                        KeyValuePair("CPU_ABI", Build.CPU_ABI),
                        KeyValuePair("CPU_ABI2", Build.CPU_ABI2),
                        KeyValuePair("SUPPORTED_ABIS", Build.SUPPORTED_ABIS.joinToString(",")),
                        KeyValuePair("DEVICE", Build.DEVICE),
                        KeyValuePair("DISPLAY", Build.DISPLAY),
                        KeyValuePair("FINGERPRINT", Build.FINGERPRINT),
                        KeyValuePair("HARDWARE", Build.HARDWARE),
                        KeyValuePair("HOST", Build.HOST),
                        KeyValuePair("ID", Build.ID),
                        KeyValuePair("MANUFACTURER", Build.MANUFACTURER),
                        KeyValuePair("MODEL", Build.MODEL),
                        KeyValuePair("PRODUCT", Build.PRODUCT),
                        KeyValuePair("RADIO", Build.RADIO),
                        KeyValuePair("SERIAL", Build.SERIAL),
                        KeyValuePair("TAGS", Build.TAGS),
                        KeyValuePair("TIME", Build.TIME.toString()),
                        KeyValuePair("TYPE", Build.TYPE),
                        KeyValuePair("USER", Build.USER),
                        KeyValuePair("VERSION.CODENAME", Build.VERSION.CODENAME),
                        KeyValuePair("VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL),
                        KeyValuePair("VERSION.RELEASE", Build.VERSION.RELEASE),
                        KeyValuePair("VERSION.SDK", Build.VERSION.SDK),
                        KeyValuePair("VERSION.SDK_INT", Build.VERSION.SDK_INT.toString()),
                ),
                versionName = version.versionString,
                versionCode = BuildConfig.VERSION_CODE,
                hasAccount = false,
                isGoogleCn = false,
                enableInlineVm = true,
                cached = getCacheDir().list()?.map { it.decodeHex() }.orEmpty(),
                arch = System.getProperty("os.arch"),
                ping = pingData,
                field10 = extra?.let { of(*it) },
        )
    }

    fun fetchFromServer(flow: String?, packageName: String): Triple<String, ByteArray, ByteArray> {
        return fetchFromServer(flow, createRequest(flow, packageName))
    }

    fun fetchFromServer(flow: String?, request: Request): Triple<String, ByteArray, ByteArray> {
        ProfileManager.ensureInitialized(context)
        val future = RequestFuture.newFuture<SignedResponse>()
        queue.add(object : VolleyRequest<SignedResponse>(Method.POST, SERVER_URL, future) {
            override fun parseNetworkResponse(response: NetworkResponse): VolleyResponse<SignedResponse> {
                return try {
                    VolleyResponse.success(SignedResponse.ADAPTER.decode(response.data), null)
                } catch (e: Exception) {
                    VolleyResponse.error(VolleyError(e))
                }
            }

            override fun deliverResponse(response: SignedResponse) {
                future.onResponse(response)
            }

            override fun getBody(): ByteArray = request.encode()

            override fun getBodyContentType(): String = "application/x-protobuf"

            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "User-Agent" to "DroidGuard/${version.versionCode}"
                )
            }
        })
        val signed: SignedResponse = future.get()
        val response = signed.unpack()
        val vmKey = response.vmChecksum!!.hex()
        if (!isValidCache(vmKey)) {
            val temp = File(getCacheDir(), "${UUID.randomUUID()}.apk")
            temp.parentFile!!.mkdirs()
            temp.writeBytes(response.content!!.toByteArray())
            getOptDir(vmKey).mkdirs()
            temp.renameTo(getTheApkFile(vmKey))
            updateCacheTimestamp(vmKey)
            if (!isValidCache(vmKey)) {
                getCacheDir(vmKey).deleteRecursively()
                throw IllegalStateException()
            }
        }
        val id = "$flow/${version.versionString}/${Build.FINGERPRINT}"
        val expiry = (response.expiryTimeSecs ?: 0).toLong()
        val byteCode = response.byteCode?.toByteArray() ?: ByteArray(0)
        val extra = response.extra?.toByteArray() ?: ByteArray(0)
        if (response.save != false) {
            dgDb.put(id, expiry, vmKey, byteCode, extra)
        }
        return Triple(vmKey, byteCode, extra)
    }
    
    // Multi-step session support
    private val multiStepSessions = mutableMapOf<Long, MultiStepSession>()
    
    data class MultiStepSession(
        val sessionId: Long,
        val flow: String,
        val vmKey: String,
        val byteCode: ByteArray,
        val extra: ByteArray,
        var currentStep: Int = 0,
        var totalSteps: Int = 1,
        var intermediateResults: MutableMap<Int, ByteArray> = mutableMapOf()
    )
    
    fun beginMultiStep(flow: String, request: DroidGuardResultsRequest, initialData: Map<String, String>): Long {
        val sessionId = System.currentTimeMillis() + (Math.random() * 1000).toLong()
        
        // Create initial request with multi-step flag
        val modifiedRequest = request.setMultiStep(true)
            .setSessionId(sessionId)
            .setStepNumber(0)
            .setTotalSteps(initialData["total_steps"]?.toIntOrNull() ?: 1)
        
        // Fetch VM and bytecode for this session
        val (vmKey, byteCode, extra) = fetchFromServer(flow, createRequest(flow, "multi-step", null, null))
        
        // Store session
        multiStepSessions[sessionId] = MultiStepSession(
            sessionId = sessionId,
            flow = flow,
            vmKey = vmKey,
            byteCode = byteCode,
            extra = extra,
            currentStep = 0,
            totalSteps = modifiedRequest.getTotalSteps()
        )
        
        return sessionId
    }
    
    fun nextStepMultiStep(sessionId: Long, stepData: Map<String, String>): DroidGuardInitReply? {
        val session = multiStepSessions[sessionId] ?: return null
        
        // Update session state
        session.currentStep++
        session.intermediateResults[session.currentStep] = stepData.toString().toByteArray()
        
        // For remote DroidGuard, we need to send the step data to the server
        // This is a simplified implementation
        return DroidGuardInitReply(null, null) // Placeholder
    }
    
    fun snapshotWithSession(sessionId: Long, data: Map<String, String>): ByteArray {
        val session = multiStepSessions[sessionId] ?: return byteArrayOf()
        
        // Combine all intermediate results for final snapshot
        val allData = mutableMapOf<String, String>()
        allData.putAll(data)
        allData["session_id"] = sessionId.toString()
        allData["current_step"] = session.currentStep.toString()
        allData["total_steps"] = session.totalSteps.toString()
        
        // Create handle proxy for this session
        val callback = GuardCallback(context, "multi-step")
        val request = DroidGuardResultsRequest()
            .setMultiStep(true)
            .setSessionId(sessionId)
            .setStepNumber(session.currentStep)
            .setTotalSteps(session.totalSteps)
        
        val handle = createHandleProxy(session.flow, session.vmKey, session.byteCode, session.extra, callback, request)
        // Convert Map<String, String> to Map<Any, Any> for HandleProxy.run()
        val anyMap = allData.map { (k, v) -> k as Any to v as Any }.toMap()
        return handle.run(anyMap)
    }
    
    fun closeSession(sessionId: Long) {
        multiStepSessions.remove(sessionId)
    }

    private fun createHandleProxy(
        flow: String?,
        vmKey: String,
        byteCode: ByteArray,
        extra: ByteArray,
        callback: GuardCallback,
        request: DroidGuardResultsRequest?
    ): HandleProxy {
        ProfileManager.ensureInitialized(context)
        val clazz = loadClass(vmKey, extra)
        
        // Check if this is a multi-step request
        val isMultiStep = request?.isMultiStep() == true
        val sessionId = request?.getSessionId() ?: -1L
        
        return if (isMultiStep && sessionId > 0 && request != null) {
            // Create multi-step handle proxy
            createMultiStepHandleProxy(clazz, context, flow, byteCode, callback, vmKey, extra, request, sessionId)
        } else {
            // Original single-step handle proxy
            HandleProxy(clazz, context, flow, byteCode, callback, vmKey, extra, request?.bundle)
        }
    }
    
    private fun createMultiStepHandleProxy(
        clazz: Class<*>,
        context: Context,
        flow: String?,
        byteCode: ByteArray,
        callback: GuardCallback,
        vmKey: String,
        extra: ByteArray,
        request: DroidGuardResultsRequest,
        sessionId: Long
    ): HandleProxy {
        // For multi-step, we need to maintain session state
        // This is a simplified implementation - in reality, we'd need to:
        // 1. Store session state (step number, intermediate results, etc.)
        // 2. Modify the bundle to include multi-step information
        // 3. Handle the multi-step protocol with the remote server
        
        val modifiedBundle = request.bundle?.apply {
            // Add multi-step information to the bundle
            // These will be converted to x-request-* query parameters by the remote server
            putString("x-request-session-id", sessionId.toString())
            putString("x-request-step-number", request.getStepNumber().toString())
            putString("x-request-total-steps", request.getTotalSteps().toString())
            putString("x-request-is-multi-step", "true")
        }
        
        return HandleProxy(clazz, context, flow, byteCode, callback, vmKey, extra, modifiedBundle)
    }

    companion object {
        const val SERVER_URL = "https://www.googleapis.com/androidantiabuse/v1/x/create?alt=PROTO&key=AIzaSyBofcZsgLSS7BOnBjZPEkk4rYwzOIz-lTI"
    }
}
