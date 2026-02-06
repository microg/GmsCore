/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.os.Build
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.appcert.AppCertManager
import org.microg.gms.common.GmsService

private const val TAG = "ConstellationService"

/**
 * Constellation service endpoint for phone number verification.
 * Used by Google Messages for RCS provisioning via TS43 carrier-based verification.
 */
class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {

    private val appCert by lazy { AppCertManager(this) }

    private val api by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ConstellationApiServiceImpl(this, appCert::getSpatulaHeader)
        } else {
            // Pre-Android 15: use the same implementation but some telephony APIs
            // will use reflection or fallback values
            Log.w(TAG, "Running on API ${Build.VERSION.SDK_INT}, some telephony features may be limited")
            ConstellationApiServiceImpl(this, appCert::getSpatulaHeader)
        }
    }

    @Throws(RemoteException::class)
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        val conn = ConnectionInfo().apply {
            features = arrayOf(
                Feature("verify_phone_number", 2),
                Feature("get_iid_token", 1),
                Feature("ts43", 1),
            )
        }
        callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.SUCCESS, api.asBinder(), conn)
    }
}
