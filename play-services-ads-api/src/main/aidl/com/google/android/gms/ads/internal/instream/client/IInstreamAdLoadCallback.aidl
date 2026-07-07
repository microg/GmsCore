/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.instream.client;

import com.google.android.gms.ads.internal.instream.client.IInstreamAd;

interface IInstreamAdLoadCallback {
    void onInstreamAdLoaded(IInstreamAd ad) = 0;
    void onInstreamAdFailedToLoad(int errorCode) = 1;
}
