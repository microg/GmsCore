package org.microg.gms.wearable

import android.os.Binder
import android.os.Bundle
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

class WearableService : BaseService("WearableService", GmsService.WEARABLE) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        // Return an empty Binder stub to satisfy Wear OS binding requests
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, Binder(), Bundle())
    }
}