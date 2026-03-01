/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale

private const val RCS_TAG = "RcsApiService"
private const val CARRIER_AUTH_TAG = "CarrierAuthService"
private const val TRACE_CAPACITY = 64
private const val DEFAULT_RCS_DESCRIPTOR = "com.google.android.gms.rcs.IRcsService"
private const val DEFAULT_CARRIER_DESCRIPTOR = "com.google.android.gms.carrierauth.internal.ICarrierAuthService"
private const val BLOCKER_THRESHOLD = 4

private data class BinderTrace(
    val traceId: Long,
    val service: String,
    val caller: String,
    val callerUid: Int,
    val callerPid: Int,
    val code: Int,
    val flags: Int,
    val dataSize: Int,
    val token: String?,
    val detail: String,
    val handled: Boolean,
    val elapsedRealtimeMs: Long
)

private object BinderTraceStore {
    private val traces = ArrayDeque<BinderTrace>()
    private var nextTraceId = 1L

    @Synchronized
    fun add(trace: BinderTrace): Long {
        val traceId = nextTraceId++
        val materialized = trace.copy(traceId = traceId)
        while (traces.size >= TRACE_CAPACITY) traces.removeFirst()
        traces.addLast(materialized)
        return traceId
    }

    @Synchronized
    fun dump(tag: String) {
        Log.d(tag, "trace_summary count=${traces.size}")
        traces.forEach {
            Log.d(
                tag,
                "trace id=${it.traceId} service=${it.service} caller=${it.caller} uid=${it.callerUid} pid=${it.callerPid} code=${it.code} flags=${it.flags} size=${it.dataSize} token=${it.token} detail=${it.detail} handled=${it.handled} t=${it.elapsedRealtimeMs}"
            )
        }
    }
}

private data class BlockerKey(
    val service: String,
    val token: String,
    val code: Int,
    val detail: String,
    val caller: String
)

private data class BlockerStats(
    val firstTraceId: Long,
    var lastTraceId: Long,
    var count: Int
)

private object BlockerDetector {
    private val counters = LinkedHashMap<BlockerKey, BlockerStats>()

    @Synchronized
    fun observe(trace: BinderTrace): String? {
        if (trace.handled) return null
        if (trace.detail == "passthrough") return null
        val token = trace.token ?: "<null>"
        val key = BlockerKey(
            service = trace.service,
            token = token,
            code = trace.code,
            detail = trace.detail,
            caller = trace.caller
        )
        val stats = counters[key]
        val nextCount = if (stats == null) {
            counters[key] = BlockerStats(
                firstTraceId = trace.traceId,
                lastTraceId = trace.traceId,
                count = 1
            )
            1
        } else {
            stats.lastTraceId = trace.traceId
            stats.count += 1
            stats.count
        }
        if (nextCount == BLOCKER_THRESHOLD || nextCount % 10 == 0) {
            return "blocker_candidate service=${key.service} caller=${key.caller} token=${key.token} code=${key.code} detail=${key.detail} repeated=$nextCount"
        }
        return null
    }

    @Synchronized
    fun dump(tag: String) {
        if (counters.isEmpty()) {
            Log.d(tag, "blocker_summary count=0")
            return
        }
        val ranked = counters.entries
            .sortedWith(
                compareByDescending<Map.Entry<BlockerKey, BlockerStats>> { it.value.count }
                    .thenBy { it.value.firstTraceId }
            )
        Log.d(tag, "blocker_summary count=${ranked.size}")
        ranked.take(10).forEachIndexed { index, entry ->
            val k = entry.key
            val v = entry.value
            Log.d(
                tag,
                "blocker_summary rank=${index + 1} repeated=${v.count} first_trace=${v.firstTraceId} last_trace=${v.lastTraceId} service=${k.service} caller=${k.caller} token=${k.token} code=${k.code} detail=${k.detail}"
            )
        }
    }
}

class RcsService : BaseService(RCS_TAG, GmsService.RCS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(
            ConnectionResult.SUCCESS,
            DynamicBinderAdapter(applicationContext, "rcs", packageName, DEFAULT_RCS_DESCRIPTOR),
            null
        )
    }
}

class CarrierAuthService : BaseService(CARRIER_AUTH_TAG, GmsService.CARRIER_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(
            ConnectionResult.SUCCESS,
            DynamicBinderAdapter(applicationContext, "carrier_auth", packageName, DEFAULT_CARRIER_DESCRIPTOR),
            null
        )
    }
}

private class DynamicBinderAdapter(
    private val context: Context,
    private val serviceName: String,
    private val callingPackage: String,
    private val defaultDescriptor: String
) : Binder() {
    private val iface = object : IInterface {
        override fun asBinder(): IBinder = this@DynamicBinderAdapter
    }
    init {
        attachInterface(iface, defaultDescriptor)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(defaultDescriptor)
            return true
        }
        if (code == DUMP_TRANSACTION) {
            val tag = if (serviceName == "rcs") RCS_TAG else CARRIER_AUTH_TAG
            BinderTraceStore.dump(tag)
            BlockerDetector.dump(tag)
            reply?.writeNoException()
            return true
        }

        val token = readInterfaceToken(data)
        val decision = routeTransaction(code, token)
        applyDecisionReply(decision, reply, flags)
        val trace = BinderTrace(
            traceId = 0L,
            service = serviceName,
            caller = callingPackage,
            callerUid = getCallingUid(),
            callerPid = getCallingPid(),
            code = code,
            flags = flags,
            dataSize = data.dataSize(),
            token = token,
            detail = decision.detail,
            handled = decision.handled,
            elapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
        val traceId = BinderTraceStore.add(trace)
        if (decision.detail != "passthrough") {
            Log.i(
                if (serviceName == "rcs") RCS_TAG else CARRIER_AUTH_TAG,
                "trace_decision id=$traceId detail=${decision.detail} handled=${decision.handled} token=$token code=$code"
            )
        }
        val blockerHint = BlockerDetector.observe(trace.copy(traceId = traceId))
        if (blockerHint != null) {
            Log.w(
                if (serviceName == "rcs") RCS_TAG else CARRIER_AUTH_TAG,
                blockerHint
            )
        }
        return decision.handled
    }

    private fun routeTransaction(code: Int, token: String?): ContractDecision {
        return RcsContractPolicy.decide(
            ContractRow(
                token = token,
                code = code,
                callingPackage = callingPackage
            ),
            RcsPolicyConfigStore.current(context)
        )
    }

    private fun applyDecisionReply(decision: ContractDecision, reply: Parcel?, flags: Int) {
        when (decision.mode) {
            ContractDecisionMode.COMPLETE_CONFIG_UNAVAILABLE -> {
                RcsReplyCodec.writeConfigUnavailable(reply, flags)
            }
            ContractDecisionMode.COMPLETE_GENERIC_UNAVAILABLE -> {
                RcsReplyCodec.writeGenericUnavailable(reply, flags)
            }
            else -> Unit
        }
    }

    private fun readInterfaceToken(parcel: Parcel): String? {
        val position = parcel.dataPosition()
        return try {
            parcel.setDataPosition(0)
            val raw = parcel.readString()
            if (looksLikeInterfaceToken(raw)) return raw
            parcel.setDataPosition(0)
            parcel.readInt() // strict mode header or parcel preamble
            val shifted = parcel.readString()
            if (looksLikeInterfaceToken(shifted)) shifted else null
        } catch (_: Throwable) {
            null
        } finally {
            parcel.setDataPosition(position)
        }
    }

    private fun looksLikeInterfaceToken(candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        val normalized = candidate.lowercase(Locale.US)
        return normalized.startsWith("com.google.android") &&
            (normalized.contains(".rcs.") || normalized.contains(".carrierauth.") || normalized.contains("provisioning"))
    }
}
