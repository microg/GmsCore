package com.google.android.gms.constellation.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;

interface IConstellationCallbacks {
    oneway void onGetIidToken(in Status status, in GetIidTokenResponse response) = 0;
    oneway void onVerifyPhoneNumber(in Status status, in VerifyPhoneNumberResponse response) = 1;
    oneway void onGetPnvCapabilities(in Status status, in GetPnvCapabilitiesResponse response) = 2;
}
