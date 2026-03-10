/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.chimera;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.Keep;

import android.content.Context;

@Keep
public class DynamiteModuleInitializer {
    private static final String TAG = "DynamiteModule";

    public static void initializeModuleV1(Context context) {
        initializeModuleV2(context, "com.google.android.gms".equals(context.getPackageName()));
    }

    public static void initializeModuleV2(Context context, boolean withGmsPackage) {
        Log.d(TAG, "initializeModuleV2 context: " + context + ", withGmsPackage: " + withGmsPackage);
    }
}
