package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;

interface IConstellationApiService {
    void verifyPhoneNumberV1(
        IConstellationCallbacks cb,
        in Bundle bundle,
        in ApiMetadata apiMetadata
    );

    void verifyPhoneNumberSingleUse(
        IConstellationCallbacks cb,
        in Bundle bundle,
        in ApiMetadata apiMetadata
    );

    void verifyPhoneNumber(
        IConstellationCallbacks cb,
        in VerifyPhoneNumberRequest request,
        in ApiMetadata apiMetadata
    );

    void getIidToken(
        IConstellationCallbacks cb,
        in GetIidTokenRequest request,
        in ApiMetadata apiMetadata
    );

    void getPnvCapabilities(
        IConstellationCallbacks cb,
        in GetPnvCapabilitiesRequest request,
        in ApiMetadata apiMetadata
    );
}
