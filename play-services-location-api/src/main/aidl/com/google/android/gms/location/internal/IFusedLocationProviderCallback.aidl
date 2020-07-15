package com.google.android.gms.location.internal;

import com.google.android.gms.location.internal.FusedLocationProviderResult;

interface IFusedLocationProviderCallback {
    void onFusedLocationProviderResult(in FusedLocationProviderResult result) = 0;
}
