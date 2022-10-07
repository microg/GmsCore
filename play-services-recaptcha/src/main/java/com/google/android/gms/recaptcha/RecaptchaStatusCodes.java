/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Status codes for the reCAPTCHA API.
 */
public final class RecaptchaStatusCodes extends CommonStatusCodes {
    /**
     * reCAPTCHA feature is disabled.
     * <p>
     * Please check if you update Google Play Services on your phone to get our latest reCAPTCHA module code that
     * matches to the API versions you used in the SDK in your app.
     */
    public static final int RECAPTCHA_FEATURE_OFF = 36004;
    /**
     * An internal error occurred during two factor authentication calls.
     * <p>
     * Please try again in a bit.
     */
    public static final int RECAPTCHA_2FA_UNKNOWN = 36005;
    /**
     * The challenge account request token has expired.
     * <p>
     * Please obtain another request token from the reCAPTCHA Enterprise server.
     */
    public static final int RECAPTCHA_2FA_CHALLENGE_EXPIRED = 36006;
    /**
     * The challenge account request token is invalid.
     * <p>
     * Please verify that you are using the correct token obtained from the reCAPTCHA Enterprise server.
     */
    public static final int RECAPTCHA_2FA_INVALID_REQUEST_TOKEN = 36007;
    /**
     * The verification PIN has invalid format.
     * <p>
     * Please verify that the input PIN is of the right length and only contain numerical characters.
     */
    public static final int RECAPTCHA_2FA_INVALID_PIN = 36008;
    /**
     * The verification PIN does not match the PIN sent to the challenged account.
     * <p>
     * Please try again using a new {@link VerificationHandle}.
     */
    public static final int RECAPTCHA_2FA_PIN_MISMATCH = 36009;
    /**
     * All allowed verification attempts are exhausted.
     * <p>
     * Please restart the verification workflow by calling
     * {@link RecaptchaClient#execute(RecaptchaHandle, RecaptchaAction)} again to fetch a new reCAPTCHA token, for
     * retrieving a new challenge token via reCAPTCHA Enterprise API CreateAssessment(), then calling
     * {@link RecaptchaClient#challengeAccount(RecaptchaHandle, String)}.
     */
    public static final int RECAPTCHA_2FA_ATTEMPTS_EXHAUSTED = 36010;
    /**
     * The operation was aborted.
     * <p>
     * Please use the abortion token with the reCAPTCHA Enterprise server to obtain more information.
     */
    public static final int RECAPTCHA_2FA_ABORTED = 36014;
}
