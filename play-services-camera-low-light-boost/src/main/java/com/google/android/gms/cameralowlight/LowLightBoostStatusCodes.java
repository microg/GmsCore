/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.cameralowlight;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;

import org.microg.gms.common.PublicApi;

/**
 * Status codes reported by Camera Low Light Boost operations.
 */
@PublicApi
public class LowLightBoostStatusCodes extends CommonStatusCodes {
    public static final int MAX_SESSIONS_REACHED = 52501;
    public static final int GLOBAL_INIT_FAILED = 52502;
    public static final int SESSION_INIT_FAILED = 52503;
    public static final int SERVICE_RELEASED = 52504;
    public static final int CAMERA_NOT_SUPPORTED = 52505;
    public static final int UNSUPPORTED_DATASPACE = 52506;
    public static final int RENDER_FAILED = 52507;
    public static final int MISSING_CAMERA_CHARACTERISTICS = 52508;
    public static final int INVALID_FRAME_METADATA = 52509;
    public static final int INTERNAL_ERROR = 52510;
    public static final int BINDER_DIED = 52516;

    protected LowLightBoostStatusCodes() {
    }

    @NonNull
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case MAX_SESSIONS_REACHED:
                return "MAX_SESSIONS_REACHED";
            case GLOBAL_INIT_FAILED:
                return "GLOBAL_INIT_FAILED";
            case SESSION_INIT_FAILED:
                return "SESSION_INIT_FAILED";
            case SERVICE_RELEASED:
                return "SERVICE_RELEASED";
            case CAMERA_NOT_SUPPORTED:
                return "CAMERA_NOT_SUPPORTED";
            case UNSUPPORTED_DATASPACE:
                return "UNSUPPORTED_DATASPACE";
            case RENDER_FAILED:
                return "RENDER_FAILED";
            case MISSING_CAMERA_CHARACTERISTICS:
                return "MISSING_CAMERA_CHARACTERISTICS";
            case INVALID_FRAME_METADATA:
                return "INVALID_FRAME_METADATA";
            case INTERNAL_ERROR:
                return "INTERNAL_ERROR";
            case BINDER_DIED:
                return "BINDER_DIED";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
