package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;

interface IConstellationCallbacks {
    oneway void onPhoneNumberVerified(in Status status, in List<PhoneNumberInfo> list, in ApiMetadata apiMetadata) = 0;
    oneway void onPhoneNumberVerificationsCompleted(in Status status, in VerifyPhoneNumberResponse response, in ApiMetadata apiMetadata) = 1;
    oneway void onIidTokenGenerated(in Status status, in GetIidTokenResponse response, in ApiMetadata apiMetadata) = 2;
    oneway void onGetPnvCapabilitiesCompleted(in Status status, in GetPnvCapabilitiesResponse response, in ApiMetadata apiMetadata) = 3;
}

