/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.asterism.internal;

import com.google.android.gms.asterism.GetAsterismConsentRequest;
import com.google.android.gms.asterism.SetAsterismConsentRequest;
import com.google.android.gms.asterism.internal.IAsterismCallbacks;

interface IAsterismApiService {
    void getAsterismConsent(in GetAsterismConsentRequest request, IAsterismCallbacks callbacks);
    void setAsterismConsent(in SetAsterismConsentRequest request, IAsterismCallbacks callbacks);
}
