package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;

oneway interface IConstellationCallbacks {
    void onPhoneNumberVerified(in Status status, in List<PhoneNumberInfo> phoneNumbers, in ApiMetadata apiMetadata);
    void onPhoneNumberVerificationsCompleted(in Status status, in VerifyPhoneNumberResponse response, in ApiMetadata apiMetadata);
    void onIidTokenGenerated(in Status status, in GetIidTokenResponse response, in ApiMetadata apiMetadata);
    void onGetPnvCapabilitiesCompleted(in Status status, in GetPnvCapabilitiesResponse response, in ApiMetadata apiMetadata);
}
