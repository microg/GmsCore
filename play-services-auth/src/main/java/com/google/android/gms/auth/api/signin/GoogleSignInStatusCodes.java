/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.signin;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * Google Sign In specific status codes, for use in {@link Status#getStatusCode()}.
 * <p>
 * In addition to codes defined in this class, you might also want to check:
 * <ul>
 *     <li>{@link CommonStatusCodes#SIGN_IN_REQUIRED}</li>
 *     <li>{@link CommonStatusCodes#NETWORK_ERROR}</li>
 *     <li>{@link CommonStatusCodes#INVALID_ACCOUNT}</li>
 *     <li>{@link CommonStatusCodes#INTERNAL_ERROR}</li>
 * </ul>
 */
@Deprecated
public class GoogleSignInStatusCodes extends CommonStatusCodes {
    private GoogleSignInStatusCodes() {
        // Disallow instantiation
    }

    /**
     * The sign in attempt didn't succeed with the current account.
     * <p>
     * Unlike {@link CommonStatusCodes#SIGN_IN_REQUIRED}. when seeing this error code, there is nothing user can do to recover from the sign in
     * failure. Switching to another account may or may not help. Check adb log to see details if any.
     */
    public static final int SIGN_IN_FAILED = 12500;
    /**
     * The sign in was cancelled by the user. i.e. user cancelled some of the sign in resolutions, e.g. account picking or OAuth consent.
     */
    public static final int SIGN_IN_CANCELLED = 12501;
    /**
     * A sign in process is currently in progress and the current one cannot continue. e.g. the user clicks the SignInButton multiple times and more
     * than one sign in intent was launched.
     */
    public static final int SIGN_IN_CURRENTLY_IN_PROGRESS = 12502;

    /**
     * Returns an untranslated debug (not user-friendly) string based on the current status code.
     */
    @NonNull
    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case SIGN_IN_FAILED:
                return "A non-recoverable sign in failure occurred";
            case SIGN_IN_CANCELLED:
                return "Sign in action cancelled";
            case SIGN_IN_CURRENTLY_IN_PROGRESS:
                return "Sign-in in progress";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
