package com.google.android.gms.ads.internal.client;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IMobileAdsSettingManagerCreator {
    IBinder getMobileAdsSettingManager(IObjectWrapper context, int clientVersion);
}