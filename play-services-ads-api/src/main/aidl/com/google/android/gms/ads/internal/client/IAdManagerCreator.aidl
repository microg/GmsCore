/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.AdSizeParcel;
import com.google.android.gms.ads.internal.client.IAdManager;
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IAdManagerCreator {
    IAdManager newAdManager(IObjectWrapper context, in AdSizeParcel adSize, String adUnitId, IAdapterCreator adapterCreator, int clientVersion) = 0;
    IAdManager newAdManagerByType(IObjectWrapper context, in AdSizeParcel adSize, String adUnitId, IAdapterCreator adapterCreator, int clientVersion, int type) = 1;
}
