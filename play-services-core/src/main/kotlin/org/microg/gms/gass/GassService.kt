/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.gass

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.gass.internal.GassRequestParcel
import com.google.android.gms.gass.internal.GassResponseParcel
import com.google.android.gms.gass.internal.IGassService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "GassService"

class GassService : BaseService(TAG, GmsService.GASS) {

    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        callback?.onPostInitComplete(ConnectionResult.SUCCESS, GassServiceImpl().asBinder(), null)
    }

}

class GassServiceImpl : IGassService.Stub() {
    override fun getGassBundle(bundle: Bundle?, code: Int): Bundle? {
        Log.d(TAG, "GassServiceImpl getGassBundle is Called")
        return null
    }

    override fun getGassResponse(gassRequestParcel: GassRequestParcel?): GassResponseParcel? {
        Log.d(TAG, "GassServiceImpl getGassResponse is Called")
        return null
    }

}
