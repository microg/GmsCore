/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.ConditionVariable
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardInitReply
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle
import org.microg.gms.droidguard.BytesException
import org.microg.gms.droidguard.GuardCallback
import org.microg.gms.droidguard.HandleProxy
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicLong

class DroidGuardHandleImpl(private val context: Context, private val packageName: String, private val factory: NetworkHandleProxyFactory, private val callback: GuardCallback) : IDroidGuardHandle.Stub() {
    private val condition = ConditionVariable()

    private var flow: String? = null
    private var request: DroidGuardResultsRequest? = null
    private var handleProxy: HandleProxy? = null
    private var handleInitError: Throwable? = null
    private val sessions = mutableMapOf<Long, MultiStepSession>()
    private val sessionIdSequence = AtomicLong(System.currentTimeMillis())

    data class MultiStepSession(
        val flow: String?,
        val request: DroidGuardResultsRequest?,
        var currentStep: Int = 0,
        var initialData: MutableMap<Any?, Any?> = mutableMapOf(),
        var pendingStepData: MutableMap<Int, Map<Any?, Any?>> = mutableMapOf()
    )

    override fun init(flow: String?) {
        Log.d(TAG, "init($flow)")
        initWithRequest(flow, null)
    }

    @SuppressLint("SetWorldReadable")
    override fun initWithRequest(flow: String?, request: DroidGuardResultsRequest?): DroidGuardInitReply {
        Log.d(TAG, "initWithRequest($flow, $request)")
        this.flow = flow
        this.request = request
        var handleProxy: HandleProxy? = null
        try {
            if (!LOW_LATENCY_ENABLED || flow in NOT_LOW_LATENCY_FLOWS) {
                handleProxy = null
            } else {
                try {
                    handleProxy = factory.createLowLatencyHandle(flow, callback, request)
                    Log.d(TAG, "Using low-latency handle")
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    handleProxy = null
                }
            }
            if (handleProxy == null) {
                handleProxy = factory.createHandle(packageName, flow, callback, request)
            }
            if (handleProxy.init()) {
                this.handleProxy = handleProxy
            } else {
                throw Exception("init failed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during handle init", e)
            this.handleInitError = e
        }
        this.condition.open()
        if (handleInitError == null) {
            try {
                val `object` = handleProxy!!.handle.javaClass.getDeclaredMethod("rb").invoke(handleProxy.handle) as? Parcelable?
                if (`object` != null) {
                    val vmKey = handleProxy.vmKey
                    val theApk = factory.getTheApkFile(vmKey)
                    try {
                        theApk.setReadable(true, false)
                        return DroidGuardInitReply(ParcelFileDescriptor.open(theApk, ParcelFileDescriptor.MODE_READ_ONLY), `object`)
                    } catch (e: FileNotFoundException) {
                        throw Exception("Files for VM $vmKey not found on disk")
                    }
                }
            } catch (e: Exception) {
                this.handleProxy = null
                handleInitError = e
            }
        }
        return DroidGuardInitReply(null, null)
    }

    override fun snapshot(map: MutableMap<Any?, Any?>): ByteArray {
        Log.d(TAG, "snapshot($map)")
        return snapshotWithFlow(map, flow)
    }

    private fun snapshotWithFlow(map: MutableMap<Any?, Any?>, flow: String?): ByteArray {
        condition.block()
        handleInitError?.let { return FallbackCreator.create(flow, context, map, it) }
        val handleProxy = this.handleProxy ?: return FallbackCreator.create(flow, context, map, IllegalStateException())
        return try {
            handleProxy.handle::class.java.getDeclaredMethod("ss", Map::class.java).invoke(handleProxy.handle, map) as ByteArray
        } catch (e: Exception) {
            try {
                throw BytesException(handleProxy.extra, e)
            } catch (e2: Exception) {
                FallbackCreator.create(flow, context, map, e2)
            }
        }
    }

    override fun begin(flow: String?, request: DroidGuardResultsRequest?, initialData: Map<Any?, Any?>?): Long {
        Log.d(TAG, "begin($flow, $request, $initialData)")
        condition.block()
        if (handleProxy == null) return -1

        val sessionId = sessionIdSequence.incrementAndGet()
        val normalizedRequest = request?.copy() ?: this.request?.copy() ?: DroidGuardResultsRequest()
        val resolvedFlow = flow ?: this.flow
        val session = MultiStepSession(
            flow = resolvedFlow,
            request = normalizedRequest,
            initialData = initialData?.toMutableMap() ?: mutableMapOf()
        )
        sessions[sessionId] = session
        normalizedRequest.setSessionId(sessionId)
        normalizedRequest.setMultiStep(true)
        normalizedRequest.setStepNumber(0)
        return sessionId
    }

    override fun nextStep(sessionId: Long, stepData: Map<Any?, Any?>?): DroidGuardInitReply {
        Log.d(TAG, "nextStep($sessionId, $stepData)")
        condition.block()
        val session = sessions[sessionId] ?: return DroidGuardInitReply(null, null)
        session.currentStep++
        session.pendingStepData[session.currentStep] = stepData.orEmpty()
        return DroidGuardInitReply(null, null)
    }

    override fun snapshotWithSession(sessionId: Long, map: MutableMap<Any?, Any?>): ByteArray {
        Log.d(TAG, "snapshotWithSession($sessionId, $map)")
        condition.block()
        val session = sessions.remove(sessionId) ?: return byteArrayOf()
        val request = session.request
            ?: this.request
            ?: return byteArrayOf()

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

        return snapshotWithFlow(combinedMap, session.flow)
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

    override fun close() {
        Log.d(TAG, "close()")
        condition.block()
        try {
            handleProxy?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during handle close", e)
        }
        sessions.clear()
        request = null
        flow = null
        handleProxy = null
        handleInitError = null
    }

    companion object {
        private const val TAG = "GmsGuardHandleImpl"
        private val LOW_LATENCY_ENABLED = false
        private val NOT_LOW_LATENCY_FLOWS = setOf("ad_attest", "attest", "checkin", "federatedMachineLearningReduced", "msa-f", "ad-event-attest-token")
    }
}
