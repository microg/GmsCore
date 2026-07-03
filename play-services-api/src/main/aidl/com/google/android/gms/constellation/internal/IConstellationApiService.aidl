package com.google.android.gms.constellation.internal;

import com.google.android.gms.constellation.internal.IConstellationCallbacks;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;

interface IConstellationApiService {
    oneway void getIidToken(IConstellationCallbacks callbacks, in GetIidTokenRequest request) = 0;
    oneway void verifyPhoneNumberV1(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request) = 1;
    oneway void verifyPhoneNumberSingleUse(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request) = 2;
    oneway void verifyPhoneNumber(IConstellationCallbacks callbacks, in VerifyPhoneNumberRequest request) = 3;
    oneway void getPnvCapabilities(IConstellationCallbacks callbacks, in GetPnvCapabilitiesRequest request) = 4;
}
