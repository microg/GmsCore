package com.google.android.gms.asterism.internal;

import com.google.android.gms.common.api.Status;

import com.google.android.gms.asterism.GetAsterismConsentResponse;
import com.google.android.gms.asterism.SetAsterismConsentResponse;

interface IAsterismCallbacks {
    void onConsentFetched(in Status status, in GetAsterismConsentResponse response);
    void onConsentRegistered(in Status status, in SetAsterismConsentResponse response);
    void onIsPnvrConstellationDevice(in Status status, boolean isPnvrDevice);
}