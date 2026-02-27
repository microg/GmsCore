/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

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

private const val RCS_TAG = "RcsApiService"
private const val CARRIER_AUTH_TAG = "CarrierAuthService"
private const val TRACE_CAPACITY = 64

private data class BinderTrace(
    val service: String,
    val caller: String,
    val code: Int,
    val flags: Int,
    val token: String?,
    val elapsedRealtimeMs: Long
)

private object BinderTraceStore {
    private val traces = ArrayDeque<BinderTrace>()

    @Synchronized
    fun add(trace: BinderTrace) {
        while (traces.size >= TRACE_CAPACITY) traces.removeFirst()
        traces.addLast(trace)
    }

    @Synchronized
    fun dump(tag: String) {
        traces.forEach {
            Log.d(
                tag,
                "trace service=${it.service} caller=${it.caller} code=${it.code} flags=${it.flags} token=${it.token} t=${it.elapsedRealtimeMs}"
            )
        }
    }
}

class RcsService : BaseService(RCS_TAG, GmsService.RCS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, ShimBinder("rcs", packageName, "com.google.android.gms.rcs.IRcsService"), null)
    }
}

class CarrierAuthService : BaseService(CARRIER_AUTH_TAG, GmsService.CARRIER_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitComplete(
            ConnectionResult.SUCCESS,
            ShimBinder("carrier_auth", packageName, "com.google.android.gms.carrierauth.internal.ICarrierAuthService"),
            null
        )
    }
}

private class ShimBinder(
    private val serviceName: String,
    private val callingPackage: String,
    private val descriptor: String
) : Binder() {
    private val iface = object : IInterface {
        override fun asBinder(): IBinder = this@ShimBinder
    }

    init {
        attachInterface(iface, descriptor)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(descriptor)
            return true
        }
        if (code == DUMP_TRANSACTION) {
            BinderTraceStore.dump(if (serviceName == "rcs") RCS_TAG else CARRIER_AUTH_TAG)
            reply?.writeNoException()
            return true
        }
        BinderTraceStore.add(
            BinderTrace(
                service = serviceName,
                caller = callingPackage,
                code = code,
                flags = flags,
                token = readInterfaceToken(data),
                elapsedRealtimeMs = SystemClock.elapsedRealtime()
            )
        )
        if (flags and FLAG_ONEWAY == 0) {
            reply?.writeNoException()
            reply?.writeInt(0)
        }
        return true
    }

    private fun readInterfaceToken(parcel: Parcel): String? {
        val position = parcel.dataPosition()
        return runCatching { parcel.readInterfaceToken() }
            .getOrNull()
            .also { parcel.setDataPosition(position) }
    }
}
