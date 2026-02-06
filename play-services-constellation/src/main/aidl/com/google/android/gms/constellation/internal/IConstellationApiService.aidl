package com.google.android.gms.constellation.internal;

import android.os.Bundle;
import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

interface IConstellationApiService {
//    void verifyPhoneNumberV1(IConstellationCallbacks callbacks, in Bundle bundle, in ApiMetadata apiMetadata) = 0;
//    void verifyPhoneNumberSingleUse(IConstellationCallbacks callbacks, in Bundle bundle, in ApiMetadata apiMetadata) = 1;
    void verifyPhoneNumber(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request, in ApiMetadata apiMetadata) = 2;
    void getIidToken(IConstellationCallbacks callbacks, in GetIidTokenRequest request, in ApiMetadata apiMetadata) = 3;
//    void getPnvCapabilities(IConstellationCallbacks callbacks, in GetPnvCapabilitiesRequest request, in ApiMetadata apiMetadata) = 4;
}

