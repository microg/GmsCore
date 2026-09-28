package com.google.android.gms.measurement.internal;

import com.google.android.gms.measurement.internal.TriggerUriParcel;

interface ITriggerUrisCallback {
    void onTriggerUris(in List<TriggerUriParcel> uris) = 1;
}
