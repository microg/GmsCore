/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal.client;

import com.google.android.gms.ads.internal.client.IAdPreloader;
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator;
import com.google.android.gms.dynamic.IObjectWrapper;

interface IAdPreloaderCreator {
    IAdPreloader createAdPreloader(IObjectWrapper context, IAdapterCreator adapterCreator, int clientVersion) = 0;
}
