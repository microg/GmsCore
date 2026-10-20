/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.appopen.client;

import com.google.android.gms.ads.internal.AdErrorParcel;

/** Callback registered on IAdManager transaction 40 by recent Google Mobile Ads SDKs. */
interface IAppOpenAdLoadCallback {
    void onAdFailedToLoad(in AdErrorParcel error) = 2;
}
