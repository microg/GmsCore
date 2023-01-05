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
    /**
     * None of the input threat types to {@code lookupUri(String, String, int...)} are supported.
     */
    public static final int SAFE_BROWSING_UNSUPPORTED_THREAT_TYPES = 12000;
    /**
     * The API key required for calling {@code lookupUri(String, String, int...)} is missing in the manifest.
     * <p>
     * A meta-data name-value pair in the app manifest with the name "com.google.android.safetynet.API_KEY" and a value
     * consisting of the API key from the Google Developers Console is not present.
     */
    public static final int SAFE_BROWSING_MISSING_API_KEY = 12001;
    /**
     * An internal error occurred causing the call to {@code lookupUri(String, String, int...)} to be unavailable.
     */
    public static final int SAFE_BROWSING_API_NOT_AVAILABLE = 12002;
    /**
     * Verify Apps is not supported on this device.
     */
    public static final int VERIFY_APPS_NOT_AVAILABLE = 12003;
    /**
     * An internal error occurred while using the Verify Apps API.
     */
    public static final int VERIFY_APPS_INTERNAL_ERROR = 12004;
    /**
     * Cannot list potentially harmful apps because Verify Apps is not enabled.
     * <p>
     * The developer may call {@code enableVerifyApps()} to request the user turn on Verify Apps.
     */
    public static final int VERIFY_APPS_NOT_ENABLED = 12005;
    /**
     * User device SDK version is not supported.
     */
    public static final int UNSUPPORTED_SDK_VERSION = 12006;
    /**
     * Cannot start the reCAPTCHA service because site key parameter is not valid.
     */
    public static final int RECAPTCHA_INVALID_SITEKEY = 12007;
    /**
     * Cannot start the reCAPTCHA service because type of site key is not valid.
     * <p>
     * Please register new site key with the key type set to "reCAPTCHA Android"
     */
    public static final int RECAPTCHA_INVALID_KEYTYPE = 12008;
    /**
     * {@code lookupUri(String, String, int...)} called without first calling {@code initSafeBrowsing()}.
     */
    public static final int SAFE_BROWSING_API_NOT_INITIALIZED = 12009;
    /**
     * Cannot start the reCAPTCHA service because calling package name is not matched with site key.
     * <p>
     * Please add the new package name to your site key via reCAPTCHA Admin Console or choose to disable the package
     * name validation for your key.
     */
    public static final int RECAPTCHA_INVALID_PACKAGE_NAME = 12013;
}
