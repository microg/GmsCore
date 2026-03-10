package com.google.android.gms.facs.cache.internal;

import com.google.android.gms.facs.cache.FacsCacheCallOptions;
import com.google.android.gms.facs.cache.internal.IFacsCacheCallbacks;

interface IFacsCacheService {
    void forceSettingsCacheRefresh(IFacsCacheCallbacks callbacks, in FacsCacheCallOptions options) = 0;
    void updateActivityControlsSettings(IFacsCacheCallbacks callbacks, in byte[] bytes, in FacsCacheCallOptions options) = 1;
    void getActivityControlsSettings(IFacsCacheCallbacks callbacks, in FacsCacheCallOptions options) = 2;
    void readDeviceLevelSettings(IFacsCacheCallbacks callbacks) = 3;
    void writeDeviceLevelSettings(IFacsCacheCallbacks callbacks, in byte[] bytes) = 4;
}
