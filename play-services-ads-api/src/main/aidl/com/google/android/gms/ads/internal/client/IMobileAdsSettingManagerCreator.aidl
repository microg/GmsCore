package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.IMobileAdsSettingManager;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IMobileAdsSettingManagerCreator {
    IMobileAdsSettingManager getMobileAdsSettingManager(IObjectWrapper context, int clientVersion);
}