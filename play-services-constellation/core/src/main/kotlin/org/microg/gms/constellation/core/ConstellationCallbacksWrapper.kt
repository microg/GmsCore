package org.microg.gms.constellation.core

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.Status
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.internal.IConstellationCallbacks

class ConstellationCallbacksWrapper(
    private val cb: IConstellationCallbacks
) : IConstellationCallbacks {

    override fun onPhoneNumberVerified(
        status: Status?,
        phoneNumbers: List<PhoneNumberInfo?>?,
        apiMetadata: ApiMetadata?
    ) = runRemote("onPhoneNumberVerified") {
        cb.onPhoneNumberVerified(status, phoneNumbers, apiMetadata)
    }

    override fun onPhoneNumberVerificationsCompleted(
        status: Status?,
        response: VerifyPhoneNumberResponse?,
        apiMetadata: ApiMetadata?
    ) = runRemote("onPhoneNumberVerificationsCompleted") {
        cb.onPhoneNumberVerificationsCompleted(status, response, apiMetadata)
    }

    override fun onIidTokenGenerated(
        status: Status?,
        response: GetIidTokenResponse?,
        apiMetadata: ApiMetadata?
    ) = runRemote("onIidTokenGenerated") {
        cb.onIidTokenGenerated(status, response, apiMetadata)
    }

    override fun onGetPnvCapabilitiesCompleted(
        status: Status?,
        response: GetPnvCapabilitiesResponse?,
        apiMetadata: ApiMetadata?
    ) = runRemote("onGetPnvCapabilitiesCompleted") {
        cb.onGetPnvCapabilitiesCompleted(status, response, apiMetadata)
    }

    override fun asBinder(): IBinder = cb.asBinder()

    private inline fun runRemote(methodName: String, block: () -> Unit) {
        try {
            block()
        } catch (e: RemoteException) {
            Log.w(TAG, "RemoteException in $methodName", e)
        }
    }

    companion object {
        private const val TAG = "ConstellationCallbacks"
    }
}