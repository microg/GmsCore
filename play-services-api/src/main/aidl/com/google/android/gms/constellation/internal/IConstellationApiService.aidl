package com.google.android.gms.constellation.internal;

import android.os.Bundle;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;

/**
 * Constellation API service for phone number verification.
 */
interface IConstellationApiService {
    void verifyPhoneNumberV1(IConstellationCallbacks callbacks, in Bundle bundle, in ApiMetadata metadata) = 0;
    void verifyPhoneNumberSingleUse(IConstellationCallbacks callbacks, in Bundle bundle, in ApiMetadata metadata) = 1;
    void verifyPhoneNumber(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request, in ApiMetadata metadata) = 2;
    void getIidToken(IConstellationCallbacks callbacks, in GetIidTokenRequest request, in ApiMetadata metadata) = 3;
    void getPnvCapabilities(IConstellationCallbacks callbacks, in GetPnvCapabilitiesRequest request, in ApiMetadata metadata) = 4;
}
