package com.google.android.gms.ads.internal.rewarded.client;

import com.google.android.gms.ads.internal.AdRequestParcel;
import com.google.android.gms.ads.internal.ServerSideVerificationOptionsParcel;
import com.google.android.gms.ads.internal.client.IOnPaidEventListener;
import com.google.android.gms.ads.internal.client.IOnAdMetadataChangedListener;
import com.google.android.gms.ads.internal.client.IResponseInfo;
import com.google.android.gms.ads.internal.rewarded.client.IRewardedAdCallback;
import com.google.android.gms.ads.internal.rewarded.client.IRewardedAdLoadCallback;
import com.google.android.gms.ads.internal.rewarded.client.IRewardedAdSkuListener;
import com.google.android.gms.ads.internal.rewarded.client.IRewardItem;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IRewardedAd {
    void load(in AdRequestParcel request, IRewardedAdLoadCallback callback) = 0;
    void setCallback(IRewardedAdCallback callback) = 1;
    boolean canBeShown() = 2;
    String getMediationAdapterClassName() = 3;
    void show(IObjectWrapper activity) = 4;
    void setRewardedAdSkuListener(IRewardedAdSkuListener listener) = 5;
    void setServerSideVerificationOptions(in ServerSideVerificationOptionsParcel options) = 6;
    void setOnAdMetadataChangedListener(IOnAdMetadataChangedListener listener) = 7;
    Bundle getAdMetadata() = 8;
    void showWithImmersive(IObjectWrapper activity, boolean immersive) = 9;
    IRewardItem getRewardItem() = 10;
    IResponseInfo getResponseInfo() = 11;
    void setOnPaidEventListener(IOnPaidEventListener listener) = 12;
    void loadInterstitial(in AdRequestParcel request, IRewardedAdLoadCallback callback) = 13;
    void setImmersiveMode(boolean enabled) = 14;
}