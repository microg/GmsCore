package org.microg.gms.wearable

import android.os.IBinder
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

/**
 * Stub Wearable GMS service for Wear OS pairing support.
 */
class WearableService : BaseService("WearableService", GmsService.WEARABLE) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        // Return success even though wearable features are not implemented
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, null as IBinder?, null)
    }
}