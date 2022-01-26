/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.measurement.internal

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.measurement.api.internal.*

private const val TAG = "AppMeasurementService"

class AppMeasurementDynamiteService : IAppMeasurementDynamiteService.Stub() {
    override fun initialize(context: IObjectWrapper?, params: InitializationParams?, timestamp: Long) {
        Log.d(TAG, "Not yet implemented: initialize")
    }

    override fun logEvent(str: String?, str2: String?, bundle: Bundle?, z: Boolean, z2: Boolean, timestamp: Long) {
        Log.d(TAG, "Not yet implemented: logEvent")
    }

    override fun logEventAndBundle(str: String?, str2: String?, bundle: Bundle?, receiver: IBundleReceiver?, j: Long) {
        Log.d(TAG, "Not yet implemented: logEventAndBundle")
        receiver?.onBundle(Bundle().apply { putByteArray("r", ByteArray(0)) })
    }

    override fun setUserProperty(str: String?, str2: String?, obj: IObjectWrapper?, z: Boolean, j: Long) {
        Log.d(TAG, "Not yet implemented: setUserProperty")
    }

    override fun getUserProperties(str: String?, str2: String?, z: Boolean, receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getUserProperties")
        receiver?.onBundle(Bundle())
    }

    override fun getMaxUserProperties(str: String?, receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getMaxUserProperties")
        receiver?.onBundle(Bundle().apply { putInt("r", 25) })
    }

    override fun setUserId(str: String?, j: Long) {
        Log.d(TAG, "Not yet implemented: setUserId")
    }

    override fun setConditionalUserProperty(bundle: Bundle?, j: Long) {
        Log.d(TAG, "Not yet implemented: setConditionalUserProperty")
    }

    override fun clearConditionalUserProperty(str: String?, str2: String?, bundle: Bundle?) {
        Log.d(TAG, "Not yet implemented: clearConditionalUserProperty")
    }

    override fun getConditionalUserProperties(str: String?, str2: String?, receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getConditionalUserProperties")
        receiver?.onBundle(Bundle().apply { putParcelableArrayList("r", arrayListOf<Parcelable>()) })
    }

    override fun setMeasurementEnabled(z: Boolean, j: Long) {
        Log.d(TAG, "Not yet implemented: setMeasurementEnabled")
    }

    override fun resetAnalyticsData(j: Long) {
        Log.d(TAG, "Not yet implemented: resetAnalyticsData")
    }

    override fun setMinimumSessionDuration(j: Long) {
        Log.d(TAG, "Not yet implemented: setMinimumSessionDuration")
    }

    override fun setSessionTimeoutDuration(j: Long) {
        Log.d(TAG, "Not yet implemented: setSessionTimeoutDuration")
    }

    override fun setCurrentScreen(obj: IObjectWrapper?, str: String?, str2: String?, j: Long) {
        Log.d(TAG, "Not yet implemented: setCurrentScreen")
    }

    override fun getCurrentScreenName(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCurrentScreenName")
    }

    override fun getCurrentScreenClass(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCurrentScreenClass")
    }

    override fun setInstanceIdProvider(provider: IStringProvider?) {
        Log.d(TAG, "Not yet implemented: setInstanceIdProvider")
    }

    override fun getCachedAppInstanceId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCachedAppInstanceId")
    }

    override fun getAppInstanceId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getAppInstanceId")
    }

    override fun getGmpAppId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getGmpAppId")
    }

    override fun generateEventId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: generateEventId")
    }

    override fun beginAdUnitExposure(str: String?, j: Long) {
        Log.d(TAG, "Not yet implemented: beginAdUnitExposure")
    }

    override fun endAdUnitExposure(str: String?, j: Long) {
        Log.d(TAG, "Not yet implemented: endAdUnitExposure")
    }

    override fun onActivityStarted(activity: IObjectWrapper?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStarted")
    }

    override fun onActivityStopped(activity: IObjectWrapper?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStopped")
    }

    override fun onActivityCreated(activity: IObjectWrapper?, bundle: Bundle?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityCreated")
    }

    override fun onActivityDestroyed(activity: IObjectWrapper?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityDestroyed")
    }

    override fun onActivityPaused(activity: IObjectWrapper?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityPaused")
    }

    override fun onActivityResumed(activity: IObjectWrapper?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivityResumed")
    }

    override fun onActivitySaveInstanceState(activity: IObjectWrapper?, receiver: IBundleReceiver?, j: Long) {
        Log.d(TAG, "Not yet implemented: onActivitySaveInstanceState")
        receiver?.onBundle(Bundle())
    }

    override fun performAction(bundle: Bundle?, receiver: IBundleReceiver?, j: Long) {
        Log.d(TAG, "Not yet implemented: performAction")
    }

    override fun logHealthData(i: Int, str: String?, obj: IObjectWrapper?, obj2: IObjectWrapper?, obj3: IObjectWrapper?) {
        Log.d(TAG, "Not yet implemented: logHealthData")
    }

    override fun setEventInterceptor(proxy: IEventHandlerProxy?) {
        Log.d(TAG, "Not yet implemented: setEventInterceptor")
    }

    override fun registerOnMeasurementEventListener(proxy: IEventHandlerProxy?) {
        Log.d(TAG, "Not yet implemented: registerOnMeasurementEventListener")
    }

    override fun unregisterOnMeasurementEventListener(proxy: IEventHandlerProxy?) {
        Log.d(TAG, "Not yet implemented: unregisterOnMeasurementEventListener")
    }

    override fun initForTests(map: MutableMap<Any?, Any?>?) {
        Log.d(TAG, "Not yet implemented: initForTests")
    }

    override fun getTestFlag(receiver: IBundleReceiver?, i: Int) {
        Log.d(TAG, "Not yet implemented: getTestFlag")
    }

    override fun setDataCollectionEnabled(z: Boolean) {
        Log.d(TAG, "Not yet implemented: setDataCollectionEnabled")
    }

    override fun isDataCollectionEnabled(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: isDataCollectionEnabled")
        receiver?.onBundle(Bundle().apply { putBoolean("r", false) })
    }

    override fun setDefaultEventParameters(bundle: Bundle?) {
        Log.d(TAG, "Not yet implemented: setDefaultEventParameters")
    }

    override fun setConsent(bundle: Bundle?, j: Long) {
        Log.d(TAG, "Not yet implemented: setConsent")
    }

    override fun setConsentThirdParty(bundle: Bundle?, j: Long) {
        Log.d(TAG, "Not yet implemented: setConsentThirdParty")
    }

    override fun clearMeasurementEnabled(j: Long) {
        Log.d(TAG, "Not yet implemented: clearMeasurementEnabled")
    }

}
