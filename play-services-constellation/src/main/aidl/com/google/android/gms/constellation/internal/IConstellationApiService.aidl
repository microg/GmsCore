/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.GetPnvCapabilitiesRequest;
import com.google.android.gms.constellation.GetPnvCapabilitiesResponse;
import com.google.android.gms.constellation.PhoneNumberInfo;
import com.google.android.gms.constellation.VerifyPhoneNumberRequest;
import com.google.android.gms.constellation.VerifyPhoneNumberResponse;

/**
 * AIDL interface for the Constellation API service.
 * Constellation handles RCS phone number verification, IID token management,
 * and phone number capability resolution for Google Messages.
 */
interface IConstellationApiService {
    VerifyPhoneNumberResponse verifyPhoneNumber(in VerifyPhoneNumberRequest request) = 0;
    GetPnvCapabilitiesResponse getPnvCapabilities(in GetPnvCapabilitiesRequest request) = 1;
    GetIidTokenResponse getIidToken(in GetIidTokenRequest request) = 2;
    void registerCallbacks(in IConstellationCallbacks callbacks) = 3;
    void unregisterCallbacks(in IConstellationCallbacks callbacks) = 4;
    PhoneNumberInfo getPhoneNumberInfo(String phoneNumber) = 5;
}
