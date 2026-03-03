package org.microg.gms.wearable

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.wearable.internal.IWearableService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "WearableService"

class WearableService : BaseService(TAG, GmsService.WEARABLE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "WearableService: handleServiceRequest")
        val binder = WearableServiceImpl().asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class WearableServiceImpl : IWearableService.Stub() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) {
            Log.w(TAG, "Stub method called on code: $code")
            false
        }
}