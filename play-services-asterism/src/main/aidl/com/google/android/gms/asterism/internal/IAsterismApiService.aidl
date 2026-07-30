/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.GetAsterismConsentResponse;
import com.google.android.gms.asterism.SetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentResponse;

/**
 * AIDL interface for the Asterism API service.
 * Manages RCS Asterism consent state for phone number verification features.
 */
interface IAsterismApiService {
    GetAsterismConsentResponse getAsterismConsent(in GetAsterismConsentRequest request) = 0;
    SetAsterismConsentResponse setAsterismConsent(in SetAsterismConsentRequest request) = 1;
    void registerCallbacks(in IAsterismCallbacks callbacks) = 2;
    void unregisterCallbacks(in IAsterismCallbacks callbacks) = 3;
}
