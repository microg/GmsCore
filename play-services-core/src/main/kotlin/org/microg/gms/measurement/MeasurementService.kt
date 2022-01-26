/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.measurement

import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.measurement.internal.*
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "MeasurementService"

class MeasurementService : BaseService(TAG, GmsService.MEASUREMENT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, MeasurementServiceImpl(), Bundle())
    }
}

class MeasurementServiceImpl : IMeasurementService.Stub() {
    override fun f1(event: EventParcel, app: AppMetadata) {
        Log.d(TAG, "f1($event) for $app")
    }

    override fun f2(attribute: UserAttributeParcel?, app: AppMetadata) {
        Log.d(TAG, "f2($attribute) for $app")
    }

    override fun f4(app: AppMetadata) {
        Log.d(TAG, "f4() for $app")
    }

    override fun f10(p0: Long, p1: String?, p2: String?, p3: String?) {
        Log.d(TAG, "f10($p0, $p1, $p2, $p3)")
    }

    override fun f11(app: AppMetadata): String? {
        Log.d(TAG, "f11() for $app")
        return null
    }

    override fun f12(property: ConditionalUserPropertyParcel, app: AppMetadata) {
        Log.d(TAG, "f12($property) for $app")
    }

    override fun setDefaultEventParameters(params: Bundle, app: AppMetadata) {
        Log.d(TAG, "setDefaultEventParameters($params) for $app")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }
}
