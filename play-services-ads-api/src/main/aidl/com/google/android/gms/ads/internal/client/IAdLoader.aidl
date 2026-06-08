/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.AdRequestParcel;

interface IAdLoader {
    void load(in AdRequestParcel request) = 0;
    String getMediationAdapterClassName() = 1;
    boolean isLoading() = 2;
    String getAdManagerAdapterClassName() = 3;
    void loadAds(in AdRequestParcel request, int count) = 4;
}
