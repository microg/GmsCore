/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.asterism

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

private const val TAG = "AsterismService"
private const val DESCRIPTOR = "com.google.android.gms.asterism.internal.IAsterismApiService"
private const val CB_DESCRIPTOR = "com.google.android.gms.asterism.internal.IAsterismCallbacks"

class AsterismService : BaseService(TAG, GmsService.ASTERISM) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "handleServiceRequest from $packageName")
        callback.onPostInitComplete(ConnectionResult.SUCCESS, AsterismBinder(packageName), null)
    }
}

class AsterismBinder(private val packageName: String?) : Binder() {
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
                1 -> { // getAsterismConsent(cb, request)
                    val cb = data.readStrongBinder()
                    deliverSuccessConsent(cb, 1)
                    reply?.writeNoException()
                    true
                }
                2 -> { // setAsterismConsent(cb, request)
                    val cb = data.readStrongBinder()
                    deliverSuccessConsent(cb, 2)
                    reply?.writeNoException()
                    true
                }
                3 -> { // getIsPnvrConstellationDevice(cb)
                    val cb = data.readStrongBinder()
                    deliverPnvrDevice(cb)
                    reply?.writeNoException()
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling transaction $code", e)
            false
        }
    }

    private fun deliverSuccessConsent(cb: IBinder?, txCode: Int) {
        if (cb == null) return
        val parcel = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            parcel.writeInterfaceToken(CB_DESCRIPTOR)
            Status.SUCCESS.writeToParcel(parcel, 0)
            cb.transact(txCode, parcel, reply, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering consent response", e)
        } finally {
            parcel.recycle()
            reply.recycle()
        }
    }

    private fun deliverPnvrDevice(cb: IBinder?) {
        if (cb == null) return
        val parcel = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            parcel.writeInterfaceToken(CB_DESCRIPTOR)
            Status.SUCCESS.writeToParcel(parcel, 0)
            parcel.writeInt(1) // isPnvrConstellationDevice = true
            cb.transact(1, parcel, reply, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed delivering pnvr device response", e)
        } finally {
            parcel.recycle()
            reply.recycle()
        }
    }
}

