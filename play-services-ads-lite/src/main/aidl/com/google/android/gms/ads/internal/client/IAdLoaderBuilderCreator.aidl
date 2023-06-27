package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.meditation.client.IAdapterCreator;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IAdLoaderBuilderCreator {
    IBinder newAdLoaderBuilder(IObjectWrapper context, String adUnitId, IAdapterCreator adapterCreator, int clientVersion);
}