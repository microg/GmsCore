/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdErrorParcel;

interface IFullScreenContentCallback {
    void onAdFailedToShowFullScreenContent(in AdErrorParcel error) = 0;
    void onAdShowedFullScreenContent() = 1;
    void onAdDismissedFullScreenContent() = 2;
    void onAdImpression() = 3;
    void onAdClicked() = 4;
}
