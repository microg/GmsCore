package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.IAdLoader;

interface IAdLoaderBuilder {
    IAdLoader build() = 0;
    void setAdListener(IBinder listener) = 1;
    void setUnifiedNativeAdLoadedListener(IBinder listener) = 9;
}
