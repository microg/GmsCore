/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tflite.dynamite;

import androidx.annotation.Keep;

/**
 * Entry point for the TFLite CPU Dynamite API.
 */
@Keep
public final class TfLiteNativeApi {
    private static long api;

    private TfLiteNativeApi() {
    }

    public static synchronized long getApi() {
        if (api == 0) {
            System.loadLibrary("microg_tflite");
            long loadedApi = getApiNative();
            if (loadedApi == 0) throw new UnsatisfiedLinkError("TFLite CPU API is unavailable");
            api = loadedApi;
        }
        return api;
    }

    private static native long getApiNative();
}
