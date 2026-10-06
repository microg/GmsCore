/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.rcs.internal.IRcsService
import com.google.android.gms.rcs.internal.IRcsServiceCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GmsRcsService"

/**
 * RCS service stub for microG.
 *
 * Google Messages binds com.google.android.gms.rcs.START during its RCS
 * provisioning flow and expects an IRcsService binder back. Previously
 * this was handled by DummyService (which returns API_DISABLED), causing
 * the "Setting up…" spinner to hang indefinitely.
 *
 * This stub returns SUCCESS for all capability / configuration queries,
 * which allows the provisioning flow in Google Messages to proceed and
 * contact the Jibe (Google RCS) back-end directly via HTTPS. The actual
 * phone-number verification is done by Google Messages + Jibe using SMS
 * OTP or SIM-based auth (ICC); GMS only needs to hold RECEIVE_SMS /
 * READ_SMS / SEND_SMS to receive the verification OTP.
 *
 * For carriers that require ICC (SIM card) authentication (notably
 * T-Mobile and AT&T), this stub does not yet implement that path — a
 * future enhancement can bridge TelephonyManager.getIccAuthentication()
 * here once the exact protobuf schema is confirmed.
 */
class RcsService : BaseService(TAG, GmsService.RCS) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        Log.d(TAG, "handleServiceRequest: package=${request.packageName}")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, RcsServiceImpl().asBinder(), null)
    }
}

class RcsServiceImpl : IRcsService.Stub() {

    override fun getCapabilities(callbacks: IRcsServiceCallbacks?, packageName: String?) {
        Log.d(TAG, "getCapabilities(packageName=$packageName)")
        try {
            callbacks?.onStatus(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "getCapabilities: failed to deliver callback", e)
        }
    }

    override fun getConfiguration(callbacks: IRcsServiceCallbacks?, packageName: String?) {
        Log.d(TAG, "getConfiguration(packageName=$packageName)")
        try {
            // Return an empty configuration payload; Google Messages will fall back to
            // server-fetched config via its own network calls to Jibe.
            callbacks?.onResult(Status.SUCCESS, ByteArray(0))
        } catch (e: Exception) {
            Log.w(TAG, "getConfiguration: failed to deliver callback", e)
        }
    }

    override fun sendRequest(callbacks: IRcsServiceCallbacks?, packageName: String?, request: ByteArray?) {
        Log.d(TAG, "sendRequest(packageName=$packageName, requestSize=${request?.size ?: 0})")
        try {
            callbacks?.onResult(Status.SUCCESS, ByteArray(0))
        } catch (e: Exception) {
            Log.w(TAG, "sendRequest: failed to deliver callback", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
