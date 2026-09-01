/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * Immmutable object to hold the result of a verification operation.
 */
public abstract class VerificationResult {
    /**
     * Returns a status to provide more info on the result of the verification operation.
     *
     * @return {@link CommonStatusCodes#SUCCESS} or {@link RecaptchaStatusCodes#RECAPTCHA_2FA_ABORTED} status if the
     * verification operation succeeded, and non-success status (e.g.
     * {@link RecaptchaStatusCodes#RECAPTCHA_2FA_PIN_MISMATCH}) if the verification failed with more attempts
     * available, i.e. user entered wrong pin.
     */
    public abstract Status getVerificationStatus();

    /**
     * Returns an optional containing the reCAPTCHA token if the verification operation was successful or aborted.
     *
     * @return the reCAPTCHA token if {@code getVerificationStatus().equals(CommonStatusCodes.SUCCESS)} or
     * {@code getVerificationStatus().equals RecaptchaStatusCodes.RECAPTCHA_2FA_ABORTED)}, otherwise an empty
     * {@link RecaptchaOptionalObject}.
     */
    public abstract RecaptchaOptionalObject<String> recaptchaToken();

    /**
     * Returns an optional containing a verification handle if the verification operation failed and the client is allowed to retry.
     *
     * @return a verification handle on a failure status, otherwise an empty {@link RecaptchaOptionalObject}.
     */
    public abstract RecaptchaOptionalObject<VerificationHandle> verificationHandle();
}
