/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "ConstellationService"
private const val DESCRIPTOR = "com.google.android.gms.constellation.internal.IConstellationApiService"
private const val CB_DESCRIPTOR = "com.google.android.gms.constellation.internal.IConstellationCallbacks"

class ConstellationService : BaseService(TAG, GmsService.CONSTELLATION) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, ConstellationBinder(packageName), null)
    }
}

class ConstellationBinder(private val packageName: String?) : Binder() {
    init {
        attachInterface(null, DESCRIPTOR)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == INTERFACE_TRANSACTION) {
            reply?.writeString(DESCRIPTOR)
            return true
        }
        data.enforceInterface(DESCRIPTOR)
        return try {
            when (code) {
                1, 2, 3 -> { // verifyPhoneNumber variants
                    val cb = data.readStrongBinder()
                    deliverCallbackStatus(cb, 1)
                    reply?.writeNoException()
                    true
                }
                4 -> { // getIidToken
                    val cb = data.readStrongBinder()
                    deliverCallbackStatus(cb, 2)
                    reply?.writeNoException()
                    true
                }
                5 -> { // getPnvCapabilities
                    val cb = data.readStrongBinder()
                    deliverCallbackStatus(cb, 3)
                    reply?.writeNoException()
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling Constellation transaction $code", e)
            false
        }
    }

    private fun deliverCallbackStatus(cb: IBinder?, txCode: Int) {
        if (cb == null) return
        val parcel = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            parcel.writeInterfaceToken(CB_DESCRIPTOR)
            Status.SUCCESS.writeToParcel(parcel, 0)
            cb.transact(txCode, parcel, reply, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering Constellation response", e)
        } finally {
            parcel.recycle()
            reply.recycle()
        }
    }
}

