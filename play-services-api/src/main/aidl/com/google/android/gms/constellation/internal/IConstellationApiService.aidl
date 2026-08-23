/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation.internal;

import com.google.android.gms.constellation.PhoneNumberVerificationRequest;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

interface IConstellationApiService {
    void verifyPhoneNumber(in PhoneNumberVerificationRequest request, IConstellationCallbacks callbacks);
    void getVerificationStatus(String phoneNumber, IConstellationCallbacks callbacks);
}
