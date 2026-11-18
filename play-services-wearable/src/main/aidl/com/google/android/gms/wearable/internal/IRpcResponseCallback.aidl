package com.google.android.gms.wearable.internal;

interface IRpcResponseCallback {
    void onResponse(boolean success, in byte[] data) = 0;
}