/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "RemoteGuardImpl"

class RemoteHandleImpl(private val context: Context, private val packageName: String) : IDroidGuardHandle.Stub() {
    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private val sessions = mutableMapOf<Long, MultiStepSession>()
    private val sessionIdSequence = AtomicLong(System.currentTimeMillis())

    data class MultiStepSession(
        val flow: String?,
        val request: DroidGuardResultsRequest,
        var currentStep: Int = 0,
        val initialData: MutableMap<Any?, Any?> = mutableMapOf(),
        val pendingStepData: MutableMap<Int, Map<Any?, Any?>> = mutableMapOf()
    )

    private val url: String
        get() = DroidGuardPreferences.getNetworkServerUrl(context) ?: throw IllegalStateException("Network URL required")

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        this.flow = flow
    }

    override fun snapshot(map: Map<Any?, Any?>?): ByteArray {
        Log.d(TAG, "snapshot($map)")
        return doSnapshot(flow, request, map.orEmpty())
    }

    override fun begin(flow: String?, request: DroidGuardResultsRequest?, initialData: Map<Any?, Any?>?): Long {
        Log.d(TAG, "begin($flow, $request, $initialData)")
        val resolvedFlow = flow ?: this.flow
        val resolvedRequest = request?.copy() ?: this.request?.copy() ?: return -1
        val sessionId = sessionIdSequence.incrementAndGet()
        val session = MultiStepSession(
            flow = resolvedFlow,
            request = resolvedRequest,
            initialData = initialData?.toMutableMap() ?: mutableMapOf()
        )
        session.request.setSessionId(sessionId)
        session.request.setMultiStep(true)
        session.request.setStepNumber(0)
        sessions[sessionId] = session
        return sessionId
    }

    override fun nextStep(sessionId: Long, stepData: Map<Any?, Any?>?): DroidGuardInitReply {
        Log.d(TAG, "nextStep($sessionId, $stepData)")
        val session = sessions[sessionId] ?: return DroidGuardInitReply(null, null)
        session.currentStep++
        session.pendingStepData[session.currentStep] = stepData.orEmpty()
        return DroidGuardInitReply(null, null)
    }

    override fun snapshotWithSession(sessionId: Long, map: MutableMap<Any?, Any?>): ByteArray {
        Log.d(TAG, "snapshotWithSession($sessionId, $map)")
        val session = sessions.remove(sessionId) ?: return byteArrayOf()
        val request = session.request
        val combinedMap = mutableMapOf<Any?, Any?>()
        combinedMap.putAll(session.initialData)
        for (step in session.pendingStepData.toSortedMap().values) {
            combinedMap.putAll(step)
        }
        combinedMap.putAll(map)
        request.setSessionId(sessionId)
        request.setStepNumber(session.currentStep)
        request.setMultiStep(true)
        request.setTotalSteps(request.getTotalSteps())
        return doSnapshot(session.flow, request, combinedMap)
    }

    private fun DroidGuardResultsRequest.copy(): DroidGuardResultsRequest {
        return DroidGuardResultsRequest().also {
            it.bundle.putAll(bundle)
        }
    }

    override fun closeSession(sessionId: Long) {
        Log.d(TAG, "closeSession($sessionId)")
        sessions.remove(sessionId)
    }

    private fun doSnapshot(flow: String?, request: DroidGuardResultsRequest?, map: Map<Any?, Any?>): ByteArray {
        val paramsMap = mutableMapOf("flow" to flow, "source" to packageName)
        for (key in request?.bundle?.keySet().orEmpty()) {
            request?.bundle?.get(key)?.let { paramsMap["x-request-$key"] = it.toString() }
        }
        val params = paramsMap.map { Uri.encode(it.key) + "=" + Uri.encode(it.value) }.joinToString("&")
        val connection = URL("$url?$params").openConnection() as HttpURLConnection
        val payload = map.map { Uri.encode(it.key as String) + "=" + Uri.encode(it.value as String) }.joinToString("&")
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
        sessions.clear()
        this.request = null
        this.flow = null
    }

    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply? {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        return null
    }
}
