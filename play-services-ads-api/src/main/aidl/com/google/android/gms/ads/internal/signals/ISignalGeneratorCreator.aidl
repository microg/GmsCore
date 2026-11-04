/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.internal.signals;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator;
import com.google.android.gms.ads.internal.signals.ISignalGenerator;

interface ISignalGeneratorCreator {
    ISignalGenerator newSignalGenerator(IObjectWrapper context, IAdapterCreator adapterCreator, int clientVersion) = 1;
}
