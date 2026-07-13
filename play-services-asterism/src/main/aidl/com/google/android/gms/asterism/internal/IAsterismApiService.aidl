package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.internal.IAsterismCallbacks;
import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentRequest;

interface IAsterismApiService {
    void getAsterismConsent(
        IAsterismCallbacks cb,
        in GetAsterismConsentRequest request
    );
    void setAsterismConsent(
        IAsterismCallbacks cb,
        in SetAsterismConsentRequest request
    );
    void getIsPnvrConstellationDevice(
        IAsterismCallbacks cb
    );
}
