package com.google.android.gms.potokens.internal

import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

const val TAG = "PoTokensApi"

class PoTokensApiService : BaseService(TAG, GmsService.POTOKENS) {
    @Throws(RemoteException::class)
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = arrayOf(Feature("PO_TOKENS", 1))
        Log.d(TAG,"PoTokensApiService handleServiceRequest")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS, PoTokensApiServiceImpl(
                applicationContext, request.packageName
            ), connectionInfo
        )
    }
}
