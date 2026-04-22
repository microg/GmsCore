/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

import android.util.Log
import com.google.android.gms.asterism.GetAsterismConsentRequest
import com.google.android.gms.asterism.GetAsterismConsentResponse
import com.google.android.gms.asterism.SetAsterismConsentRequest
import com.google.android.gms.asterism.SetAsterismConsentResponse
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "AsterismSvc"

class AsterismService : BaseService(TAG, GmsService.ASTERISM) {

    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        Log.d(TAG, "handleServiceRequest for package: ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            AsterismServiceImpl(),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("asterism_consent", 1),
                    Feature("asterism_pnvr_constellation", 1)
                )
            }
        )
    }
}

class AsterismServiceImpl : IAsterismApiService.Stub() {

    override fun getAsterismConsent(
        callbacks: IAsterismCallbacks?,
        request: GetAsterismConsentRequest?,
        metadata: ApiMetadata?
    ) {
        Log.d(TAG, "getAsterismConsent called, requestCode: ${request?.requestCode}")
        try {
            val response = GetAsterismConsentResponse().apply {
                requestCode = request?.requestCode ?: 0
                consentState = 1 // CONSENT_GRANTED
                consentVersion = 1
            }
            callbacks?.onConsentFetched(Status(CommonStatusCodes.SUCCESS), response, metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAsterismConsent", e)
        }
    }

    override fun setAsterismConsent(
        callbacks: IAsterismCallbacks?,
        request: SetAsterismConsentRequest?,
        metadata: ApiMetadata?
    ) {
        Log.d(TAG, "setAsterismConsent called, requestCode: ${request?.requestCode}")
        try {
            val response = SetAsterismConsentResponse().apply {
                requestCode = request?.requestCode ?: 0
            }
            callbacks?.onConsentRegistered(Status(CommonStatusCodes.SUCCESS), response, metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error in setAsterismConsent", e)
        }
    }

    override fun getIsPnvrConstellationDevice(
        callbacks: IAsterismCallbacks?,
        metadata: ApiMetadata?
    ) {
        Log.d(TAG, "getIsPnvrConstellationDevice called")
        try {
            callbacks?.onIsPnvrConstellationDevice(
                Status(CommonStatusCodes.SUCCESS),
                true,
                metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getIsPnvrConstellationDevice", e)
        }
    }
}
