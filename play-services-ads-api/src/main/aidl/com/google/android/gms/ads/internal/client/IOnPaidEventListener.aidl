/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.AdValueParcel;

interface IOnPaidEventListener {
    void onPaidEvent(in AdValueParcel adValue) = 0;
    boolean isLoggingLimitedToImpressionEvents() = 1;
}
