/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RemoteGuardImpl"

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        this.flow = flow
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")
        val paramsMap = mutableMapOf("flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.getString(key)?.let { paramsMap["x-request-$key"] = it }
        }
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        val payload = map.orEmpty().map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
        Log.d(TAG, "POST ${connection.url}: $payload")
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        connection.outputStream.use { it.write(payload.encodeToByteArray()) }
        val bytes = connection.inputStream.use { it.readBytes() }.decodeToString()
        return Base64.decode(bytes, Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING)
    }

    override fun close() {
        Log.d(TAG, "close()")
        this.request = null
        this.flow = null
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        return null
    }

    // Multi-step session support
    private val multiStepSessions = mutableMapOf<Long, MultiStepSession>()
    
    data class MultiStepSession(
        val sessionId: Long,
        val flow: String?,
        var currentStep: Int = 0,
        var totalSteps: Int = 1,
        var stepData: MutableMap<Int, Map<Any?, Any?>> = mutableMapOf()
    )

    override fun begin(flow: String?, request: DroidGuardResultsRequest?, initialData: Map<Any?, Any?>?): Long {
        Log.d(TAG, "begin($flow, $request, $initialData)")
        val sessionId = System.currentTimeMillis() + (Math.random() * 1000).toLong()
        val totalSteps = request?.getTotalSteps() ?: 1
        
        multiStepSessions[sessionId] = MultiStepSession(
            sessionId = sessionId,
            flow = flow,
            currentStep = 0,
            totalSteps = totalSteps
        )
        
        // Store initial data if provided
        initialData?.let { multiStepSessions[sessionId]?.stepData?.put(0, it) }
        
        return sessionId
    }

    override fun nextStep(sessionId: Long, stepData: Map<Any?, Any?>?): DroidGuardInitReply? {
        Log.d(TAG, "nextStep($sessionId, $stepData)")
        val session = multiStepSessions[sessionId] ?: return null
        
        session.currentStep++
        stepData?.let { session.stepData[session.currentStep] = it }
        
        // For remote DroidGuard, we need to send the step data to the server
        // This implementation returns null, indicating no more init steps required
        // In a real implementation, this would communicate with the remote server
        return null
    }

    override fun snapshotWithSession(sessionId: Long, map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshotWithSession($sessionId, $map)")
        val session = multiStepSessions[sessionId] ?: return byteArrayOf()
        
        // Combine all step data for final snapshot
        val allData = mutableMapOf<Any?, Any?>()
        session.stepData.values.forEach { allData.putAll(it) }
        map?.let { allData.putAll(it) }
        
        // Add session metadata
        allData["session_id"] = sessionId.toString()
        allData["current_step"] = session.currentStep.toString()
        allData["total_steps"] = session.totalSteps.toString()
        allData["is_multi_step"] = "true"
        
        // Use the original snapshot method with combined data
        return snapshot(allData)
    }

    override fun closeSession(sessionId: Long) {
        Log.d(TAG, "closeSession($sessionId)")
        multiStepSessions.remove(sessionId)
    }
}