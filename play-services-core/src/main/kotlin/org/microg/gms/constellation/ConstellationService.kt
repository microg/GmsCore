/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.constellation.GetIidTokenRequest
import com.google.android.gms.constellation.GetIidTokenResponse
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse
import com.google.android.gms.constellation.VerifyPhoneNumberRequest
import com.google.android.gms.constellation.VerifyPhoneNumberResponse
import com.google.android.gms.constellation.PhoneNumberInfo
import com.google.android.gms.constellation.internal.IConstellationApiService
import com.google.android.gms.constellation.internal.IConstellationCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "ConstellationSvc"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {

    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        Log.d(TAG, "handleServiceRequest for package: ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            ConstellationServiceImpl(),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("constellation_phone_number_verification", 1),
                    Feature("constellation_iid_token", 1),
                    Feature("constellation_pnv_capabilities", 1)
                )
            }
        )
    }
}

class ConstellationServiceImpl : IConstellationApiService.Stub() {

    override fun verifyPhoneNumberV1(
        callbacks: IConstellationCallbacks?,
        bundle: Bundle?,
        metadata: com.google.android.gms.common.api.ApiMetadata?
    ) {
        Log.d(TAG, "verifyPhoneNumberV1 called")
        try {
            callbacks?.onPhoneNumberVerificationsCompleted(
                Status(CommonStatusCodes.SUCCESS),
                VerifyPhoneNumberResponse(),
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyPhoneNumberV1", e)
        }
    }

    override fun verifyPhoneNumberSingleUse(
        callbacks: IConstellationCallbacks?,
        bundle: Bundle?,
        metadata: com.google.android.gms.common.api.ApiMetadata?
    ) {
        Log.d(TAG, "verifyPhoneNumberSingleUse called")
        try {
            callbacks?.onPhoneNumberVerificationsCompleted(
                Status(CommonStatusCodes.SUCCESS),
                VerifyPhoneNumberResponse(),
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyPhoneNumberSingleUse", e)
        }
    }

    override fun verifyPhoneNumber(
        callbacks: IConstellationCallbacks?,
        request: VerifyPhoneNumberRequest?,
        metadata: com.google.android.gms.common.api.ApiMetadata?
    ) {
        Log.d(TAG, "verifyPhoneNumber called for subscription: ${request?.subscriptionId}")
        try {
            callbacks?.onPhoneNumberVerificationsCompleted(
                Status(CommonStatusCodes.SUCCESS),
                VerifyPhoneNumberResponse(),
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyPhoneNumber", e)
        }
    }

    override fun getIidToken(
        callbacks: IConstellationCallbacks?,
        request: GetIidTokenRequest?,
        metadata: com.google.android.gms.common.api.ApiMetadata?
    ) {
        Log.d(TAG, "getIidToken called for subscription: ${request?.subscriptionId}")
        try {
            callbacks?.onIidTokenGenerated(
                Status(CommonStatusCodes.SUCCESS),
                GetIidTokenResponse(),
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getIidToken", e)
        }
    }

    override fun getPnvCapabilities(
        callbacks: IConstellationCallbacks?,
        request: GetPnvCapabilitiesRequest?,
        metadata: com.google.android.gms.common.api.ApiMetadata?
    ) {
        Log.d(TAG, "getPnvCapabilities called")
        try {
            callbacks?.onGetPnvCapabilitiesCompleted(
                Status(CommonStatusCodes.SUCCESS),
                GetPnvCapabilitiesResponse(),
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getPnvCapabilities", e)
        }
    }
}
