package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.ApiMetadata;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;

oneway interface IConstellationCallbacks {
    void onPhoneNumberVerified(in Status status, in List<PhoneNumberInfo> phoneNumberInfos, in ApiMetadata apiMetadata);
    void onPhoneNumberVerificationsCompleted(in Status status, in VerifyPhoneNumberResponse response, in ApiMetadata apiMetadata);
    void onIidTokenGenerated(in Status status, in GetIidTokenResponse response, in ApiMetadata apiMetadata);
    void onGetPnvCapabilitiesCompleted(in Status status, in GetPnvCapabilitiesResponse response, in ApiMetadata apiMetadata);
}
