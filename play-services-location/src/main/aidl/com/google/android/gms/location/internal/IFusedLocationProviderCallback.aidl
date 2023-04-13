package com.google.android.gms.location.internal;

import com.google.android.gms.location.internal.FusedLocationProviderResult;

interface IFusedLocationProviderCallback {
    oneway void onFusedLocationProviderResult(in FusedLocationProviderResult result) = 0;
}
