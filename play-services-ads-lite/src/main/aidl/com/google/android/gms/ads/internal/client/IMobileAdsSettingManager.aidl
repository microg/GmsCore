package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdapterStatusParcel;
import com.google.android.gms.ads.internal.RequestConfigurationParcel;
import com.google.android.gms.ads.internal.client.IOnAdInspectorClosedListener;
import com.google.android.gms.ads.internal.initialization.IInitializationCallback;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IMobileAdsSettingManager {
    void initialize() = 0;
    void setAppVolume(float volume) = 1;
    void setAppMuted(boolean muted) = 3;
    void openDebugMenu(IObjectWrapper context, String adUnitId) = 4;
    void fetchAppSettings(String appId, IObjectWrapper runnable) = 5;
    float getAdVolume() = 6;
    boolean isAdMuted() = 7;
    String getVersionString() = 8;
    void registerRtbAdapter(String className) = 9;
    void addInitializationCallback(IInitializationCallback callback) = 11;
    List<AdapterStatusParcel> getAdapterStatus() = 12;
    void setRequestConfiguration(in RequestConfigurationParcel configuration) = 13;
    void disableMediationAdapterInitialization() = 14;
    void openAdInspector(IOnAdInspectorClosedListener listener) = 15;
    void enableSameAppKey(boolean enabled) = 16;
    void setPlugin(String plugin) = 17;
}