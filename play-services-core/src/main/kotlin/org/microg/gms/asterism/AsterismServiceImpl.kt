/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

import android.content.Context
import android.util.Log
import com.google.android.gms.asterism.*
import com.google.android.gms.asterism.internal.IAsterismApiService
import com.google.android.gms.asterism.internal.IAsterismCallbacks
import com.google.android.gms.common.api.Status

private const val TAG = "GmsAsterism"

class AsterismServiceImpl : IAsterismApiService.Stub() {

    override fun getAsterismConsent(callbacks: IAsterismCallbacks?, request: GetAsterismConsentRequest?) {
        Log.d(TAG, "getAsterismConsent($request)")
        val response = GetAsterismConsentResponse()
        response.consented = true
        response.consentToken = "microg-consent-token"
        callbacks?.onGetAsterismConsent(Status.SUCCESS, response)
    }

    override fun setAsterismConsent(callbacks: IAsterismCallbacks?, request: SetAsterismConsentRequest?) {
        Log.d(TAG, "setAsterismConsent(consented=${request?.consented})")
        val response = SetAsterismConsentResponse()
        response.success = true
        callbacks?.onSetAsterismConsent(Status.SUCCESS, response)
    }
}
