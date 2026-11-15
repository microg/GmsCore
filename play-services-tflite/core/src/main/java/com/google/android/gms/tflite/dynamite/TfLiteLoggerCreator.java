/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tflite.dynamite;

import androidx.annotation.Keep;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;
import com.google.android.gms.tflite.dynamite.internal.CustomerInfo;

import org.microg.gms.tflite.TfLiteLogger;

@Keep
public final class TfLiteLoggerCreator extends ITfLiteLoggerCreator.Stub {
    @Override
    public IObjectWrapper createRemoteLogger(IObjectWrapper context, CustomerInfo customerInfo) {
        TfLiteDynamiteLoaderImpl.checkClient(context, customerInfo);
        return ObjectWrapper.wrap(new TfLiteLogger());
    }
}
