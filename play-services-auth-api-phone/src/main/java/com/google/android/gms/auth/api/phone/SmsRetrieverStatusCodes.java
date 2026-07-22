/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.phone;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * SMS Retriever specific status codes, for use in {@link Status#getStatusCode()}.
 */
public class SmsRetrieverStatusCodes extends CommonStatusCodes {
    /**
     * The current Android platform does not support this particular API.
     */
    public static final int PLATFORM_NOT_SUPPORTED = 36500;
    /**
     * The calling application is not eligible to use this particular API.
     * <p>
     * Note: For {@link SmsCodeAutofillClient}, this status indicates that the calling application is not the current user-designated
     * autofill service. For {@link SmsCodeBrowserClient}, it indicates that the caller is not the system default browser app.
     */
    public static final int API_NOT_AVAILABLE = 36501;
    /**
     * The user has not granted the calling application permission to use this particular API.
     */
    public static final int USER_PERMISSION_REQUIRED = 36502;

    /**
     * Returns an untranslated debug string based on the given status code.
     */
    @NonNull
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case PLATFORM_NOT_SUPPORTED:
                return "PLATFORM_NOT_SUPPORTED";
            case API_NOT_AVAILABLE:
                return "API_NOT_AVAILABLE";
            case USER_PERMISSION_REQUIRED:
                return "USER_PERMISSION_REQUIRED";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
