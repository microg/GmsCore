package com.google.android.gms.asterism.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.asterism.GetAsterismConsentResponse;
import com.google.android.gms.asterism.SetAsterismConsentResponse;

interface IAsterismCallbacks {
    oneway void onGetAsterismConsent(in Status status, in GetAsterismConsentResponse response) = 0;
    oneway void onSetAsterismConsent(in Status status, in SetAsterismConsentResponse response) = 1;
}
