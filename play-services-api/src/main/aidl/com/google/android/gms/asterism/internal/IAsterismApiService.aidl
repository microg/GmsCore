package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.internal.IAsterismCallbacks;
import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentRequest;
import com.google.android.gms.common.api.ApiMetadata;

interface IAsterismApiService {
    void getAsterismConsent(IAsterismCallbacks callbacks, in GetAsterismConsentRequest request, in ApiMetadata metadata) = 0;
    void setAsterismConsent(IAsterismCallbacks callbacks, in SetAsterismConsentRequest request, in ApiMetadata metadata) = 1;
    void getIsPnvrConstellationDevice(IAsterismCallbacks callbacks, in ApiMetadata metadata) = 2;
}
