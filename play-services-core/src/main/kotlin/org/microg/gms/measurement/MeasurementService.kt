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
    override fun sendEvent(event: EventParcel, app: AppMetadata) {
        Log.d(TAG, "sendEvent($event) for $app")
    }

    override fun sendUserProperty(attribute: UserAttributeParcel?, app: AppMetadata) {
        Log.d(TAG, "sendUserProperty($attribute) for $app")
    }

    override fun sendAppLaunch(app: AppMetadata?) {
        Log.d(TAG, "sendAppLaunch() for $app")
    }

    override fun sendMeasurementEnabled(app: AppMetadata) {
        Log.d(TAG, "sendMeasurementEnabled() for $app")
    }

    override fun getAllUserProperties(app: AppMetadata?, includeInternal: Boolean): List<UserAttributeParcel> {
        Log.d(TAG, "getAllUserProperties($includeInternal) for $app")
        return emptyList()
    }

    override fun sendCurrentScreen(id: Long, name: String?, referrer: String?, packageName: String?) {
        Log.d(TAG, "sendCurrentScreen($id, $name, $referrer, $packageName)")
    }

    override fun getAppInstanceId(app: AppMetadata): String? {
        Log.d(TAG, "getAppInstanceId() for $app")
        return null
    }

    override fun sendConditionalUserProperty(property: ConditionalUserPropertyParcel, app: AppMetadata) {
        Log.d(TAG, "sendConditionalUserProperty($property) for $app")
    }

    override fun getUserProperties(origin: String?, propertyNamePrefix: String?, includeInternal: Boolean, app: AppMetadata?): List<UserAttributeParcel> {
        Log.d(TAG, "getUserProperties($origin, $propertyNamePrefix, $includeInternal) for $app")
        return emptyList()
    }

    override fun getUserPropertiesAs(packageName: String?, origin: String?, propertyNamePrefix: String?, includeInternal: Boolean): List<UserAttributeParcel> {
        Log.d(TAG, "getUserPropertiesAs($packageName, $origin, $propertyNamePrefix, $includeInternal)")
        return emptyList()
    }

    override fun getConditionalUserProperties(origin: String?, propertyNamePrefix: String?, app: AppMetadata?): List<ConditionalUserPropertyParcel> {
        Log.d(TAG, "getConditionalUserProperties($origin, $propertyNamePrefix) for $app")
        return emptyList()
    }

    override fun getConditionalUserPropertiesAs(packageName: String?, origin: String?, propertyNamePrefix: String?): List<ConditionalUserPropertyParcel> {
        Log.d(TAG, "getConditionalUserPropertiesAs($packageName, $origin, $propertyNamePrefix)")
        return emptyList()
    }

    override fun reset(app: AppMetadata) {
        Log.d(TAG, "reset() for $app")
    }

    override fun sendDefaultEventParameters(params: Bundle, app: AppMetadata) {
        Log.d(TAG, "sendDefaultEventParameters($params) for $app")
    }

    override fun sendConsentSettings(app: AppMetadata) {
        Log.d(TAG, "sendConsentSettings() for $app")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
