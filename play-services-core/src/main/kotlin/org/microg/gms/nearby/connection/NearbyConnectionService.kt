/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.connection

import android.content.Context
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.nearby.connection.internal.INearbyConnectionCallbacks
import com.google.android.gms.nearby.connection.internal.INearbyConnectionService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NearbyConnectionService"

class NearbyConnectionService : BaseService(TAG, GmsService.NEARBY_CONNECTIONS) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName for service $service")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, NearbyConnectionServiceImpl(this, packageName).asBinder(), null)
    }
}

class NearbyConnectionServiceImpl(
    private val context: Context,
    private val packageName: String?
) : INearbyConnectionService.Stub() {

    private val advertisingCallbacks = ConcurrentHashMap<String, INearbyConnectionCallbacks>()
    private val discoveryCallbacks = ConcurrentHashMap<String, INearbyConnectionCallbacks>()

    override fun startAdvertising(callbacks: INearbyConnectionCallbacks?, name: String?, serviceId: String?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "startAdvertising: name=$name serviceId=$serviceId")
        if (callbacks != null && serviceId != null) {
            advertisingCallbacks[serviceId] = callbacks
        }
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun stopAdvertising(statusCallback: IStatusCallback?) {
        Log.d(TAG, "stopAdvertising")
        advertisingCallbacks.clear()
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun startDiscovery(callbacks: INearbyConnectionCallbacks?, serviceId: String?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "startDiscovery: serviceId=$serviceId")
        if (callbacks != null && serviceId != null) {
            discoveryCallbacks[serviceId] = callbacks
        }
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun stopDiscovery(statusCallback: IStatusCallback?) {
        Log.d(TAG, "stopDiscovery")
        discoveryCallbacks.clear()
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun requestConnection(callbacks: INearbyConnectionCallbacks?, name: String?, endpointId: String?, connectionInfo: ByteArray?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "requestConnection: endpointId=$endpointId name=$name")
        callbacks?.onConnectionInitiated(endpointId ?: "unknown", connectionInfo ?: ByteArray(0))
        callbacks?.onConnectionResult(endpointId ?: "unknown", Status.SUCCESS)
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun acceptConnection(callbacks: INearbyConnectionCallbacks?, endpointId: String?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "acceptConnection: endpointId=$endpointId")
        callbacks?.onConnectionResult(endpointId ?: "unknown", Status.SUCCESS)
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun rejectConnection(callbacks: INearbyConnectionCallbacks?, endpointId: String?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "rejectConnection: endpointId=$endpointId")
        callbacks?.onConnectionResult(endpointId ?: "unknown", Status(CommonStatusCodes.CANCELED, "Rejected"))
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun sendPayload(endpointIds: MutableList<String>?, payloadId: Long, bytes: ByteArray?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "sendPayload: payloadId=$payloadId count=${endpointIds?.size}")
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun disconnectFromEndpoint(endpointId: String?, statusCallback: IStatusCallback?) {
        Log.d(TAG, "disconnectFromEndpoint: endpointId=$endpointId")
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun stopAllEndpoints(statusCallback: IStatusCallback?) {
        Log.d(TAG, "stopAllEndpoints")
        statusCallback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
