/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.carrierauth.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.carrierauth.CarrierAuthResult;

interface ICarrierAuthCallbacks {
    void onCarrierAuthResult(in Status status, in CarrierAuthResult result);
}
