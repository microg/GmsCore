package com.google.android.gms.measurement.api.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.measurement.api.internal.IBundleReceiver;
import com.google.android.gms.measurement.api.internal.IEventHandlerProxy;
import com.google.android.gms.measurement.api.internal.IStringProvider;
import com.google.android.gms.measurement.api.internal.InitializationParams;

interface IAppMeasurementDynamiteService {
    void initialize(in IObjectWrapper context, in InitializationParams params, long eventTimeMillis) = 0;
    void logEvent(String origin, String name, in Bundle params, boolean z, boolean z2, long eventTimeMillis) = 1;
    void logEventAndBundle(String origin, String name, in Bundle params, IBundleReceiver receiver, long eventTimeMillis) = 2;
    void setUserProperty(String origin, String name, in IObjectWrapper value, boolean z, long eventTimeMillis) = 3;
    void getUserProperties(String origin, String propertyNamePrefix, boolean includeInternal, IBundleReceiver receiver) = 4;
    void getMaxUserProperties(String origin, IBundleReceiver receiver) = 5;
    void setUserId(String userId, long eventTimeMillis) = 6;
    void setConditionalUserProperty(in Bundle bundle, long eventTimeMillis) = 7;
    void clearConditionalUserProperty(String name, String eventName, in Bundle bundle) = 8;
    void getConditionalUserProperties(String origin, String propertyNamePrefix, IBundleReceiver receiver) = 9;
    void setMeasurementEnabled(boolean measurementEnabled, long eventTimeMillis) = 10;
    void resetAnalyticsData(long eventTimeMillis) = 11;
    void setMinimumSessionDuration(long minimumSessionDuration) = 12;
    void setSessionTimeoutDuration(long sessionTimeoutDuration) = 13;
    void setCurrentScreen(in IObjectWrapper obj, String screenName, String className, long eventTimeMillis) = 14;
    void getCurrentScreenName(IBundleReceiver receiver) = 15;
    void getCurrentScreenClass(IBundleReceiver receiver) = 16;
    void setInstanceIdProvider(IStringProvider provider) = 17;
    void getCachedAppInstanceId(IBundleReceiver receiver) = 18;
    void getAppInstanceId(IBundleReceiver receiver) = 19;
    void getGmpAppId(IBundleReceiver receiver) = 20;
    void generateEventId(IBundleReceiver receiver) = 21;
    void beginAdUnitExposure(String adUnitId, long eventElapsedRealtime) = 22;
    void endAdUnitExposure(String adUnitId, long eventElapsedRealtime) = 23;
    void onActivityStarted(in IObjectWrapper activity, long eventElapsedRealtime) = 24;
    void onActivityStopped(in IObjectWrapper activity, long eventElapsedRealtime) = 25;
    void onActivityCreated(in IObjectWrapper activity, in Bundle savedInstanceState, long eventElapsedRealtime) = 26;
    void onActivityDestroyed(in IObjectWrapper activity, long eventElapsedRealtime) = 27;
    void onActivityPaused(in IObjectWrapper activity, long eventElapsedRealtime) = 28;
    void onActivityResumed(in IObjectWrapper activity, long eventElapsedRealtime) = 29;
    void onActivitySaveInstanceState(in IObjectWrapper activity, IBundleReceiver receiver, long eventElapsedRealtime) = 30;
    void performAction(in Bundle bundle, IBundleReceiver receiver, long eventTimeMillis) = 31;
    void logHealthData(int i, String str, in IObjectWrapper obj, in IObjectWrapper obj2, in IObjectWrapper obj3) = 32;
    void setEventInterceptor(IEventHandlerProxy eventHandler) = 33;
    void registerOnMeasurementEventListener(IEventHandlerProxy eventHandler) = 34;
    void unregisterOnMeasurementEventListener(IEventHandlerProxy eventHandler) = 35;
    void initForTests(in Map map) = 36;
    void getTestFlag(IBundleReceiver receiver, int i) = 37;
    void setDataCollectionEnabled(boolean dataCollectionEnabled) = 38;
    void isDataCollectionEnabled(IBundleReceiver receiver) = 39;

    void setDefaultEventParameters(in Bundle bundle) = 41;
    void clearMeasurementEnabled(long eventTimeMillis) = 42;
    void setConsent(in Bundle bundle, long eventTimeMillis) = 43;
    void setConsentThirdParty(in Bundle bundle, long eventTimeMillis) = 44;
    void getSessionId(IBundleReceiver receiver) = 45;

    void setSgtmDebugInfo(in Intent intent) = 47;
}
