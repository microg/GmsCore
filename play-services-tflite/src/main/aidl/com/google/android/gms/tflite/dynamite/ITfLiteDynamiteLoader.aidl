/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tflite.dynamite;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.tflite.dynamite.internal.CustomerInfo;

interface ITfLiteDynamiteLoader {
    long loadTfLiteLibrary(in IObjectWrapper context, in CustomerInfo customerInfo) = 1;
}
