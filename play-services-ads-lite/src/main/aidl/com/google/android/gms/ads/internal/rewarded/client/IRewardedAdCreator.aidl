package com.google.android.gms.ads.internal.rewarded.client;

import com.google.android.gms.ads.internal.meditation.client.IAdapterCreator;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IRewardedAdCreator {
    IBinder newRewardedAd(IObjectWrapper context, String str, IAdapterCreator adapterCreator, int clientVersion);
}