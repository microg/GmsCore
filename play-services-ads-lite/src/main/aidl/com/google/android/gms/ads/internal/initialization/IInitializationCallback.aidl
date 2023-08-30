package com.google.android.gms.ads.internal.initialization;

import com.google.android.gms.ads.internal.AdapterStatusParcel;

interface IInitializationCallback {
    void onInitialized(in List<AdapterStatusParcel> status);
}