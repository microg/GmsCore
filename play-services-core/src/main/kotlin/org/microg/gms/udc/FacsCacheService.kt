/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.udc

import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.facs.cache.FacsCacheCallOptions
import com.google.android.gms.facs.cache.internal.IFacsCacheCallbacks
import com.google.android.gms.facs.cache.internal.IFacsCacheService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "GmsFacsCache"

class FacsCacheService : BaseService(TAG, GmsService.FACS_CACHE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        callback.onPostInitComplete(0, FacsCacheServiceImpl().asBinder(), null)
    }
}

class FacsCacheServiceImpl : IFacsCacheService.Stub() {
    override fun forceSettingsCacheRefresh(callbacks: IFacsCacheCallbacks, options: FacsCacheCallOptions) {
        Log.d(TAG, "forceSettingsCacheRefresh")
        callbacks.onForceSettingsCacheRefreshResult(Status.CANCELED, null)
    }

    override fun updateActivityControlsSettings(callbacks: IFacsCacheCallbacks, bytes: ByteArray, options: FacsCacheCallOptions) {
        Log.d(TAG, "updateActivityControlsSettings")
        callbacks.onUpdateActivityControlsSettingsResult(Status.CANCELED, null)
    }

    override fun getActivityControlsSettings(callbacks: IFacsCacheCallbacks, options: FacsCacheCallOptions) {
        Log.d(TAG, "getActivityControlsSettings")
        callbacks.onGetActivityControlsSettingsResult(Status.CANCELED, null)
    }

    override fun readDeviceLevelSettings(callbacks: IFacsCacheCallbacks) {
        Log.d(TAG, "readDeviceLevelSettings")
        callbacks.onReadDeviceLevelSettingsResult(Status.CANCELED, null)
    }

    override fun writeDeviceLevelSettings(callbacks: IFacsCacheCallbacks, bytes: ByteArray) {
        Log.d(TAG, "writeDeviceLevelSettings")
        callbacks.onWriteDeviceLevelSettingsResult(Status.CANCELED)
    }

}
