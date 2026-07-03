package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.internal.IAsterismCallbacks;
import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentRequest;

interface IAsterismApiService {
    oneway void getAsterismConsent(IAsterismCallbacks callbacks, in GetAsterismConsentRequest request) = 0;
    oneway void setAsterismConsent(IAsterismCallbacks callbacks, in SetAsterismConsentRequest request) = 1;
}
