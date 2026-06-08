package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdErrorParcel;

interface IOnAdInspectorClosedListener {
    void onAdInspectorClosed(in @nullable AdErrorParcel adErrorParcel);
}