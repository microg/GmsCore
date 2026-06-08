/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdErrorParcel;

interface IAdListener {
    void onAdClosed() = 0;
    void onAdFailedToLoad(int errorCode) = 1;
    void onAdLeftApplication() = 2;
    void onAdOpened() = 3;
    void onAdLoaded() = 4;
    void onAdClicked() = 5;
    void onAdImpression() = 6;
    void onAdFailedToLoadWithAdError(in AdErrorParcel error) = 7;
}
