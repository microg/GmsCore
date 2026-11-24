package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.GetAsterismConsentResponse;
import com.google.android.gms.asterism.SetAsterismConsentResponse;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;

oneway interface IAsterismCallbacks {
    void onConsentFetched(in Status status, in GetAsterismConsentResponse response, in ApiMetadata metadata) = 0;
    void onConsentRegistered(in Status status, in SetAsterismConsentResponse response, in ApiMetadata metadata) = 1;
    void onIsPnvrConstellationDevice(in Status status, boolean result, in ApiMetadata metadata) = 2;
}
