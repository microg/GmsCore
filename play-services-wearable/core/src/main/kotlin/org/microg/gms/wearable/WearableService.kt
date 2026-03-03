package org.microg.gms.wearable

import android.os.Binder
import android.os.Bundle
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "WearableService"

/**
 * Minimal stub for wearable API to allow binding without errors.
 */
class WearableService : BaseService(TAG, GmsService.WEARABLE) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        // Return a generic Binder stub; calls will no-op.
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, Binder(), Bundle())
    }
}