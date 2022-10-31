package com.google.android.gms.measurement.api.internal;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.measurement.api.internal.IBundleReceiver;
import com.google.android.gms.measurement.api.internal.IEventHandlerProxy;
import com.google.android.gms.measurement.api.internal.IStringProvider;
import com.google.android.gms.measurement.api.internal.InitializationParams;

interface IAppMeasurementDynamiteService {
    void initialize(in IObjectWrapper context, in InitializationParams params, long timestamp) = 0;
    void logEvent(String str, String str2, in Bundle bundle, boolean z, boolean z2, long timestamp) = 1;
    void logEventAndBundle(String str, String str2, in Bundle bundle, IBundleReceiver receiver, long j) = 2;
    void setUserProperty(String str, String str2, in IObjectWrapper obj, boolean z, long j) = 3;
    void getUserProperties(String str, String str2, boolean z, IBundleReceiver receiver) = 4;
    void getMaxUserProperties(String str, IBundleReceiver receiver) = 5;
    void setUserId(String str, long j) = 6;
    void setConditionalUserProperty(in Bundle bundle, long j) = 7;
    void clearConditionalUserProperty(String str, String str2, in Bundle bundle) = 8;
    void getConditionalUserProperties(String str, String str2, IBundleReceiver receiver) = 9;
    void setMeasurementEnabled(boolean z, long j) = 10;
    void resetAnalyticsData(long j) = 11;
    void setMinimumSessionDuration(long j) = 12;
    void setSessionTimeoutDuration(long j) = 13;
    void setCurrentScreen(in IObjectWrapper obj, String str, String str2, long j) = 14;
    void getCurrentScreenName(IBundleReceiver receiver) = 15;
    void getCurrentScreenClass(IBundleReceiver receiver) = 16;
    void setInstanceIdProvider(IStringProvider provider) = 17;
    void getCachedAppInstanceId(IBundleReceiver receiver) = 18;
    void getAppInstanceId(IBundleReceiver receiver) = 19;
    void getGmpAppId(IBundleReceiver receiver) = 20;
    void generateEventId(IBundleReceiver receiver) = 21;
    void beginAdUnitExposure(String str, long j) = 22;
    void endAdUnitExposure(String str, long j) = 23;
    void onActivityStarted(in IObjectWrapper activity, long j) = 24;
    void onActivityStopped(in IObjectWrapper activity, long j) = 25;
    void onActivityCreated(in IObjectWrapper activity, in Bundle bundle, long j) = 26;
    void onActivityDestroyed(in IObjectWrapper activity, long j) = 27;
    void onActivityPaused(in IObjectWrapper activity, long j) = 28;
    void onActivityResumed(in IObjectWrapper activity, long j) = 29;
    void onActivitySaveInstanceState(in IObjectWrapper activity, IBundleReceiver receiver, long j) = 30;
    void performAction(in Bundle bundle, IBundleReceiver receiver, long j) = 31;
    void logHealthData(int i, String str, in IObjectWrapper obj, in IObjectWrapper obj2, in IObjectWrapper obj3) = 32;
    void setEventInterceptor(IEventHandlerProxy proxy) = 33;
    void registerOnMeasurementEventListener(IEventHandlerProxy proxy) = 34;
    void unregisterOnMeasurementEventListener(IEventHandlerProxy proxy) = 35;
    void initForTests(in Map map) = 36;
    void getTestFlag(IBundleReceiver receiver, int i) = 37;
    void setDataCollectionEnabled(boolean z) = 38;
    void isDataCollectionEnabled(IBundleReceiver receiver) = 39;

    void setDefaultEventParameters(in Bundle bundle) = 41;
    void setConsent(in Bundle bundle, long j) = 43;
    void setConsentThirdParty(in Bundle bundle, long j) = 44;
    void clearMeasurementEnabled(long j) = 42;
}
