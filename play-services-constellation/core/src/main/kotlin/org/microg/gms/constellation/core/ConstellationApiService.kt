package org.microg.gms.constellation.core

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "C11NApiService"

class ConstellationApiService : BaseService(TAG, GmsService.CONSTELLATION) {
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
            ConstellationApiServiceImpl(this, packageName, serviceScope).asBinder(),
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
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel("ConstellationApiService destroyed")
    }
}

class ConstellationApiServiceImpl(
    private val context: Context,
    private val packageName: String?,
    private val serviceScope: CoroutineScope
) : IConstellationApiService.Stub() {
    override fun verifyPhoneNumberV1(
        cb: IConstellationCallbacks?,
        bundle: Bundle?,
        apiMetadata: ApiMetadata?
    ) {
        Log.i(
            TAG,
            "verifyPhoneNumberV1(): mode=${bundle?.getInt("verification_mode")}, policy=${
                bundle?.getString("policy_id")
            }"
        )
        if (cb == null || bundle == null) return
        serviceScope.launch { handleVerifyPhoneNumberV1(context, cb, bundle, packageName) }
    }

    override fun verifyPhoneNumberSingleUse(
        cb: IConstellationCallbacks?,
        bundle: Bundle?,
        apiMetadata: ApiMetadata?
    ) {
        Log.i(TAG, "verifyPhoneNumberSingleUse()")
        if (cb == null || bundle == null) return
        serviceScope.launch { handleVerifyPhoneNumberSingleUse(context, cb, bundle, packageName) }
    }

    override fun verifyPhoneNumber(
        cb: IConstellationCallbacks?,
        request: VerifyPhoneNumberRequest?,
        apiMetadata: ApiMetadata?
    ) {
        Log.i(
            TAG,
            "verifyPhoneNumber(): apiVersion=${request?.apiVersion}, policy=${request?.policyId}"
        )
        if (cb == null || request == null) return
        serviceScope.launch { handleVerifyPhoneNumberRequest(context, cb, request, packageName) }
    }

    override fun getIidToken(
        cb: IConstellationCallbacks?,
        request: GetIidTokenRequest?,
        apiMetadata: ApiMetadata?,
    ) {
        Log.i(TAG, "getIidToken(): $request")
        if (cb == null || request == null) return
        serviceScope.launch { handleGetIidToken(context, cb, request) }
    }

    override fun getPnvCapabilities(
        cb: IConstellationCallbacks?,
        request: GetPnvCapabilitiesRequest?,
        apiMetadata: ApiMetadata?,
    ) {
        Log.i(TAG, "getPnvCapabilities(): $request")
        if (cb == null || request == null) return
        serviceScope.launch { handleGetPnvCapabilities(context, cb, request) }
    }
}
