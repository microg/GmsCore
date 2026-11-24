package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;

/**
 * Constellation callbacks.
 */
oneway interface IConstellationCallbacks {
    void onPhoneNumberVerified(in Status status, in List<PhoneNumberInfo> phoneNumbers, in ApiMetadata metadata) = 0;
    void onPhoneNumberVerificationsCompleted(in Status status, in VerifyPhoneNumberResponse response, in ApiMetadata metadata) = 1;
    void onIidTokenGenerated(in Status status, in GetIidTokenResponse response, in ApiMetadata metadata) = 2;
    void onGetPnvCapabilitiesCompleted(in Status status, in GetPnvCapabilitiesResponse response, in ApiMetadata metadata) = 3;
}
