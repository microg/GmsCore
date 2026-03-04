package org.microg.gms.wearable

import android.os.Parcel
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.wearable.internal.IWearableService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "WearableService"

/**
 * Stub WearableService to register Wear OS connectivity API.
 * Minimal implementation returning an empty IWearableService binder.
 */
class WearableService : BaseService(TAG, GmsService.WEARABLE) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        // Return our stub binder for wearable service
        val binder = WearableServiceImpl().asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class WearableServiceImpl : IWearableService.Stub() {
    // Override transact to catch warnings, other methods default to no-op or throw as per stub
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}