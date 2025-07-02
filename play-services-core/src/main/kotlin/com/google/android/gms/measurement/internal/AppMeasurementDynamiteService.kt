/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.measurement.internal

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.measurement.api.internal.*
import org.microg.gms.utils.toHexString
import org.microg.gms.utils.warnOnTransactionIssues
import kotlin.random.Random

private const val TAG = "AppMeasurementService"

@Keep
class AppMeasurementDynamiteService : IAppMeasurementDynamiteService.Stub() {
    private var initialized: Boolean = false

    private fun returnBundle(receiver: IBundleReceiver?, bundle: Bundle?) = runCatching { receiver?.onBundle(bundle) }
    private fun returnResult(receiver: IBundleReceiver?, value: Any?) = returnBundle(receiver, bundleOf("r" to value))

    private fun generateSessionId(): Long {
        val min = 1_000_000_000L
        val max = 9_999_999_999L
        return (min..max).random()
    }

    private fun requireInitialized() {
        require(initialized) { "Attempting to perform action before initialize." }
    }

    override fun initialize(context: IObjectWrapper?, params: InitializationParams?, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: initialize")
        initialized = true
    }

    override fun logEvent(origin: String?, name: String?, params: Bundle?, z: Boolean, z2: Boolean, eventTimeMillis: Long) {
        requireInitialized()
        Log.d(TAG, "Not yet implemented: logEvent")
    }

    override fun logEventAndBundle(origin: String?, name: String?, params: Bundle?, receiver: IBundleReceiver?, eventTimeMillis: Long) {
        requireInitialized()
        Log.d(TAG, "Not yet implemented: logEventAndBundle")
        returnResult(receiver, ByteArray(0))
    }

    override fun setUserProperty(origin: String?, name: String?, value: IObjectWrapper?, z: Boolean, eventTimeMillis: Long) {
        requireInitialized()
        setUserProperty(origin, name, ObjectWrapper.unwrap(value), z, eventTimeMillis)
    }

    private fun setUserProperty(origin: String?, name: String?, value: Any?, z: Boolean, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setUserProperty($origin, $name, $value, $z)")
    }

    override fun getUserProperties(origin: String?, prefix: String?, includeInternal: Boolean, receiver: IBundleReceiver?) {
        requireInitialized()
        Log.d(TAG, "Not yet implemented: getUserProperties($origin, $prefix, $includeInternal)")
        returnBundle(receiver, Bundle())
    }

    override fun getMaxUserProperties(origin: String?, receiver: IBundleReceiver?) {
        requireInitialized()
        returnResult(receiver, 25)
    }

    override fun setUserId(userId: String?, eventTimeMillis: Long) {
        if (userId != null && userId.isEmpty()) {
            Log.w(TAG, "User ID must be non-empty or null")
        } else {
            Log.d(TAG, "Not yet implemented: setUserId($userId)")
            setUserProperty(null, "_id", userId, true, eventTimeMillis)
        }
    }

    override fun setConditionalUserProperty(bundle: Bundle?, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setConditionalUserProperty")
    }

    override fun clearConditionalUserProperty(name: String?, eventName: String?, bundle: Bundle?) {
        Log.d(TAG, "Not yet implemented: clearConditionalUserProperty($name, $eventName)")
    }

    override fun getConditionalUserProperties(origin: String?, prefix: String?, receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getConditionalUserProperties($origin, $prefix)")
        returnResult(receiver, arrayListOf<Bundle>())
    }

    override fun setMeasurementEnabled(measurementEnabled: Boolean, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setMeasurementEnabled($measurementEnabled)")
    }

    override fun resetAnalyticsData(eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: resetAnalyticsData")
    }

    override fun setMinimumSessionDuration(minimumSessionDuration: Long) {
        Log.d(TAG, "Not yet implemented: setMinimumSessionDuration($minimumSessionDuration)")
    }

    override fun setSessionTimeoutDuration(sessionTimeoutDuration: Long) {
        Log.d(TAG, "Not yet implemented: setSessionTimeoutDuration($sessionTimeoutDuration)")
    }

    override fun setCurrentScreen(obj: IObjectWrapper?, screenName: String?, className: String?, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setCurrentScreen($screenName, $className)")
    }

    override fun getCurrentScreenName(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCurrentScreenName")
        returnResult(receiver, null)
    }

    override fun getCurrentScreenClass(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCurrentScreenClass")
        returnResult(receiver, null)
    }

    override fun setInstanceIdProvider(provider: IStringProvider?) {
        Log.d(TAG, "Not yet implemented: setInstanceIdProvider")
    }

    override fun getCachedAppInstanceId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getCachedAppInstanceId")
        returnResult(receiver, null)
    }

    override fun getAppInstanceId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getAppInstanceId")
        // Generate a random ID -> equivalent to ephemeral app instance id
        // Correct behavior would be to generate appropriate AppMetadata and call IMeasurementService.getAppInstanceId
        val ephemeralAppInstanceId = Random.nextBytes(16).toHexString("")
        returnResult(receiver, ephemeralAppInstanceId)
    }

    override fun getGmpAppId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getGmpAppId")
        returnResult(receiver, null)
    }

    override fun generateEventId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: generateEventId")
        returnResult(receiver, 1L)
    }

    override fun beginAdUnitExposure(adUnitId: String?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: beginAdUnitExposure")
    }

    override fun endAdUnitExposure(adUnitId: String?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: endAdUnitExposure")
    }

    override fun onActivityStarted(activity: IObjectWrapper?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStarted")
    }

    override fun onActivityStopped(activity: IObjectWrapper?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStopped")
    }

    override fun onActivityCreated(activity: IObjectWrapper?, savedInstanceState: Bundle?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityCreated")
    }

    override fun onActivityDestroyed(activity: IObjectWrapper?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityDestroyed")
    }

    override fun onActivityPaused(activity: IObjectWrapper?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityPaused")
    }

    override fun onActivityResumed(activity: IObjectWrapper?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityResumed")
    }

    override fun onActivitySaveInstanceState(activity: IObjectWrapper?, receiver: IBundleReceiver?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivitySaveInstanceState")
        returnBundle(receiver, Bundle())
    }

    override fun performAction(bundle: Bundle?, receiver: IBundleReceiver?, eventTimeMillis: Long) {
        requireInitialized()
        returnBundle(receiver, null)
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
        when(i) {
            0 -> returnResult(receiver, "---")
            1 -> returnResult(receiver, -1L)
            2 -> returnResult(receiver, 3.0)
            3 -> returnResult(receiver, -2)
            4 -> returnResult(receiver, false)
        }
    }

    override fun setDataCollectionEnabled(dataCollectionEnabled: Boolean) {
        Log.d(TAG, "Not yet implemented: setDataCollectionEnabled")
    }

    override fun isDataCollectionEnabled(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: isDataCollectionEnabled")
        returnResult(receiver, false)
    }

    override fun setDefaultEventParameters(bundle: Bundle?) {
        Log.d(TAG, "Not yet implemented: setDefaultEventParameters")
    }

    override fun clearMeasurementEnabled(eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: clearMeasurementEnabled")
    }

    override fun setConsent(bundle: Bundle?, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setConsent")
    }

    override fun setConsentThirdParty(bundle: Bundle?, eventTimeMillis: Long) {
        Log.d(TAG, "Not yet implemented: setConsentThirdParty")
    }

    override fun getSessionId(receiver: IBundleReceiver?) {
        Log.d(TAG, "Not yet implemented: getSessionId")
        returnResult(receiver, generateSessionId())
    }

    override fun setSgtmDebugInfo(intent: Intent?) {
        Log.d(TAG, "Not yet implemented: setSgtmDebugInfo")
    }

    override fun setCurrentScreenByScionActivityInfo(info: ScionActivityInfo?, screenName: String?, className: String?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: setCurrentScreenByScionActivityInfo")
    }

    override fun onActivityStartedByScionActivityInfo(info: ScionActivityInfo?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStartedByScionActivityInfo")
    }

    override fun onActivityStoppedByScionActivityInfo(info: ScionActivityInfo?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityStoppedByScionActivityInfo")
    }

    override fun onActivityCreatedByScionActivityInfo(info: ScionActivityInfo?, savedInstanceState: Bundle?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityCreatedByScionActivityInfo")
    }

    override fun onActivityDestroyedByScionActivityInfo(info: ScionActivityInfo?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityDestroyedByScionActivityInfo")
    }

    override fun onActivityPausedByScionActivityInfo(info: ScionActivityInfo?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityPausedByScionActivityInfo")
    }

    override fun onActivityResumedByScionActivityInfo(info: ScionActivityInfo?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivityResumedByScionActivityInfo")
    }

    override fun onActivitySaveInstanceStateByScionActivityInfo(info: ScionActivityInfo?, receiver: IBundleReceiver?, eventElapsedRealtime: Long) {
        Log.d(TAG, "Not yet implemented: onActivitySaveInstanceStateByScionActivityInfo")
        returnBundle(receiver, Bundle())
    }

    override fun retrieveAndUploadBatches(callback: IDynamiteUploadBatchesCallback?) {
        Log.d(TAG, "Not yet implemented: retrieveAndUploadBatches")
        runCatching { callback?.onUploadBatches() }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
