/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement.utils;

import android.os.Build;
//import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

/** Provides API for debugging and not allow to debug on user build. */
public final class DebugUtils {
    private static final String TAG = "ServiceEntitlement";

    private static final String PROP_PII_LOGGABLE = "dbg.se.pii_loggable";
    private static final String BUILD_TYPE_USER = "user";
    private static final String PROP_FAKE_EAP_AKA_RESPONSE =
            "persist.entitlement.fake_eap_aka_response";

    private DebugUtils() {}

    /** Logs PII data if allowed. */
    public static void logPii(String message) {
        if (isPiiLoggable()) {
            Log.i(TAG, message);
        }
    }

    /**
     * Get the bypass EAP-AKA response. This is only available on debug builds and can be set by
     * running the following commands, where {@code response} should be the expected response from
     * an EAP-AKA request:
     * adb root
     * adb shell setprop persist.entitlement.fake_eap_aka_response response
     *
     * @return The bypass EAP-AKA response, or an empty string if it is either not set or the device
     * is not on a debug build.
     */
    @NonNull
    public static String getBypassEapAkaResponse() {
        String bypassResponse = "";//SystemProperties.get(PROP_FAKE_EAP_AKA_RESPONSE);
        if (TextUtils.isEmpty(bypassResponse) || !isDebugBuild()) {
            return "";
        }
        return bypassResponse;
    }

    private static boolean isDebugBuild() {
        return !BUILD_TYPE_USER.equals(Build.TYPE);
    }

    private static boolean isPiiLoggable() {
//        if (!isDebugBuild()) {
//            return false;
//        }
//
        return true; //SystemProperties.getBoolean(PROP_PII_LOGGABLE, false);
    }
}
