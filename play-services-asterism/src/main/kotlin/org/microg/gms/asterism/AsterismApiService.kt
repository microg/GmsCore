package org.microg.gms.asterism

import android.content.Context
import android.util.Log
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "AsterismApiService"

class AsterismApiService : BaseService(TAG, GmsService.ASTERISM) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun handleServiceRequest(
        callback: IGmsCallbacks?,
        request: GetServiceRequest?,
        service: GmsService?
    ) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        if (!PackageUtils.isGooglePackage(this, packageName)) {
            throw SecurityException("$packageName is not a Google package")
        }
        callback!!.onPostInitCompleteWithConnectionInfo(
            0,
            AsterismApiServiceImpl(this, serviceScope).asBinder(),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("asterism_consent", 3),
                    Feature("one_time_verification", 1),
                    Feature("carrier_auth", 1),
                    Feature("verify_phone_number", 2),
                    Feature("get_iid_token", 1),
                    Feature("get_pnv_capabilities", 1),
                    Feature("ts43", 1),
                    Feature("verify_phone_number_local_read", 1)
                )
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel("AsterismApiService destroyed")
    }
}

class AsterismApiServiceImpl(
    private val context: Context,
    private val serviceScope: CoroutineScope
) : IAsterismApiService.Stub() {
    override fun getAsterismConsent(
        cb: IAsterismCallbacks?,
        request: GetAsterismConsentRequest?,
    ) {
        Log.i(TAG, "getAsterismConsent(): $request")
        if (cb == null || request == null) return
        serviceScope.launch { handleGetAsterismConsent(context, cb, request) }
    }

    override fun setAsterismConsent(cb: IAsterismCallbacks?, request: SetAsterismConsentRequest?) {
        Log.i(TAG, "setAsterismConsent(): $request")
        if (cb == null || request == null) return
        serviceScope.launch { handleSetAsterismConsent(context, cb, request) }
    }

    override fun getIsPnvrConstellationDevice(cb: IAsterismCallbacks?) {
        Log.i(TAG, "getIsPnvrConstellationDevice()")
        if (cb == null) return
        serviceScope.launch { handleGetIsPnvrConstellationDevice(context, cb) }
    }
}
