package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdRequestParcel;

interface IAdLoader {
    void load(in AdRequestParcel request) = 0;
    boolean isLoading() = 2;
    void loadAds(in AdRequestParcel request, int count) = 4;
}
