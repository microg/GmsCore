package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdapterResponseInfoParcel;

interface IResponseInfo {
    String getMediationAdapterClassName() = 0;
    String getResponseId() = 1;
    List<AdapterResponseInfoParcel> getAdapterResponseInfo() = 2;
    AdapterResponseInfoParcel getLoadedAdapterResponse() = 3;
    Bundle getResponseExtras() = 4;
}