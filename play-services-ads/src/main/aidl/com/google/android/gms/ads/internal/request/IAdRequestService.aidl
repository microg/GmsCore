package com.google.android.gms.ads.internal.request;

import com.google.android.gms.ads.internal.NonagonRequestParcel;
import com.google.android.gms.ads.internal.request.INonagonStreamingResponseListener;

interface IAdRequestService {
    void getAdRequest(in NonagonRequestParcel request, INonagonStreamingResponseListener listener) = 3;
    void getSignals(in NonagonRequestParcel request, INonagonStreamingResponseListener listener) = 4;
    void getUrlAndCacheKey(in NonagonRequestParcel request, INonagonStreamingResponseListener listener) = 5;
    void removeCacheUrl(String key, INonagonStreamingResponseListener listener) = 6;
}