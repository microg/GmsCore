/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.internal.signals;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.ads.internal.signals.ISignalCallback;
import com.google.android.gms.ads.internal.signals.SignalRequestParcel;

interface ISignalGenerator {
    void generateSignals(IObjectWrapper context, in SignalRequestParcel request, ISignalCallback callback) = 0;
    void registerWebView(IObjectWrapper webView) = 7;
}
