package com.google.android.gms.constellation.internal;

import android.os.Bundle;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.ApiMetadata;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

interface IConstellationApiService {
    void verifyPhoneNumberV1(IConstellationCallbacks cb, in Bundle params, in ApiMetadata apiMetadata);
    void verifyPhoneNumberSingleUse(IConstellationCallbacks cb, in Bundle params, in ApiMetadata apiMetadata);
    void verifyPhoneNumber(IConstellationCallbacks cb, in VerifyPhoneNumberRequest request, in ApiMetadata apiMetadata);
    void getIidToken(IConstellationCallbacks cb, in GetIidTokenRequest request, in ApiMetadata apiMetadata);
    void getPnvCapabilities(IConstellationCallbacks cb, in GetPnvCapabilitiesRequest request, in ApiMetadata apiMetadata);
}
