/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.carrierauth.internal;

import com.google.android.gms.carrierauth.CarrierAuthRequest;
import com.google.android.gms.carrierauth.internal.ICarrierAuthCallbacks;

interface ICarrierAuthApiService {
    void getCarrierAuthToken(in CarrierAuthRequest request, ICarrierAuthCallbacks callbacks);
}
