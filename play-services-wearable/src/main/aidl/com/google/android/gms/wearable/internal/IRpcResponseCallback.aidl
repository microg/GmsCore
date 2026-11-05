package com.google.android.gms.wearable.internal;

oneway interface IRpcResponseCallback {
    void onResponse(boolean success, in byte[] data) = 0;
}
