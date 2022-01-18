/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Status codes for the SafetyNet API.
 */
public class SafetyNetStatusCodes extends CommonStatusCodes {
    public static final int SAFE_BROWSING_UNSUPPORTED_THREAT_TYPES = 12000;
    public static final int SAFE_BROWSING_MISSING_API_KEYINT = 12001;
    public static final int SAFE_BROWSING_API_NOT_AVAILABLE = 12002;
    public static final int VERIFY_APPS_NOT_AVAILABLE = 12003;
    public static final int VERIFY_APPS_INTERNAL_ERROR = 12004;
    public static final int VERIFY_APPS_NOT_ENABLED = 12005;
    public static final int UNSUPPORTED_SDK_VERSION = 12006;
    /**
     * Cannot start the reCAPTCHA service because site key parameter is not valid.
     */
    public static final int RECAPTCHA_INVALID_SITEKEY = 12007;
    /**
     * Cannot start the reCAPTCHA service because type of site key is not valid.
     */
    public static final int RECAPTCHA_INVALID_KEYTYPE = 12008;
    public static final int SAFE_BROWSING_API_NOT_INITIALIZED = 12009;
    /**
     * Cannot start the reCAPTCHA service because calling package name is not matched with site key.
     */
    public static final int RECAPTCHA_INVALID_PACKAGE_NAME = 12013;
}
