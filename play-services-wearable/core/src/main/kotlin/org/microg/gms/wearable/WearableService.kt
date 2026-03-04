package org.microg.gms.wearable

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "WearableService"

class WearableService : BaseService(TAG, GmsService.WEARABLE) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        Log.w(TAG, "WearableService not supported yet")
        // Return an error status and no binder
        callback.onPostInitComplete(CommonStatusCodes.INTERNAL_ERROR, null, Bundle())
    }
}